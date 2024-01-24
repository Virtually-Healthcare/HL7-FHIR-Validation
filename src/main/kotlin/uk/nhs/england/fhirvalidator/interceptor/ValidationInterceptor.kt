package uk.nhs.england.fhirvalidator.interceptor

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.i18n.Msg
import ca.uhn.fhir.interceptor.api.Hook
import ca.uhn.fhir.interceptor.api.Interceptor
import ca.uhn.fhir.interceptor.api.Pointcut
import ca.uhn.fhir.rest.api.EncodingEnum
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.server.RestfulServerUtils
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.apache.commons.io.IOUtils
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Resource
import org.slf4j.LoggerFactory
import uk.nhs.england.fhirvalidator.configuration.MessageProperties
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.servlet.http.HttpServletRequest


@Interceptor
class ValidationInterceptor(val ctx : FhirContext, val messageProperties: MessageProperties)  {

    private val log = LoggerFactory.getLogger("FHIRAudit")


    // Custom version of https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-server/src/main/java/ca/uhn/fhir/rest/server/interceptor/RequestValidatingInterceptor.java
    // To support remote service validation

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    fun incomingRequest(request: HttpServletRequest, requestDetails: RequestDetails?, resource: IBaseResource?) :Boolean {

        // Don't validate validate!
        if ((request.method.equals("POST") || request.method.equals("PUT")) && !request.pathInfo.startsWith("/$") ) {
            val encoding = RestfulServerUtils.determineRequestEncodingNoDefault(requestDetails)
            if (encoding == null) {
                log.trace("Incoming request does not appear to be FHIR, not going to validate")
                return true
            }

           // val charset: Charset = ResourceParameter.determineRequestCharset(requestDetails)
            val requestText = requestDetails?.loadRequestContents()
            if (requestText !=null) {
                val methodOutcome = validate(encoding,requestText)

                if (methodOutcome.resource is OperationOutcome) {
                    val validationResult = methodOutcome.resource as OperationOutcome
                    if (validationResult.hasIssue()) {

                        for (issue in validationResult.issue) {
                            if (issue.hasSeverity() && (
                                    issue.severity.equals(OperationOutcome.IssueSeverity.ERROR) ||
                                            issue.severity.equals(OperationOutcome.IssueSeverity.FATAL)
                                        )) {
                                log.debug(issue.diagnostics)
                                fail(validationResult)
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    protected fun fail(theValidationResult: OperationOutcome) {
        throw UnprocessableEntityException(
            Msg.code(330) + "Oh dear",
            theValidationResult
        )
    }
    /*
    protected fun handleFailure(theOutcome: IRepositoryValidatingRule.RuleEvaluation) {
        if (theOutcome.getOperationOutcome() != null) {
            val firstIssue = OperationOutcomeUtil.getFirstIssueDetails(myFhirContext, theOutcome.getOperationOutcome())
            throw PreconditionFailedException(firstIssue, theOutcome.getOperationOutcome())
        }
        throw PreconditionFailedException(theOutcome.getFailureDescription())
    }
*/




    fun validate(encoding : EncodingEnum, input : ByteArray): MethodOutcome {

        val method = MethodOutcome()
        method.created = true
        val opOutcome = OperationOutcome()

        method.operationOutcome = opOutcome

        val url = messageProperties.getValidationFhirServer()
        var myUrl: URL? = null
        val queryParams = ""
        val path = "/FHIR/R4/\$validate"
        myUrl = if (queryParams != null) {
            URL("$url$path?$queryParams")
        } else {
            URL(url + path)
        }
        var retry = 2

        while (retry > 0) {
            val conn = myUrl.openConnection() as HttpURLConnection
            if (encoding.equals(EncodingEnum.XML)) {
                conn.setRequestProperty("Content-Type", "application/fhir+xml")
            } else {
                conn.setRequestProperty("Content-Type", "application/fhir+json")
            }
            conn.setRequestProperty("Accept", "application/fhir+json")
            conn.requestMethod = "POST"
            conn.setDoOutput(true)
           // val jsonInputString = ctx.newJsonParser().encodeResourceToString(request.inputStream)
            try {
                conn.getOutputStream().use { os ->

                    os.write(input, 0, input.size)
                }
                //conn.connect()
                val `is` = InputStreamReader(conn.inputStream)
                try {
                    val rd = BufferedReader(`is`)
                    val postedResource :Resource = ctx.newJsonParser().parseResource(IOUtils.toString(rd)) as Resource
                    if (postedResource != null && postedResource is Resource) {
                        method.resource = postedResource
                    }
                    return method
                } finally {
                    `is`.close()
                }
            }
            catch (ex : IOException) {
                val `is` = InputStreamReader(conn.errorStream)
                try {
                    val rd = BufferedReader(`is`)
                    val postedResource: Resource = ctx.newJsonParser().parseResource(IOUtils.toString(rd)) as Resource
                    if (postedResource != null && postedResource is Resource) {
                        method.resource = postedResource
                    }
                    return method
                }
                catch (exOther: Exception) {
                        throw ex
                    } finally {
                        `is`.close()
                    }
                }
            catch (ex: Exception) {
                retry--
                if (ex.message != null) {
                    if (ex.message!!.contains("401") || ex.message!!.contains("403")) {
                        //this.authenticationResult = null
                        if (retry < 1) throw UnprocessableEntityException(ex.message)
                    }

                } else {
                    throw UnprocessableEntityException(ex.message)
                }
            }
        }
        throw UnprocessableEntityException("Number of retries exhausted")
    }
}


package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.Operation
import ca.uhn.fhir.rest.annotation.OperationParam
import io.swagger.util.Yaml
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import org.apache.commons.io.IOUtils
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CapabilityStatement
import org.hl7.fhir.r4.model.OperationOutcome
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.service.OpenAPIParser
import uk.nhs.england.fhirvalidator.service.VerifyOAS
import uk.nhs.england.fhirvalidator.util.createOperationOutcome
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class OpenAPIProvider(@Qualifier("R4") private val fhirContext: FhirContext,

                      private val verifyOAS: VerifyOAS
) {

    @Operation(name = "convertOAS", idempotent = true,manualResponse=true, manualRequest=true)
    fun convertOAS(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse
    ) {
        var input = IOUtils.toString(servletRequest.getReader());
        var openAPI : OpenAPI? = null
        openAPI = OpenAPIV3Parser().readContents(input).openAPI
        servletResponse.writer.write(Json.pretty(openAPI))
        servletResponse.writer.flush()
        return
    }

    @Operation(name = "verifyOAS", idempotent = true,manualResponse=true, manualRequest=true)
    fun verifyOAS(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse
    ) {
        var openAPI : OpenAPI? = null
        val parseOptions = ParseOptions()
        parseOptions.isResolve = false // implicit
        var input = IOUtils.toString(servletRequest.getReader());
        /*
        if (url != null) {


            //  parseOptions.isResolveFully = true
            openAPI = OpenAPIV3Parser().readLocation(url,null,parseOptions).openAPI
        }
        else {

         */
        servletResponse.setContentType("application/json")
        servletResponse.setCharacterEncoding("UTF-8")
        val oasResult: SwaggerParseResult;
        if (input !== null && !input.isEmpty()) {
            oasResult = io.swagger.parser.OpenAPIParser().readContents(input, null, parseOptions)

            openAPI = oasResult.openAPI
        } else {
            servletResponse.writer.write(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(
                OperationOutcome()
                .addIssue(
                    OperationOutcome.OperationOutcomeIssueComponent()
                    .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                    .setDiagnostics("If url is not provided, the OAS must be present in the payload"))))
            servletResponse.writer.flush()
            return
        }


        if (openAPI !=null) {
            val results = verifyOAS.validate(openAPI)
            var outcome = createOperationOutcome(results)
            if (oasResult !== null && oasResult.getMessages() != null) {
                oasResult.getMessages().forEach({
                   var issue = outcome.addIssue()
                    issue.setSeverity(OperationOutcome.IssueSeverity.WARNING)
                        .setDiagnostics("OAS Issues: "+it)
                    issue.code = OperationOutcome.IssueType.CODEINVALID
                }
                )
            }; // validation errors and warnings


            servletResponse.writer.write(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(outcome))
            servletResponse.writer.flush()
            return
        }

        servletResponse.writer.write(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(
            OperationOutcome().addIssue(
                OperationOutcome.OperationOutcomeIssueComponent()
            .setSeverity(OperationOutcome.IssueSeverity.FATAL).setDiagnostics("Unable to process OAS"))))
        servletResponse.writer.flush()
        return
    }



}

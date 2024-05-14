package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ConceptValidationOptions
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport.CodeValidationResult
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.awsProvider.AWSCodeSystem
import uk.nhs.england.fhirvalidator.interceptor.CognitoAuthInterceptor
import uk.nhs.england.fhirvalidator.service.CodingSupport
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import uk.nhs.england.fhirvalidator.shared.LookupCodeResultUK
import java.nio.charset.StandardCharsets
import jakarta.servlet.http.HttpServletRequest

@Component
class CodeSystemProvider (@Qualifier("R4") private val fhirContext: FhirContext,
                          private val supportChain: ValidationSupportChain,
                          private val codingSupport: CodingSupport,
                          private val validationSupportContext: ValidationSupportContext,
                          private val awsCodeSystem: AWSCodeSystem,
                          private val cognitoAuthInterceptor: CognitoAuthInterceptor
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<CodeSystem> {
        return CodeSystem::class.java
    }

    @Update
    fun update(
        theRequest: HttpServletRequest,
        @ResourceParam codeSystem: CodeSystem,
        @IdParam theId: IdType,
        theRequestDetails: RequestDetails?
    ): MethodOutcome? {
        return awsCodeSystem.update(codeSystem, theId)
    }
    @Create
    fun create(theRequest: HttpServletRequest, @ResourceParam codeSystem: CodeSystem): MethodOutcome? {
        return awsCodeSystem.create(codeSystem)
    }
    @Read
    fun read(httpRequest : HttpServletRequest, @IdParam internalId: IdType): CodeSystem? {
        val resource: Resource? = cognitoAuthInterceptor.readFromUrl(httpRequest.pathInfo, null,"CodeSystem")
        return if (resource is CodeSystem) resource else null
    }
    @Delete
    fun create(theRequest: HttpServletRequest, @IdParam theId: IdType): MethodOutcome? {
        return awsCodeSystem.delete(theId)
    }

    @Search
    fun search(@RequiredParam(name = CodeSystem.SP_URL) url: TokenParam): List<CodeSystem> {
        val list = mutableListOf<CodeSystem>()
        var decodeUri = java.net.URLDecoder.decode(url.value, StandardCharsets.UTF_8.name());
        val resource = supportChain.fetchResource(CodeSystem::class.java,decodeUri)
        if (resource != null) {
            if (resource.id == null) resource.setId(decodeUri)
            list.add(resource)
        } else {
            val resources = awsCodeSystem.search(url)
            if (resources.size>0) list.addAll(resources)
        }

        return list
    }

    @Operation(name = "\$subsumes", idempotent = true)
    fun subsumes (  @OperationParam(name = "codeA") codeA: String?,
                    @OperationParam(name = "codeB") codeB: String?,
                    @OperationParam(name = "system") system: String?) : Parameters? {
        return codingSupport.subsumes(codeA,codeB,java.net.URLDecoder.decode(system, StandardCharsets.UTF_8.name()))
    }

    @Operation(name = "\$validate-code", idempotent = true)
    fun validateCode (
        @OperationParam(name = "url") url: String?,
        @OperationParam(name = "context") context: String?,
        @ResourceParam valueSet: ValueSet?,
        @OperationParam(name = "valueSetVersion") valueSetVersion: String?,
        @OperationParam(name = "code") code: String?,
        @OperationParam(name = "system") system: String?,
        @OperationParam(name = "systemVersion") systemVersion: String?,
        @OperationParam(name = "display") display: String?,
        @OperationParam(name = "coding") coding: TokenParam?,
        @OperationParam(name = "codeableConcept") codeableConcept: CodeableConcept?,
        @OperationParam(name = "date") date: DateParam?,
        @OperationParam(name = "abstract") abstract: BooleanType?,
        @OperationParam(name = "displayLanguage") displayLanguage: CodeType?
    ) : OperationOutcome {
        val input = OperationOutcome()
        input.issueFirstRep.severity = OperationOutcome.IssueSeverity.INFORMATION
        if (code != null) {
            val conceptValidaton = ConceptValidationOptions()
            var validationResult: CodeValidationResult? =
                supportChain.validateCode(this.validationSupportContext, conceptValidaton, java.net.URLDecoder.decode(system, StandardCharsets.UTF_8.name()), code, display, java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name()))

            if (validationResult != null) {
                //logger.info(validationResult?.code)
                if (validationResult.severity != null) {
                    when (validationResult.severity) {
                        IValidationSupport.IssueSeverity.ERROR -> input.issueFirstRep.severity = OperationOutcome.IssueSeverity.ERROR;
                        IValidationSupport.IssueSeverity.WARNING -> input.issueFirstRep.severity = OperationOutcome.IssueSeverity.WARNING;
                        else -> {}
                    }
                }
                input.issueFirstRep.diagnostics = validationResult.message
                //logger.info(validationResult?.message)
            }
        }
        return input;
    }


    @Operation(name = "\$lookup", idempotent = true)
    fun lookup (

        @OperationParam(name = "code") code: String?,
        @OperationParam(name = "system") system: String?,
        @OperationParam(name = "version") version: String?,
        @OperationParam(name = "coding") coding: TokenParam?,
        @OperationParam(name = "date") date: DateParam?,
        @OperationParam(name = "displayLanguage") displayLanguage: CodeType?
    ) : Parameters? {

        return codingSupport.lookupCode(code, system, version, coding)
        /*
        val input = Parameters()

        if (code != null) {

            var lookupCodeResult: IValidationSupport.LookupCodeResult? =
                supportChain.lookupCode(this.validationSupportContext,  system, code)

            if (lookupCodeResult != null) {
                if (lookupCodeResult is uk.nhs.england.fhirvalidator.shared.LookupCodeResultUK) {
                    return (lookupCodeResult).originalParameters
                } else {
                    if (lookupCodeResult.codeDisplay != null) {
                        input.addParameter(
                            Parameters.ParametersParameterComponent().setName("display")
                                .setValue(StringType(lookupCodeResult.codeDisplay))
                        )
                    }
                    if (lookupCodeResult.codeSystemDisplayName != null) {
                        input.addParameter(
                            Parameters.ParametersParameterComponent().setName("name")
                                .setValue(StringType(lookupCodeResult.codeSystemDisplayName))
                        )
                    }
                    if (lookupCodeResult.codeSystemVersion != null) {
                        input.addParameter(
                            Parameters.ParametersParameterComponent().setName("version")
                                .setValue(StringType(java.net.URLDecoder.decode(lookupCodeResult.codeSystemVersion, StandardCharsets.UTF_8.name())))
                        )
                    }
                    if (lookupCodeResult.searchedForCode != null) {
                        input.addParameter(
                            Parameters.ParametersParameterComponent().setName("code")
                                .setValue(StringType(lookupCodeResult.searchedForCode))
                        )
                    }
                    if (lookupCodeResult.searchedForSystem != null) {
                        input.addParameter(
                            Parameters.ParametersParameterComponent().setName("system")
                                .setValue(StringType(java.net.URLDecoder.decode(lookupCodeResult.searchedForSystem, StandardCharsets.UTF_8.name())))
                        )

                    }
                }
            }
        }
        return input;

         */
    }


}

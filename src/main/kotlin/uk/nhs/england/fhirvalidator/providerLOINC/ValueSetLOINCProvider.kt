package uk.nhs.england.fhirvalidator.providerLOINC

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.ConceptValidationOptions
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport.CodeValidationResult
import ca.uhn.fhir.context.support.IValidationSupport.ValueSetExpansionOutcome
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.context.support.ValueSetExpansionOptions
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.awsProvider.AWSValueSet
import uk.nhs.england.fhirvalidator.configuration.TerminologyValidationProperties
import uk.nhs.england.fhirvalidator.interceptor.BasicAuthInterceptor
import uk.nhs.england.fhirvalidator.interceptor.CognitoAuthInterceptor
import uk.nhs.england.fhirvalidator.service.CodingSupport
import uk.nhs.england.fhirvalidator.util.AccessTokenInterceptor
import uk.nhs.england.fhirvalidator.util.FhirSystems
import java.nio.charset.StandardCharsets
import java.util.*
import javax.servlet.http.HttpServletRequest

@Component
class ValueSetLOINCProvider( @Qualifier("R4") private val fhirContext: FhirContext,
private val basicAuthInterceptor: BasicAuthInterceptor
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<ValueSet> {
        return ValueSet::class.java
    }
    companion object : KLogging()



    @Read
    fun read(httpRequest : HttpServletRequest, @IdParam internalId: IdType): ValueSet? {
        val resource: Resource? = basicAuthInterceptor.readFromUrl(httpRequest.pathInfo, null,"ValueSet")
        return if (resource is ValueSet) resource else null
    }
    @Search
    fun search(httpRequest : HttpServletRequest,
               @OptionalParam(name = ValueSet.SP_URL) url: TokenParam?): Bundle? {
        val resource: Resource? = basicAuthInterceptor.readFromUrl(httpRequest.pathInfo, httpRequest.queryString,null)
        return if (resource is Bundle) resource else null
    }



    @Operation(name = "\$expand", idempotent = true)
    fun expand(
        httpRequest : HttpServletRequest,
        @ResourceParam valueSet: ValueSet?,
               @OperationParam(name = ValueSet.SP_URL) url: TokenParam?,
                @OperationParam(name = "filter") filter: StringParam?,
                @OperationParam(name = "includeDesignations") includeDesignations: BooleanType?,
                @OperationParam(name = "elements") elements: StringOrListParam?,
                @OperationParam(name = "property") property: StringOrListParam?): ValueSet? {
        val resource: Resource? = basicAuthInterceptor.readFromUrl(httpRequest.pathInfo, httpRequest.queryString,null)
        return if (resource is ValueSet) resource else null
    }

}

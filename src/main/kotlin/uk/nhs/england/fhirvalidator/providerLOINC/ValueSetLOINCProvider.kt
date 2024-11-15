package uk.nhs.england.fhirvalidator.providerLOINC

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.param.StringOrListParam
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import mu.KLogging
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.interceptor.BasicAuthInterceptor
import jakarta.servlet.http.HttpServletRequest

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

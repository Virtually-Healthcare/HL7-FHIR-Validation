package uk.nhs.england.fhirvalidator.providerLOINC

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import mu.KLogging
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.interceptor.BasicAuthInterceptor
import jakarta.servlet.http.HttpServletRequest

@Component
class ConceptMapLOINCProvider(@Qualifier("R4") private val fhirContext: FhirContext,
                              private val basicAuthInterceptor: BasicAuthInterceptor
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<ConceptMap> {
        return ConceptMap::class.java
    }
    companion object : KLogging()


    @Search
    fun search(httpRequest : HttpServletRequest,
               @OptionalParam(name = ConceptMap.SP_URL) url: TokenParam?): Bundle? {
        val resource: Resource? = basicAuthInterceptor.readFromUrl(httpRequest.pathInfo, httpRequest.queryString,null)
        return if (resource is Bundle) resource else null
    }

}

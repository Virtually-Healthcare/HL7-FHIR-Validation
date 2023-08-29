package uk.nhs.england.fhirvalidator.providerLOINC

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import mu.KLogging
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.interceptor.BasicAuthInterceptor
import javax.servlet.http.HttpServletRequest

@Component
class QuestionnaireProvider (@Qualifier("R4") private val fhirContext: FhirContext,
    private val basicAuthInterceptor: BasicAuthInterceptor
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<Questionnaire> {
        return Questionnaire::class.java
    }


    companion object : KLogging()



    @Search
    fun search(
        httpRequest : HttpServletRequest,
        @OptionalParam(name = Questionnaire.SP_URL) url: TokenParam?,
        @OptionalParam(name = Questionnaire.SP_TITLE) title: StringParam?,
        @OptionalParam(name = "_content") content: StringParam?,
        @OptionalParam(name = "_count") count: StringParam?
               ): Bundle? {
        val resource: Resource? =
            basicAuthInterceptor.readFromUrl(httpRequest.pathInfo, httpRequest.queryString, "Patient")
        if (resource != null && resource is Bundle) {
            return resource
        }
        return null
    }

}

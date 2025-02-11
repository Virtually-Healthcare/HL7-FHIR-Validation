package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.RequiredParam
import ca.uhn.fhir.rest.annotation.Search
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.r4.model.ConceptMap
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import java.nio.charset.StandardCharsets

@Component
class ConceptMapProvider (@Qualifier("R4") private val fhirContext: FhirContext,
                          private val supportChain: ValidationSupportChain
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<ConceptMap> {
        return ConceptMap::class.java
    }

    var implementationGuideParser: ImplementationGuideParser? = ImplementationGuideParser(fhirContext)


    @Search
    fun search(@RequiredParam(name = ConceptMap.SP_URL) url: TokenParam): List<ConceptMap> {
        val list = mutableListOf<ConceptMap>()
        var decodeUri = java.net.URLDecoder.decode(url.value, StandardCharsets.UTF_8.name());
        val resource = supportChain.fetchResource(ConceptMap::class.java,decodeUri)
        if (resource != null) {
            if (resource.id == null) resource.setId(decodeUri)
            list.add(resource)
        } else {
         //   val resources = awsConceptMap.search(url)
         //   if (resources.size>0) list.addAll(resources)
        }
        return list
    }
}

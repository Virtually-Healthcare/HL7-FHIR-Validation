package uk.nhs.england.fhirvalidator.service

import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import uk.nhs.england.fhirvalidator.util.applyProfile
import uk.nhs.england.fhirvalidator.util.getResourcesOfType
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CapabilityStatement
import org.springframework.stereotype.Service

@Service
class CapabilityStatementApplier(
    val supportChain: ValidationSupportChain
) {
    private val restResources = supportChain.fetchAllConformanceResources()?.filterIsInstance(CapabilityStatement::class.java)?.filterNot { it.url == null
            || it.url.contains("sdc")
            || it.url.contains("ips")
            || (!it.url.contains(".uk") && !it.url.contains(".wales") )
            || it.url.contains("us.core")}
        ?.flatMap { it.rest }
        ?.flatMap { it.resource }

    fun applyCapabilityStatementProfiles(resource: IBaseResource) {
        restResources?.forEach { applyRestResource(resource, it) }
    }

    private fun applyRestResource(
        resource: IBaseResource,
        restResource: CapabilityStatement.CapabilityStatementRestResourceComponent
    ) {
        val matchingResources = getResourcesOfType(resource, restResource.type)
        if (restResource.hasProfile()) {
            applyProfile(matchingResources, restResource.profile)
        }
        if (restResource.hasSupportedProfile()) {
            restResource.supportedProfile.forEach{
                applyProfile(matchingResources, it.value)
            }
        }
    }

}

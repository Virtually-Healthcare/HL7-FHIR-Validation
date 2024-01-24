package uk.nhs.england.fhirvalidator.interceptor

import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import uk.nhs.england.fhirvalidator.util.applyProfile
import uk.nhs.england.fhirvalidator.util.getResourcesOfType
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CapabilityStatement
import org.springframework.stereotype.Service

@Service
class CapabilityStatementApplier(
    val supportChain: ValidationSupportChain
) {
    private val restResources = supportChain.fetchAllConformanceResources()?.filterIsInstance(CapabilityStatement::class.java)?.filterNot { it.url == null
            || it.url.contains("sdc")
            || it.url.contains("ips")
            || it.url.contains("ipa")
            || (!it.url.contains(".uk") && !it.url.contains(".wales") )
            || it.url.contains("us.core")}
        ?.flatMap { it.rest }
        ?.flatMap { it.resource }

    fun applyCapabilityStatementProfiles(resource: IBaseResource, importProfile: Boolean?) {
        restResources?.forEach { applyRestResource(resource, it, importProfile) }
    }

    private fun applyRestResource(
        resource: IBaseResource,
        restResource: CapabilityStatement.CapabilityStatementRestResourceComponent,
        importProfile: Boolean?
    ) {
        val matchingResources = getResourcesOfType(resource, restResource.type)
        if (restResource.hasProfile()) {
            applyProfile(matchingResources, restResource.profile)
        }
        if (importProfile !== null && importProfile && restResource.hasExtension()) {
            restResource.extension.forEach{
                if (it.hasUrl()
                    && it.url.equals("http://hl7.org/fhir/StructureDefinition/structuredefinition-imposeProfile")
                    && it.hasValue() && it.value is CanonicalType) {
                        val canonicalType =  it.value as CanonicalType
                        applyProfile(matchingResources, canonicalType.value)
                }
            }

        }
    }

}

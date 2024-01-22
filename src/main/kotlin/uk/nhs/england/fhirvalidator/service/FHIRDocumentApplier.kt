package uk.nhs.england.fhirvalidator.service

import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.*
import org.springframework.stereotype.Service
import uk.nhs.england.fhirvalidator.util.FhirSystems
import uk.nhs.england.fhirvalidator.util.createOperationOutcome


@Service
class FHIRDocumentApplier {

    fun applyDocumentDefinition(resource: IBaseResource): OperationOutcome? {
        if (resource !is Bundle || resource.type != Bundle.BundleType.DOCUMENT) {
            return null
        }

        val composition = findComposition(resource)
            ?: return createOperationOutcome(
                "No Composition found.",
                "Bundle.entry"
            )

        if (composition.hasType()) {
            if (composition.type.hasCoding(FhirSystems.SNOMED_CT,"4241000179101")
                || composition.type.hasCoding(FhirSystems.LOINC,"11502-2")
                ) composition.meta.addProfile("http://hl7.eu/fhir/laboratory/StructureDefinition/Composition-eu-lab")
            if (composition.type.hasCoding(FhirSystems.LOINC,"60591-5")
            ) composition.meta.addProfile("http://hl7.org/fhir/uv/ips/StructureDefinition/Composition-uv-ips")
        }
        return null
    }

    private fun findComposition(bundle: Bundle): Composition? {
        return bundle.entry
            ?.map { it.resource }
            ?.filterIsInstance(Composition::class.java)
            ?.singleOrNull()
    }


}

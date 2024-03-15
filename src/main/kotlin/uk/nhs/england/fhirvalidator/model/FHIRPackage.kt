package uk.nhs.england.fhirvalidator.model

data class DependsOn(
    val packageName: String,
    val version: String,
    var canonicalUri: String?
)
data class FHIRPackage(
    val packageName: String,
    val version: String,
    val description: String,
    var canonicalUri: String?,
    val derived: Boolean,
    val dependencies: List<DependsOn>?
)

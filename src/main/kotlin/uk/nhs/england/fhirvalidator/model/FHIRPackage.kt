package uk.nhs.england.fhirvalidator.model

data class FHIRPackage(val name: String, val version: String, val description: String, val url: String?, val derived: Boolean)

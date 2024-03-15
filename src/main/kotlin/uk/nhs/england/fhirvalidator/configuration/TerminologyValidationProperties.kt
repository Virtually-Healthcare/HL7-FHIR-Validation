package uk.nhs.england.fhirvalidator.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "terminology")
data class TerminologyValidationProperties(
    var url: String?,
    var authorization: Authorization?
) {
    data class Authorization(
        var tokenUrl: String,
        var clientId: String,
        var clientSecret: String
    )
}

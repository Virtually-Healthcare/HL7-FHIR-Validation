package uk.nhs.england.fhirvalidator.configuration

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "fhir")
data class FHIRServerProperties(
    var server: Server,
    var isNHSEngland: Boolean?,
    //var ig: Package?,
    var igs: String?
) {
    data class Server(
        var baseUrl: String,
        var name: String,
        var version: String
    )
    /*
    data class Package(
        var name: String,
        var version: String
    )

     */
}

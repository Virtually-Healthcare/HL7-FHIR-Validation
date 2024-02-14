package uk.nhs.england.fhirvalidator.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "fhir")
data class FHIRServerProperties(
    var server: Server,
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

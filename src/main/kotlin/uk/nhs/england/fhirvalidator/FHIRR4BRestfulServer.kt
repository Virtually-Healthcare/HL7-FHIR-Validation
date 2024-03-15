package uk.nhs.england.fhirvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.EncodingEnum
import ca.uhn.fhir.rest.server.RestfulServer
import jakarta.servlet.annotation.WebServlet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import uk.nhs.england.fhirvalidator.providerR4B.CapabilityStatementInterceptorR4B
import uk.nhs.england.fhirvalidator.providerR4B.MedicinalProductDefinitionProviderR4B
import uk.nhs.england.fhirvalidator.providerR4B.PackagedProductDefinitionProviderR4B
import java.util.*


@ConditionalOnProperty(prefix = "services", name = ["R4B"])
@WebServlet("/FHIR/R4B/*", loadOnStartup = 1)
class FHIRR4BRestfulServer(
    @Qualifier("R4B") fhirContext: FhirContext,
    private val medicinalProductDefinitionProviderR4B: MedicinalProductDefinitionProviderR4B,
    private val packagedProductDefinitionProviderR4B: PackagedProductDefinitionProviderR4B
    ) : RestfulServer(fhirContext) {

    override fun initialize() {
        super.initialize()

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        registerProvider(medicinalProductDefinitionProviderR4B)
        registerProvider(packagedProductDefinitionProviderR4B)

        registerInterceptor(CapabilityStatementInterceptorR4B())

        isDefaultPrettyPrint = true
        defaultResponseEncoding = EncodingEnum.JSON
    }
}

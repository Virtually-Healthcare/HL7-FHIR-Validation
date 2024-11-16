package uk.nhs.england.fhirvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.rest.api.EncodingEnum
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy
import ca.uhn.fhir.rest.server.RestfulServer
import com.amazonaws.services.sqs.AmazonSQS
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.annotation.WebServlet
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import uk.nhs.england.fhirvalidator.configuration.FHIRServerProperties
import uk.nhs.england.fhirvalidator.configuration.MessageProperties
import uk.nhs.england.fhirvalidator.configuration.ServicesProperties
import uk.nhs.england.fhirvalidator.interceptor.AWSAuditEventLoggingInterceptor
import uk.nhs.england.fhirvalidator.interceptor.CapabilityStatementInterceptor
import uk.nhs.england.fhirvalidator.interceptor.ValidationInterceptor
import uk.nhs.england.fhirvalidator.model.FHIRPackage
import uk.nhs.england.fhirvalidator.provider.*
import java.util.*



@ConditionalOnProperty(prefix = "services", name = ["R4"])
@WebServlet
    ("/FHIR/R4/*", loadOnStartup = 1)
class FHIRR4RestfulServer(
    @Qualifier("R4") fhirContext: FhirContext,
    @Autowired(required = false) val sqs : AmazonSQS?,
    val fhirPackage: List<FHIRPackage>,
    private val validateR4Provider: ValidateR4Provider,
    private val capabilityStatementProvider: CapabilityStatementProvider,
    private val messageDefinitionProvider: MessageDefinitionProvider,
    private val structureDefinitionProvider: StructureDefinitionProvider,
    private val operationDefinitionProvider: OperationDefinitionProvider,
    private val searchParameterProvider: SearchParameterProvider,
    private val structureMapProvider: StructureMapProvider,
    private val conceptMapProvider: ConceptMapProvider,
    private val namingSystemProvider: NamingSystemProvider,
    private val valueSetProvider: ValueSetProvider,
    private val codeSystemProvider: CodeSystemProvider,
    private val compostionProvider: CompostionProvider,
    @Qualifier("SupportChain") private val supportChain: IValidationSupport,
    val fhirServerProperties: FHIRServerProperties,
    private val messageProperties: MessageProperties,
    private val servicesProperties: ServicesProperties
) : RestfulServer(fhirContext) {

    override fun initialize() {
        super.initialize()

        val serverBaseUrl = fhirServerProperties.server.baseUrl + "/FHIR/R4"
        serverAddressStrategy = HardcodedServerAddressStrategy(serverBaseUrl)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        registerProvider(validateR4Provider)
        registerProvider(capabilityStatementProvider)
        registerProvider(messageDefinitionProvider)
        registerProvider(structureDefinitionProvider)
        registerProvider(operationDefinitionProvider)
        registerProvider(searchParameterProvider)
        registerProvider(structureMapProvider)
        registerProvider(conceptMapProvider)
        registerProvider(namingSystemProvider)
        registerProvider(valueSetProvider)
        registerProvider(codeSystemProvider)
        registerProvider(compostionProvider)



        registerInterceptor(CapabilityStatementInterceptor(this.fhirContext, fhirPackage, supportChain, fhirServerProperties))


        val awsAuditEventLoggingInterceptor =
            AWSAuditEventLoggingInterceptor(
                this.fhirContext,
                fhirServerProperties,
                messageProperties,
                sqs
            )
        interceptorService.registerInterceptor(awsAuditEventLoggingInterceptor)

        val validationInterceptor = ValidationInterceptor(fhirContext,messageProperties)
        interceptorService.registerInterceptor(validationInterceptor)

        isDefaultPrettyPrint = true
        defaultResponseEncoding = EncodingEnum.JSON
    }
}

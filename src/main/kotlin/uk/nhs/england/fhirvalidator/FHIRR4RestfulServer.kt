package uk.nhs.england.fhirvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.rest.api.EncodingEnum
import ca.uhn.fhir.rest.server.RestfulServer
import com.amazonaws.services.sqs.AmazonSQS
import com.fasterxml.jackson.databind.ObjectMapper
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import uk.nhs.england.fhirvalidator.configuration.FHIRServerProperties
import uk.nhs.england.fhirvalidator.configuration.MessageProperties
import uk.nhs.england.fhirvalidator.interceptor.AWSAuditEventLoggingInterceptor
import uk.nhs.england.fhirvalidator.interceptor.CapabilityStatementInterceptor
import uk.nhs.england.fhirvalidator.interceptor.ValidationInterceptor
import uk.nhs.england.fhirvalidator.provider.*
import java.util.*
import javax.servlet.annotation.WebServlet

@ConditionalOnProperty(prefix = "services", name = ["R4"])
@WebServlet("/FHIR/R4/*", loadOnStartup = 1)
class FHIRR4RestfulServer(
    @Qualifier("R4") fhirContext: FhirContext,
    @Autowired(required = false) val sqs : AmazonSQS?,
    val objectMapper: ObjectMapper,
    private val validateR4Provider: ValidateR4Provider,
    private val openAPIProvider: OpenAPIProvider,
    private val markdownProvider: MarkdownProvider,
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

    private val npmPackages: List<NpmPackage>,
    @Qualifier("SupportChain") private val supportChain: IValidationSupport,
    val fhirServerProperties: FHIRServerProperties,
    private val messageProperties: MessageProperties
) : RestfulServer(fhirContext) {

    override fun initialize() {
        super.initialize()

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        registerProvider(validateR4Provider)
        registerProvider(openAPIProvider)
        registerProvider(markdownProvider)
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



        registerInterceptor(CapabilityStatementInterceptor(this.fhirContext, objectMapper, supportChain, fhirServerProperties))


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

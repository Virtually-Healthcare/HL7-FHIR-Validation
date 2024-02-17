package uk.nhs.england.fhirvalidator.interceptor

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.interceptor.api.Hook
import ca.uhn.fhir.interceptor.api.Interceptor
import ca.uhn.fhir.interceptor.api.Pointcut
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import com.fasterxml.jackson.databind.ObjectMapper
import org.hl7.fhir.instance.model.api.IBaseConformance
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.core.io.ClassPathResource
import uk.nhs.england.fhirvalidator.configuration.FHIRServerProperties
import uk.nhs.england.fhirvalidator.model.FHIRPackage
import uk.nhs.england.fhirvalidator.model.SimplifierPackage
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser

@Interceptor
class CapabilityStatementInterceptor(
    fhirContext: FhirContext,
    private val fhirPackage: List<FHIRPackage>,
    private val supportChain: IValidationSupport,
    private val fhirServerProperties: FHIRServerProperties
) {

    var implementationGuideParser: ImplementationGuideParser? = ImplementationGuideParser(fhirContext)

    @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
    fun customize(theCapabilityStatement: IBaseConformance) {

        // Cast to the appropriate version
        val cs: CapabilityStatement = theCapabilityStatement as CapabilityStatement

        // Customize the CapabilityStatement as desired
       // val apiextension = Extension();
      //  apiextension.url = "https://fhir.nhs.uk/StructureDefinition/Extension-NHSDigital-CapabilityStatement-Package"
        /*
         if (enhance && fhirPackage !== null && fhirPackage.size > 0) {
             var igDescription = "\n\n | FHIR Implementation Guide | Version |\n |-----|-----|\n"

             fhirPackage.forEach {
                 if (!it.derived) {
                     val name = it.url
                     val version = it.version
                     val pckg = it.name
                     val url = getDocumentationPath(it.url)
                     if (name == null) igDescription += " ||$pckg#$version|\n"
                     else igDescription += " |[$name]($url)|$pckg#$version|\n"
                 }
             }
             openApi.info.description += "\n\n" + igDescription
         }

          */


            fhirPackage.forEach {
                if (!it.derived) {
                    var implementationGuide = ImplementationGuide()
                    implementationGuide.packageId = it.packageName
                    implementationGuide.version = it.version
                    implementationGuide.status = Enumerations.PublicationStatus.UNKNOWN
                    implementationGuide.name = it.packageName
                    implementationGuide.url = it.canonicalUri
                    implementationGuide.id = it.packageName
                    if (it.dependencies !== null) {
                        for (dependency in it.dependencies) {
                            implementationGuide.dependsOn.add(
                                ImplementationGuide.ImplementationGuideDependsOnComponent()
                                    .setPackageId(dependency.packageName)
                                    .setVersion(dependency.version)
                                    .setUri(dependency.canonicalUri)
                            )
                        }
                    }
                    cs.implementationGuide.add(CanonicalType("#" + it.packageName))
                    cs.contained.add(implementationGuide)
                }
            }

        /*
        val packageExtension = Extension();
        packageExtension.url="openApi"
        packageExtension.extension.add(Extension().setUrl("documentation").setValue(UriType("https://simplifier.net/guide/NHSDigital/Home")))
        packageExtension.extension.add(Extension().setUrl("description").setValue(StringType("NHS England FHIR Implementation Guide")))
        apiextension.extension.add(packageExtension)
        cs.extension.add(apiextension)
*/

        for (resourceIG in supportChain.fetchAllConformanceResources()?.filterIsInstance<CapabilityStatement>()!!) {
            if (resourceIG.url.contains(".uk")) {
                for (restComponent in resourceIG.rest) {
                    for (component in restComponent.resource) {

                        if (component.hasProfile()) {
                            var resourceComponent = getResourceComponent(component.type, cs)
                            if (resourceComponent != null) {
                                resourceComponent.type = component.type
                                resourceComponent.profile = component.profile
                            } else {
                                // add this to CapabilityStatement to indicate profile being valiated against
                                resourceComponent = CapabilityStatement.CapabilityStatementRestResourceComponent().setType(component.type)
                                    .setProfile(component.profile)
                                cs.restFirstRep.resource.add(resourceComponent)
                            }
                            if (component.hasExtension()) {
                                component.extension.forEach{
                                    if (it.url.equals("http://hl7.org/fhir/StructureDefinition/structuredefinition-imposeProfile")) {
                                        var found = false
                                        if (resourceComponent !== null) {
                                            if (resourceComponent.hasExtension()) {
                                                for (extension in resourceComponent.extension) {
                                                    if (extension.url.equals("http://hl7.org/fhir/StructureDefinition/structuredefinition-imposeProfile")) {
                                                        if (extension.hasValue() && it.hasValue() && it.value is CanonicalType && extension.value is CanonicalType) {
                                                            if ((extension.value as CanonicalType).value.equals((it.value as CanonicalType).value)) {
                                                                found = true
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (!found) {
                                                resourceComponent.extension.add(it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val message = CapabilityStatement.CapabilityStatementMessagingComponent()

        for (resourceIG in supportChain.fetchAllConformanceResources()?.filterIsInstance<MessageDefinition>()!!) {
            if (resourceIG.hasUrl()) {
                val messageDefinition = CapabilityStatement.CapabilityStatementMessagingSupportedMessageComponent()
                    .setDefinition(resourceIG.url)

                var found = false;
                for (mes in message.supportedMessage) {
                    if (mes.definition.equals(messageDefinition.definition)) found = true;
                }
                if (!found) message.supportedMessage.add(messageDefinition)
            }
        }
        if (message.supportedMessage.size>0)  cs.messaging.add(message)

        for (ops in cs.restFirstRep.operation) {
            val operation = getOperationDefinition(ops.name)
            if (operation !=null) {
                ops.definition = operation.url
            }
        }
        for (resource in cs.restFirstRep.resource) {
            for (ops in resource.operation) {
                val operation = getOperationDefinition(ops.name)
                if (operation != null) {
                    ops.definition = operation.url
                }
            }
        }
        cs.name = fhirServerProperties.server.name
        cs.software.name = fhirServerProperties.server.name
        cs.software.version = fhirServerProperties.server.version
        cs.publisher = "NHS England"
        cs.implementation.url = fhirServerProperties.server.baseUrl + "/FHIR/R4"
        cs.implementation.description = "NHS England FHIR Conformance"
    }

    fun getResourceComponent(type : String, cs : CapabilityStatement ) : CapabilityStatement.CapabilityStatementRestResourceComponent? {
        for (rest in cs.rest) {
            for (resource in rest.resource) {
                // println(type + " - " +resource.type)
                if (resource.type.equals(type))
                    return resource
            }
        }
        return null
    }

    fun getOperationDefinition(operationCode : String) : OperationDefinition? {
        val operation= operationCode.removePrefix("$")
        for (resource in supportChain.fetchAllConformanceResources()!!) {
            if (resource is OperationDefinition) {
                if (resource.code.equals(operation)) {
                    return resource
                }
            }
        }
        return null
    }
}

package uk.nhs.england.fhirvalidator.configuration

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.context.support.ValidationSupportContext
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import ca.uhn.fhir.validation.FhirValidator
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.transform.util.APIFileDownload
import io.swagger.v3.oas.models.examples.Example
import mu.KLogging
import org.hl7.fhir.common.hapi.validation.support.*
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.r4.model.CapabilityStatement
import org.hl7.fhir.r4.model.ImplementationGuide
import org.hl7.fhir.r4.model.StructureDefinition
import org.hl7.fhir.utilities.json.model.JsonProperty
import org.hl7.fhir.utilities.npm.NpmPackage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import uk.nhs.england.fhirvalidator.awsProvider.*
import uk.nhs.england.fhirvalidator.model.DependsOn
import uk.nhs.england.fhirvalidator.model.FHIRPackage
import uk.nhs.england.fhirvalidator.model.SimplifierPackage
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import uk.nhs.england.fhirvalidator.util.AccessTokenInterceptor
import uk.nhs.england.fhirvalidator.validationSupport.SwitchedTerminologyServiceValidationSupport
import uk.nhs.england.fhirvalidator.validationSupport.UnsupportedCodeSystemWarningValidationSupport
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.Predicate
import kotlin.collections.ArrayList


@Configuration
open class ValidationConfiguration(
    private val implementationGuideParser: ImplementationGuideParser,
    private val terminologyValidationProperties: TerminologyValidationProperties,
    val messageProperties: MessageProperties,
    val fhirServerProperties: FHIRServerProperties,
    val objectMapper: ObjectMapper
) {
    companion object : KLogging()

    var npmPackages: List<NpmPackage> = emptyList()

    var fhirPackage = mutableListOf<FHIRPackage>()
    @Bean
    open fun fhirPackages() : List<FHIRPackage> {
        return this.fhirPackage
    }
    @Bean
    open fun validator(@Qualifier("R4") fhirContext: FhirContext, instanceValidator: FhirInstanceValidator): FhirValidator {
        return fhirContext.newValidator().registerValidatorModule(instanceValidator)
    }

    @Bean
    open fun instanceValidator(supportChain: ValidationSupportChain): FhirInstanceValidator {
       return FhirInstanceValidator(uk.nhs.england.fhirvalidator.shared.NHSDCachingValidationSupport(supportChain))
       // return FhirInstanceValidator(supportChain)
    }

    @Bean open fun validationSupportContext(supportChain: ValidationSupportChain): ValidationSupportContext {
        return ValidationSupportContext(supportChain)
    }


    @Bean("SupportChain")
    open fun validationSupportChain(
        @Qualifier("R4") fhirContext: FhirContext,
        switchedTerminologyServiceValidationSupport: SwitchedTerminologyServiceValidationSupport,
        awsQuestionnaire: AWSQuestionnaire,
        awsCodeSystem: AWSCodeSystem,
        awsValueSet: AWSValueSet,
        awsAuditEvent: AWSAuditEvent,
        awsConceptMap: AWSConceptMap
    ): ValidationSupportChain {
        val supportChain = ValidationSupportChain(
            DefaultProfileValidationSupport(fhirContext),
            SnapshotGeneratingValidationSupport(fhirContext),
            CommonCodeSystemsTerminologyService(fhirContext),
            switchedTerminologyServiceValidationSupport
        )
        if (messageProperties.getAWSValidationSupport()) supportChain.addValidationSupport( AWSValidationSupport(fhirContext, awsQuestionnaire,awsCodeSystem,awsValueSet, awsConceptMap))
        val manifest = getPackages()
        if (npmPackages != null) {
            /*
            npmPackages!!
                .filter { !it.name().equals("hl7.fhir.r4.examples") }
                .map(implementationGuideParser::createPrePopulatedValidationSupport)

                .forEach(supportChain::addValidationSupport)

             */
            val npms = npmPackages!!.filter { !it.name().equals("hl7.fhir.r4.examples") }
            for (pckg in npms) {
                val support = implementationGuideParser.createPrePopulatedValidationSupport(pckg)
                supportChain.addValidationSupport(support)
                var description = pckg.description()
                if (description == null) description = ""
                var derived = true
                if (manifest !==null) {
                    manifest.forEach {
                        if (it.packageName.equals(pckg.name())) derived = false
                    }
                }
                val dependsOn = ArrayList<DependsOn>()
                for (dependency in pckg.dependencies()) {
                    val pckgStrs = dependency.split("#")
                    if (pckgStrs.size>1) {
                        // dummy value for now
                        var uri = "https://example.fhir.org/ImplementationGuide/" + pckgStrs[0] + "|" + pckgStrs[1]
                        if (pckgStrs[0].contains("hl7.fhir.r4.core")) uri = "https://hl7.org/fhir/R4/"
                        if (pckgStrs[0].contains("ukcore")) uri = "https://simplifier.net/guide/ukcoreversionhistory"
                        if (pckgStrs[0].contains("nhsengland")) uri = "https://simplifier.net/guide/nhs-england-implementation-guide-version-history"
                        val depends = DependsOn(
                            pckgStrs[0],
                            pckgStrs[1],
                            uri
                        )
                        dependsOn.add(depends)
                    }
                }
                var packUrl = pckg.url()
                if (pckg.name().contains("hl7.fhir.r4.core")) packUrl = "https://hl7.org/fhir/R4/"
                if (pckg.name().contains("ukcore")) packUrl = "https://simplifier.net/guide/ukcoreversionhistory"
                if (pckg.name().contains("nhsengland")) packUrl = "https://simplifier.net/guide/nhs-england-implementation-guide-version-history"
                var newPckg = FHIRPackage(pckg.name(),pckg.version(),description,packUrl,derived,dependsOn)
                this.fhirPackage.add(newPckg)
            }
            //Initialise now instead of when the first message arrives
            generateSnapshots(supportChain)
            supportChain.fetchCodeSystem("http://snomed.info/sct")
            // Correct dependencies canonical urls
            for (pkg in this.fhirPackage) {
                if (pkg.canonicalUri !== null && pkg.derived) {
                    for (otherPkg in this.fhirPackage) {
                        if (!otherPkg.packageName.equals(pkg.packageName) && !otherPkg.version.equals(pkg.version) && otherPkg.dependencies !== null) {
                            for (depencyPkg in otherPkg.dependencies) {
                                if (depencyPkg.packageName.equals(pkg.packageName) && depencyPkg.version.equals(pkg.version)) {
                                    depencyPkg.canonicalUri = pkg.canonicalUri
                                }
                            }
                        }
                    }
                }
            }
            // packages have been processed so remove them
            npmPackages = emptyList()
            return supportChain
        } else {
            throw UnprocessableEntityException("Unable to process npm package configuration")
        }
    }

    @Bean
    open fun switchedTerminologyServiceValidationSupport(
        @Qualifier("R4") fhirContext: FhirContext,
        optionalRemoteTerminologySupport: Optional<uk.nhs.england.fhirvalidator.shared.RemoteTerminologyServiceValidationSupport>
    ): SwitchedTerminologyServiceValidationSupport {
        val snomedValidationSupport = if (optionalRemoteTerminologySupport.isPresent) {
            uk.nhs.england.fhirvalidator.shared.NHSDCachingValidationSupport(optionalRemoteTerminologySupport.get())
            // Disabled default caching as it was causing invalid results (on snomed display terms)
        } else {
            UnsupportedCodeSystemWarningValidationSupport(fhirContext)
        }

        return SwitchedTerminologyServiceValidationSupport(
            fhirContext,
            InMemoryTerminologyServerValidationSupport(fhirContext),
            snomedValidationSupport,
            Predicate { it.startsWith("http://snomed.info/sct")
                    || it.startsWith("https://dmd.nhs.uk")
                    || it.startsWith("http://read.info")
                    || it.startsWith("http://hl7.org/fhir/sid/icd")
            }
        )
    }

    @Bean
    @ConditionalOnProperty("terminology.url")
    open fun remoteTerminologyServiceValidationSupport(
        @Qualifier("R4") fhirContext: FhirContext,
        optionalAuthorizedClientManager: Optional<OAuth2AuthorizedClientManager>
    ): uk.nhs.england.fhirvalidator.shared.RemoteTerminologyServiceValidationSupport {
        logger.info("Using remote terminology server at ${terminologyValidationProperties.url}")
        val validationSupport =
            uk.nhs.england.fhirvalidator.shared.RemoteTerminologyServiceValidationSupport(
                fhirContext
            )
        validationSupport.setBaseUrl(terminologyValidationProperties.url)

        if (optionalAuthorizedClientManager.isPresent) {
            val authorizedClientManager = optionalAuthorizedClientManager.get()
            val accessTokenInterceptor = AccessTokenInterceptor(authorizedClientManager)
            validationSupport.addClientInterceptor(accessTokenInterceptor)
        }

        return validationSupport
    }

        fun generateSnapshots(supportChain: IValidationSupport) {
            val structureDefinitions = supportChain.fetchAllStructureDefinitions<StructureDefinition>() ?: return
            val context = ValidationSupportContext(supportChain)
            structureDefinitions
                .filter { shouldGenerateSnapshot(it) }
                .forEach {
                    try {
                        circularReferenceCheck(it,supportChain)
                    } catch (e: Exception) {
                        logger.error("Failed to generate snapshot for $it", e)
                    }
                }

            structureDefinitions
                .filter { shouldGenerateSnapshot(it) }
                .forEach {
                    try {
                        val start: Instant = Instant.now()
                        supportChain.generateSnapshot(context, it, it.url, "https://fhir.nhs.uk/R4", it.name)
                        val end: Instant = Instant.now()
                        val duration: Duration = Duration.between(start, end)
                        logger.info(duration.toMillis().toString() + " ms $it")
                    } catch (e: Exception) {
                        logger.error("Failed to generate snapshot for $it", e)
                    }
                }
        }

    private fun circularReferenceCheck(structureDefinition: StructureDefinition, supportChain: IValidationSupport): StructureDefinition {
        if (structureDefinition.hasSnapshot()) logger.error(structureDefinition.url + " has snapshot!!")
        structureDefinition.differential.element.forEach{
            //   ||
            if ((
                        it.id.endsWith(".partOf") ||
                        it.id.endsWith(".basedOn") ||
                        it.id.endsWith(".replaces") ||
                        it.id.contains("Condition.stage.assessment") ||
                        it.id.contains("Observation.derivedFrom") ||
                                it.id.contains("Observation.hasMember") ||
                                it.id.contains("CareTeam.encounter") ||
                        it.id.contains("CareTeam.reasonReference") ||
                        it.id.contains("ServiceRequest.encounter") ||
                        it.id.contains("ServiceRequest.reasonReference") ||
                        it.id.contains("EpisodeOfCare.diagnosis.condition") ||
                        it.id.contains("Encounter.diagnosis.condition") ||
                        it.id.contains("Encounter.reasonReference") ||
                                it.id.contains("Encounter.appointment")
                                )
                && it.hasType()) {
                logger.warn(structureDefinition.url + " has circular references ("+ it.id + ")")
                it.type.forEach{
                    if (it.hasTargetProfile())
                        it.targetProfile.forEach {
                            it.value = getBase(it.value, supportChain);
                        }
                }
            }
        }
        return structureDefinition
    }


    open fun getPackages() :Array<SimplifierPackage>? {
        var manifest : Array<SimplifierPackage>? = null
        if (fhirServerProperties.igs != null && !fhirServerProperties.igs!!.isEmpty()   ) {
            val packages= fhirServerProperties.igs!!.split(",")
            val manifest2 = arrayListOf<SimplifierPackage>()
            packages.forEachIndexed{ index, pkg  ->
                manifest2.add(SimplifierPackage(pkg.substringBefore("#"),pkg.substringAfter("#"),null))
            }
            manifest = manifest2.toTypedArray()
        } else {
            val configurationInputStream = ClassPathResource("manifest.json").inputStream
            manifest = objectMapper.readValue(configurationInputStream, Array<SimplifierPackage>::class.java)
        }

        val packages = HashMap<String, NpmPackage>()
        if (manifest == null) throw UnprocessableEntityException("Error processing IG manifest")
        for (packageNpm in manifest ) {
            val packageName = packageNpm.packageName + "-" + packageNpm.version+ ".tgz"

            var inputStream: InputStream? = null
            try {
                inputStream = ClassPathResource(packageName).inputStream
            } catch (ex : Exception) {
                if (ex.message != null) logger.info(ex.message)
            }
            if (inputStream == null) {
                val downloadedPackages = downloadPackage(packageNpm.packageName,packageNpm.version, packageNpm.downloadUrl)
                for (downloadpackage in downloadedPackages) {
                    val name = downloadpackage.name()+'#'+downloadpackage.version()
                    if (packages.get(name) == null) {
                        packages.put(name,downloadpackage)
                    } else {
                        logger.info("package "+name + " already present")
                    }
                }
            } else {
                logger.info("Using local cache for {} - {}",packageNpm.packageName, packageNpm.version)
                val downloadpackage = NpmPackage.fromPackage(inputStream)
                val name = downloadpackage.name()+'#'+downloadpackage.version()
                if (packages.get(name) == null) {
                    packages.put(name,downloadpackage)
                } else {
                    logger.info("package "+name + " already present")
                }
            }
        }
        this.npmPackages = packages.values.toList()
        /*
        if (fhirServerProperties.ig != null && !fhirServerProperties.ig!!.isEmpty()) {
            return downloadPackage(fhirServerProperties.ig!!)
        }
        return Arrays.stream(packages)
            .map { "${it.packageName}-${it.version}.tgz" }
            .map { ClassPathResource(it).inputStream }
            .map { NpmPackage.fromPackage(it) }
            .toList()

         */
        return manifest
    }

    open fun downloadPackage(name : String, version : String, downloadPath: String?) : List<NpmPackage> {
        logger.info("Downloading from AWS Cache {} - {}",name, version)
        // Try self first
        var inputStream : InputStream? = null;
        try {
            if (downloadPath == null ) {
                val packUrl = "https://fhir.nhs.uk/ImplementationGuide/" + name + "-" + version
                inputStream =
                    readFromUrl(messageProperties.getNPMFhirServer() + "/FHIR/R4/ImplementationGuide/\$package?url=" + packUrl)
                logger.info("Found Package on AWS Cache {} - {}", name, version)
            } else {
                val packUrl = "https://fhir.nhs.uk/ImplementationGuide/" + name + "-" + version
                inputStream =
                    readFromUrl(downloadPath)
                logger.info("Found Package at specified download path {} - {}", name, version)
            }
        } catch (ex : Exception) {
            logger.warn("Package not found in IG Publisher or AWS Cache trying simplifier {} - {}",name,version)
            if (ex.message!=null) logger.info(ex.message)
            try {
                inputStream = readFromUrl("https://packages.simplifier.net/" + name + "/" + version)
                logger.info("Found Package on Simplifier {} - {}",name,version)
            } catch (exSimplifier: Exception) {
                logger.error("Package not found on simplifier {} - {}",name, version)
            }
        }
        if (inputStream == null) logger.error("Failed to download  {} - {}",name, version)
        val packages = arrayListOf<NpmPackage>()
        val npmPackage = NpmPackage.fromPackage(inputStream)

        val dependency= npmPackage.npm.get("dependencies")

        if (dependency !== null) {
            if (dependency.isJsonArray) logger.info("isJsonArray")
            if (dependency.isJsonObject) {
                val obj = dependency.asJsonObject()
                obj.properties
                val entrySet: MutableList<JsonProperty>? = obj.properties
                entrySet?.forEach()
                {
                    logger.info(it.name + " version =  " + it.value)
                    if (it.name != "hl7.fhir.r4.core") {
                        val entryVersion = it.value?.asString()?.replace("\"","")
                        if (it.name != null && entryVersion != null) {
                            val packs = downloadPackage(it.name!!, entryVersion, null)
                            if (packs.size > 0) {
                                for (pack in packs) {
                                    packages.add(pack)
                                }
                            }
                        }
                    }
                }
            }
            if (dependency.isJsonNull) logger.info("isNull")
            if (dependency.isJsonPrimitive) logger.info("isJsonPrimitive")
        } else {
            logger.info("No dependencies found for {} - {}",name,version)
        }

        packages.add(npmPackage)


        return packages
    }

    fun readFromUrl(url: String): InputStream {

        val myUrl =  URL(url)

        var retry = 2
        while (retry > 0) {
            val conn = myUrl.openConnection() as HttpURLConnection


            conn.requestMethod = "GET"

            try {
                conn.connect()
                return conn.inputStream
            } catch (ex: FileNotFoundException) {
                retry--
                if (retry < 1) throw UnprocessableEntityException(ex.message)
            } catch (ex: IOException) {
                retry--
                if (retry < 1) throw UnprocessableEntityException(ex.message)

            }
        }
        throw UnprocessableEntityException("Number of retries exhausted")
    }

    private fun getBase(profile : String,supportChain: IValidationSupport): String? {
        val structureDefinitionResource = supportChain.fetchStructureDefinition(profile)
        if (structureDefinitionResource === null) {
            logger.error("Issue retrieving " + profile)
            return null;
        }
        val structureDefinition = structureDefinitionResource as StructureDefinition;

        if (structureDefinition.hasBaseDefinition()) {
            var baseProfile = structureDefinition.baseDefinition
            if (baseProfile.contains(".uk")) baseProfile = getBase(baseProfile, supportChain)
            return baseProfile
        }
        return null;
    }
    private fun shouldGenerateSnapshot(structureDefinition: StructureDefinition): Boolean {
        return !structureDefinition.hasSnapshot() && structureDefinition.derivation == StructureDefinition.TypeDerivationRule.CONSTRAINT
    }
}

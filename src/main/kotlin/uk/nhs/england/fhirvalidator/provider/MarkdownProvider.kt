package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.rest.annotation.Operation
import ca.uhn.fhir.rest.annotation.OperationParam
import ca.uhn.fhir.rest.param.StringParam
import mu.KLogging
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.service.oas.CapabilityStatementToOpenAPIConversion
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class MarkdownProvider (
    private val oasParser : CapabilityStatementToOpenAPIConversion,
    @Qualifier("SupportChain") private val supportChain: IValidationSupport,
) {
    companion object : KLogging()

    @Operation(name = "\$markdown", idempotent = true,manualResponse=true, manualRequest=true)
    @Throws(Exception::class)
    fun createProfileMarkdown(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        @OperationParam(name="url") profile: StringParam?
    ) {
        servletResponse.setContentType("text/markdown")
        servletResponse.setCharacterEncoding("UTF-8")
        if (profile != null) {
            servletResponse.writer.write(generateMarkdown(java.net.URLDecoder.decode(profile.value, StandardCharsets.UTF_8.name())))
        }
        servletResponse.writer.flush()
        return
    }

    private fun getElementDescription(element : ElementDefinition) : String {

        var table = "\n\n| | |\n|----|----|"
        // header body
        /*
        if (element.hasMustSupport() && element.mustSupport) {
            description += "\n `mustSupport`"
        }
*/
        table += "\n|Element Id|"+element.id+"|"
        table += "\n|[Cardinality](https://www.hl7.org/fhir/conformance-rules.html#cardinality)|"+element.min+".."+element.max+"|"


        if (element.hasFixed()) {
            if (element.fixed is UriType) {
                table += "\n|Fixed Value|"+(element.fixed as UriType).value+"|"
            }
            if (element.fixed is CodeType) {
                table += "\n|Fixed Value|"+(element.fixed as CodeType).value+"|"
            }
        }

        if (element.hasBinding()) {
            if (element.binding.hasValueSet())
            {
                val valueSet = supportChain.fetchValueSet(element.binding.valueSet)
                if (valueSet !== null) {
                    var description =
                        "[" + (valueSet as ValueSet).name + "](" + oasParser.getDocumentationPath(element.binding.valueSet) + ")"
                    if (element.binding.hasStrength()) description += " (" + element.binding.strength.display + ")"
                    if (element.binding.hasDescription()) {
                        var elementDescription = element.binding.description
                        elementDescription = " <br/>" + elementDescription.replace("\\n", "\n")
                        description += elementDescription + " "
                    }
                    table += "\n|[Terminology Binding](https://www.hl7.org/fhir/terminologies.html)|" + description + "|"
                }
            }
        }

        if (element.hasSliceName()) {
            table += "\n|[Slice Name](https://www.hl7.org/fhir/profiling.html#slicing)|"+element.sliceName+"|"
        }
        if (element.hasSlicing()) {
            var description = ""
            if (element.slicing.hasRules()) {
                description += " *"+element.slicing.rules.name + "*"
            }
            if (element.slicing.hasDiscriminator()) {
                for (discrimninator in element.slicing.discriminator) {
                    description += " discriminator - "
                    if (discrimninator.hasType()) {
                        description += " *"+discrimninator.type.name + "*"
                    }
                    if (discrimninator.hasPath()) {
                        description += " *"+discrimninator.path + "*"
                    }
                }
            }
            if (element.slicing.hasDescription()) {
                description += "<br/> "+element.slicing.description
            }
            table += "\n|[Slicing](https://www.hl7.org/fhir/profiling.html#slicing)|"+description+"|"
        }

        // Data type
        if (element.hasType()) {
            var description = ""
            for (type in element.type) {

                description += "["+type.code+"](https://www.hl7.org/fhir/datatypes.html#"+type.code+")"
                var itemDescription=""
                var first = true
                for (target in type.targetProfile) {
                    if (itemDescription.isEmpty()) description+= "("
                    val profile = supportChain.fetchStructureDefinition(target.value)
                    if (!first) {
                        itemDescription += " "
                    } else {
                        first = false
                    }
                    itemDescription += "["+(profile as StructureDefinition).name + "]("+ oasParser.getDocumentationPath(target.value) +")"

                }
                first = true
                for (target in type.profile) {
                    if (itemDescription.isEmpty()) description+= "("
                    val profile = supportChain.fetchStructureDefinition(target.value)
                    if (!first) {
                        itemDescription += " "
                    } else {
                        first = false
                    }
                    itemDescription += "["+(profile as StructureDefinition).name+ "]("  + oasParser.getDocumentationPath(target.value) +")"
                }

                if (itemDescription.isNotEmpty()) description+= itemDescription + ")"
                if (type.hasAggregation()) {
                    for (aggregation in type.aggregation) {
                        description += "<br/> Aggregation - [" + aggregation.code+"](http://www.hl7.org/fhir/valueset-resource-aggregation-mode.html)"
                    }
                }
            }
            table += "\n|[type](https://www.hl7.org/fhir/datatypes.html)|"+description+"|"
        }
        var description = table + "\n\n<br/>"
        // Documentation section
        /*
        if (element.hasShort()) {

            description += "\n\n " + element.short
        }
         */

        if (element.hasDefinition()) {
            description += "\n\n #### Definition"
            description += "\n\n " + element.definition.replace("\\n","\n")
        }

        if (element.hasRequirements()) {
            description += "\n\n #### Requirements"
            description += "\n\n " + element.requirements.replace("\\n","\n")
        }

        if (element.hasComment()) {
            description += "\n\n #### Comment"
            description += "\n\n " + element.comment.replace("\\n","\n")
        }

        if (element.hasConstraint()) {
            var displayConstraints = false
            for (constraint in element.constraint) {
                if (doDisplay(constraint.key)) displayConstraints = true
            }

            if (displayConstraints) {
                description += "\n\n #### Constraints \n"
                for (constraint in element.constraint) {
                    if (doDisplay(constraint.key)) description += "\n- **" + constraint.key + "** (*" + constraint.severity + "*) " + constraint.human.replace(
                        "\\n",
                        "\n"
                    )
                }
            }
        }

        return description
    }

    fun generateMarkdown(profile :String) : String {
        var mainDescription = ""
        var subDescription = ""
        var index = ""


        if (profile != null) {
            val structureDefinition = oasParser.getProfile(profile)
            if (structureDefinition is StructureDefinition) {

                if (structureDefinition.hasDescription()) {
                    mainDescription += "\n\n " + structureDefinition.description
                }
                if (structureDefinition.hasPurpose()) {
                    mainDescription += "\n\n " + structureDefinition.purpose
                }

                for (element in structureDefinition.snapshot.element) {
                    val paths = element.path.split(".")
                    if (
                        element.hasMustSupport() || element.hasFixed() || (element.hasSliceName() && !paths[paths.size-1].equals("extension"))
                        || element.id.split(".").size == 1) {
                        val paths = element.id.split(".")
                        var title = ""


                        if (paths.size>1){
                            for (i in 2..paths.size) {
                                if (title.isNotEmpty()) title += "."
                                title += paths[i-1]
                            }
                            index += "\n- <a href=\"#"+title+"\">"+title+"</a>"
                            subDescription += "\n\n<a name=\""+title+"\"></a>\n ## "+title
                            subDescription += getElementDescription(element)
                        } else {
                            mainDescription += getElementDescription(element)
                        }



                    }
                }
            }
        }
        return mainDescription + "\n\n" + index + subDescription
    }

    private fun doDisplay(key : String) : Boolean {
        if (key.startsWith("ext")) return false
        if (key.startsWith("ele")) return false
        if (key.startsWith("dom")) return false
        return true
    }

}

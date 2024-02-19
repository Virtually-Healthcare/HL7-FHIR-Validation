package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.apache.commons.io.output.ByteArrayOutputStream
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import uk.nhs.england.fhirvalidator.util.MyUriResolver
import java.io.ByteArrayInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource


@Component
class CompostionProvider(@Qualifier("R4") private val fhirContext: FhirContext,
                         private val supportChain: ValidationSupportChain)  : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<Composition> {
        return Composition::class.java
    }
    var implementationGuideParser: ImplementationGuideParser? = ImplementationGuideParser(fhirContext)

    private val log = LoggerFactory.getLogger("FHIRAudit")

   
    @Operation(name = "\$convert", idempotent = true,manualResponse=true, manualRequest=false)
    fun convertOpenAPI(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        @ResourceParam document: Bundle,
        @OperationParam(name = "enhance") abstract: BooleanType?,
    ) {
        if (document.type !== Bundle.BundleType.DOCUMENT) {
            throw UnprocessableEntityException("Not a FHIR Document")
        }
        // Set the property to use saxon processor
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.BasicTransformerFactory")

        // Saxon is hanging at the moment so using xalan
        System.setProperty(
            "javax.xml.transform.TransformerFactory",
            "org.apache.xalan.processor.TransformerFactoryImpl"
        )
        val xml = this.fhirContext.newXmlParser().encodeResourceToString(document)

        val xmlSource = StreamSource(xml.byteInputStream())
        val classLoader = javaClass.classLoader

        val inputStream = classLoader.getResourceAsStream("xslt/DocumentToHTML.xslt")
        val xsltSource = StreamSource(inputStream)

        val transformerFactory = TransformerFactory.newInstance()
        //val transformerFactory = net.sf.saxon.TransformerFactoryImpl()
        transformerFactory.setURIResolver(MyUriResolver())

        val transformer: Transformer = transformerFactory.newTransformer(xsltSource)

        val os = ByteArrayOutputStream()
        val result: StreamResult = StreamResult(os)
        try {
            transformer.transform(xmlSource, result)
            println("XSLT transformation completed successfully.")
        } catch (ex : Exception) {
            log.error(ex.message)
        }
        val output = ByteArrayInputStream(os.toByteArray())

        servletResponse.setContentType("text/html")
        servletResponse.setCharacterEncoding("UTF-8")

        val buffer = ByteArray(1024*8)

        var j = -1
        while (output.read(buffer).also { j = it } > 0) {
            servletResponse.writer.write(buffer.decodeToString(0,j))
        }

        servletResponse.writer.flush()
        return
    }

}

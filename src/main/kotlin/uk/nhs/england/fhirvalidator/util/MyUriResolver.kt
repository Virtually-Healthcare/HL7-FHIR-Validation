package uk.nhs.england.fhirvalidator.util

import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.URIResolver
import javax.xml.transform.stream.StreamSource

internal class MyUriResolver : URIResolver {
    @Throws(TransformerException::class)
    override fun resolve(href: String, base: String?): Source? {
        try {
            val inputStream = this.javaClass.classLoader.getResourceAsStream("xslt/$href")
            return StreamSource(inputStream)
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }
}

package uk.nhs.england.fhirvalidator.controller

import io.swagger.v3.oas.annotations.Hidden
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import kotlin.math.log

@RestController
@Hidden
class StatusController {
    companion object : KLogging()
    @GetMapping("_status")
    fun validate(): String {
        return "Validator is alive"
    }
    @ExceptionHandler
    @ResponseStatus(value= HttpStatus.NOT_FOUND)
    fun conflict() {
        // Nothing to do
        logger.info("not found")
    }
}

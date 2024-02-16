package uk.nhs.england.fhirvalidator.controller

import mu.KLogging
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Controller
class ErrorController : ErrorController {
    companion object : KLogging()
    @RequestMapping("/error")
    fun handleError(request : HttpServletRequest, response: HttpServletResponse): String {
        val status: Any = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)

        val statusCode = status.toString().toInt()

        if (statusCode.equals(404)) {
            response.sendRedirect("/")
            return "error-404"

        }
        return "error"
    }
}

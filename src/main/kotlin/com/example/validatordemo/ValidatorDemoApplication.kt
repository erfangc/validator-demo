package com.example.validatordemo

import org.openapi4j.core.validation.ValidationException
import org.openapi4j.operation.validator.model.Request
import org.openapi4j.operation.validator.model.impl.Body
import org.openapi4j.operation.validator.model.impl.DefaultRequest
import org.openapi4j.operation.validator.validation.RequestValidator
import org.openapi4j.parser.OpenApi3Parser
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.io.File

@SpringBootApplication
class ValidatorDemoApplication

fun main(args: Array<String>) {
    
    val spec = File("openapi.json")
    val api3 = OpenApi3Parser().parse(spec, true)
    val requestValidator = RequestValidator(api3)
    
    // Pretend this is the request body we receive from the API caller
    val json = """
        {
          "id": 10,
          "id2": 10,
          "name": false,
          "category": {
            "id": 1,
            "name": "Dogs"
          },
          "tags": [
            {
              "id": 0,
              "name": "string"
            }
          ],
          "status": "available"
        }
    """.trimIndent()
    
    val request = DefaultRequest
        .Builder("/api/v3/pet", Request.Method.PUT)
        .header("Content-Type", "application/json")
        .body(Body.from(json)).build()
    
    try {
        requestValidator.validate(request)
        println("Request is valid!")
    } catch (e: ValidationException) {
        val results = e.results()
        // this is an example of the kind of error we want to return to the caller to assist them wit understanding what's wrong
        if (!results.isValid) {
            for (item in results.items()) {
                val splits = item.dataCrumbs().split(".")
                val fieldName = if (splits.size > 1) {
                    splits.subList(1, splits.size).joinToString(".")
                } else {
                    ""
                }
                if (fieldName.isNotBlank()) {
                    println("${item.message()} Affected field: '${fieldName}'.")
                } else {
                    println(item.message())
                }
            }       
        }
    }
}

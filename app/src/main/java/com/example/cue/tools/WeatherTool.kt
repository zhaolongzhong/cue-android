package com.example.cue.tools

enum class WeatherUnit(val symbol: String) {
    CELSIUS("C"),
    FAHRENHEIT("F")
}

class WeatherTool : LocalTool {
    override val name: String = "get_current_weather"
    override val description: String = "Get the current weather in a given location"
    override val parameterSchema = ParameterSchema(
        properties = mapOf(
            "location" to ParameterProperty(
                type = "string",
                description = "The city and state, e.g. San Francisco, CA"
            ),
            "unit" to ParameterProperty(
                type = "string",
                description = "Temperature unit (C or F)"
            )
        ),
        required = listOf("location")
    )

    override suspend fun call(args: ToolArguments): String {
        val location = args.getString("location") 
            ?: throw ToolError.InvalidArguments("Missing location")
        val unit = args.getString("unit") ?: "F"
        
        return withIO {
            WeatherService.getWeather(location, unit)
        }
    }
}

// Mock weather service
object WeatherService {
    suspend fun getWeather(location: String, unit: String): String {
        return "61$unit in $location"
    }
}
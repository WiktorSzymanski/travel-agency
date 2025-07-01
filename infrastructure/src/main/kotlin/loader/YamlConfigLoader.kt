package loader

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

class YamlConfigLoader {
    val mapper =
        ObjectMapper(YAMLFactory()).apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }

    inline fun <reified T> loadConfig(filePath: String): List<T> = mapper.readValue(File(filePath))
}

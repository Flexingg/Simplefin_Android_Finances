package com.randallengineering.finances.core.ai

import com.randallengineering.finances.core.network.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiApiClient(
    private val mcpTools: FinancialMcpTools
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Executes a chat turn with Gemini, handling any tool calls automatically.
     */
    suspend fun generateChatResponse(
        apiKey: String,
        modelName: String = "gemini-2.5-flash",
        systemPrompt: String,
        userMessage: String,
        history: List<Pair<String, String>> = emptyList() // Pair(role "user"/"model", text)
    ): Resource<GeminiChatOutput> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Resource.Error("Gemini API key is missing. Please configure it in Settings.")
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val executedTools = mutableListOf<ToolExecutionResult>()

            // Build tools declaration array from MCP tools
            val toolsArray = buildToolsDeclaration()

            // Build initial contents array
            val contentsArray = mutableListOf<JsonObject>()

            // Add previous history
            for ((role, text) in history) {
                contentsArray.add(
                    buildJsonObject {
                        put("role", role)
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", text) })
                        })
                    }
                )
            }

            // Add current user prompt
            contentsArray.add(
                buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", userMessage) })
                    })
                }
            )

            var maxTurns = 4
            var assistantFinalText = ""

            while (maxTurns > 0) {
                maxTurns--

                val requestPayload = buildJsonObject {
                    put("contents", JsonArray(contentsArray))
                    put("tools", JsonArray(listOf(buildJsonObject {
                        put("function_declarations", toolsArray)
                    })))
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", systemPrompt) })
                        })
                    })
                }

                val responseJson = executeHttpRequest(endpoint, requestPayload.toString())
                val candidates = responseJson["candidates"]?.jsonArray
                val firstCandidate = candidates?.firstOrNull()?.jsonObject
                val content = firstCandidate?.get("content")?.jsonObject
                val parts = content?.get("parts")?.jsonArray

                if (parts == null || parts.isEmpty()) {
                    val promptFeedback = responseJson["promptFeedback"]
                    return@withContext Resource.Error("Empty response from Gemini: $promptFeedback")
                }

                // Check for function call
                val functionCallPart = parts.find { it.jsonObject.containsKey("functionCall") }?.jsonObject?.get("functionCall")?.jsonObject
                val textPart = parts.find { it.jsonObject.containsKey("text") }?.jsonObject?.get("text")?.jsonPrimitive?.content

                if (textPart != null) {
                    assistantFinalText = textPart
                }

                if (functionCallPart != null) {
                    val toolName = functionCallPart["name"]?.jsonPrimitive?.content.orEmpty()
                    val toolArgs = functionCallPart["args"]?.toString() ?: "{}"

                    // Execute MCP tool locally
                    val toolResult = mcpTools.executeTool(toolName, toolArgs)
                    executedTools.add(toolResult)

                    // Append model's functionCall turn to contents
                    contentsArray.add(
                        buildJsonObject {
                            put("role", "model")
                            put("parts", buildJsonArray {
                                add(buildJsonObject {
                                    put("functionCall", functionCallPart)
                                })
                            })
                        }
                    )

                    // Append function execution response turn to contents
                    contentsArray.add(
                        buildJsonObject {
                            put("role", "function")
                            put("parts", buildJsonArray {
                                add(buildJsonObject {
                                    put("functionResponse", buildJsonObject {
                                        put("name", toolName)
                                        put("response", buildJsonObject {
                                            put("name", toolName)
                                            put("success", toolResult.success)
                                            put("content", toolResult.message)
                                            if (toolResult.dataJson != null) {
                                                put("data", toolResult.dataJson)
                                            }
                                        })
                                    })
                                })
                            })
                        }
                    )
                    // Continue loop so Gemini synthesizes final response with function outcome
                } else {
                    // No function call: final answer obtained
                    break
                }
            }

            Resource.Success(
                GeminiChatOutput(
                    responseText = assistantFinalText.ifBlank { "Tool executed successfully." },
                    executedTools = executedTools
                )
            )
        } catch (e: Exception) {
            Resource.Error("Gemini API Error: ${e.localizedMessage ?: e.message}", e)
        }
    }

    private fun buildToolsDeclaration(): JsonArray {
        return buildJsonArray {
            for (tool in mcpTools.availableTools) {
                try {
                    val paramsElement = json.parseToJsonElement(tool.parametersJson)
                    add(buildJsonObject {
                        put("name", tool.name)
                        put("description", tool.description)
                        put("parameters", paramsElement)
                    })
                } catch (e: Exception) {
                    // Fallback to empty object parameters
                    add(buildJsonObject {
                        put("name", tool.name)
                        put("description", tool.description)
                        put("parameters", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {})
                        })
                    })
                }
            }
        }
    }

    private fun executeHttpRequest(urlString: String, jsonBody: String): JsonObject {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.doOutput = true

        OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
            os.write(jsonBody)
            os.flush()
        }

        val statusCode = conn.responseCode
        val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
        val responseBody = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

        if (statusCode !in 200..299) {
            throw RuntimeException("HTTP $statusCode: $responseBody")
        }

        return json.parseToJsonElement(responseBody).jsonObject
    }
}

data class GeminiChatOutput(
    val responseText: String,
    val executedTools: List<ToolExecutionResult> = emptyList()
)

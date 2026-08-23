package com.randallengineering.finances.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

object FlexibleErrorListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = ListSerializer(JsonElement.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val input = decoder as? JsonDecoder ?: return emptyList()
        return try {
            when (val element = input.decodeJsonElement()) {
                is JsonArray -> {
                    element.mapNotNull { item ->
                        when (item) {
                            is JsonPrimitive -> item.content
                            is JsonObject -> {
                                val msg = item["msg"]?.jsonPrimitive?.content
                                    ?: item["message"]?.jsonPrimitive?.content
                                    ?: item["error"]?.jsonPrimitive?.content
                                val code = item["code"]?.jsonPrimitive?.content
                                if (!code.isNullOrBlank() && !msg.isNullOrBlank()) {
                                    "[$code] $msg"
                                } else {
                                    msg ?: code ?: item.toString()
                                }
                            }
                            else -> item.toString()
                        }
                    }
                }
                is JsonPrimitive -> listOf(element.content)
                is JsonObject -> {
                    val msg = element["msg"]?.jsonPrimitive?.content
                        ?: element["message"]?.jsonPrimitive?.content
                        ?: element["error"]?.jsonPrimitive?.content
                        ?: element.toString()
                    listOf(msg)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        val list = value.map { JsonPrimitive(it) }
        val array = JsonArray(list)
        (encoder as? JsonDecoder) // No-op serialization
    }
}

@Serializable
data class SimpleFinAccountsResponse(
    @Serializable(with = FlexibleErrorListSerializer::class)
    val errors: List<String> = emptyList(),
    @Serializable(with = FlexibleErrorListSerializer::class)
    val errlist: List<String> = emptyList(),
    val accounts: List<SimpleFinAccountDto> = emptyList()
) {
    val allErrors: List<String>
        get() = (errors + errlist).distinct()
}

@Serializable
data class SimpleFinAccountDto(
    val id: String,
    val name: String = "",
    val currency: String = "USD",
    val balance: String = "0.00",
    @SerialName("available-balance")
    val availableBalance: String? = null,
    @SerialName("balance-date")
    val balanceDate: Long? = null,
    val org: SimpleFinOrgDto? = null,
    val transactions: List<SimpleFinTransactionDto> = emptyList()
)

@Serializable
data class SimpleFinOrgDto(
    @SerialName("domain")
    val domain: String? = null,
    @SerialName("sfin-url")
    val sfinUrl: String? = null,
    @SerialName("name")
    val name: String? = null
)

@Serializable
data class SimpleFinTransactionDto(
    val id: String,
    val posted: Long = 0L,
    val amount: String = "0.00",
    val description: String = "",
    val payee: String? = null,
    val memo: String? = null,
    val pending: Boolean = false
)

package sdk.base

enum class IntervalTypes {
    live, quarterly, yearOverYearQuarterly, yearly, trailing
}

data class FinancialFormat(
    val key: String,
    val name: String,
    val description: String?,
    val suffix: String?,
    val prefix: String?,
    val multiplier: Double = 1.0,
    val precision: Int = 0,
    val display: Boolean = true,
    val interval: IntervalTypes
) : GenericModel() {

    fun copyWith(
        key: String? = this.key,
        name: String? = this.name,
        description: String? = this.description,
        suffix: String? = this.suffix,
        prefix: String? = this.prefix,
        multiplier: Double? = this.multiplier,
        precision: Int? = this.precision,
        display: Boolean? = this.display,
        interval: IntervalTypes? = this.interval
    ): FinancialFormat {
        return FinancialFormat(
            key ?: this.key,
            name ?: this.name,
            description ?: this.description,
            suffix ?: this.suffix,
            prefix ?: this.prefix,
            multiplier ?: this.multiplier,
            precision ?: this.precision,
            display ?: this.display,
            interval ?: this.interval
        )
    }

    override fun toJson(): Map<String, Any?> {
        return mapOf(
            "key" to key,
            "name" to name,
            "description" to description,
            "suffix" to suffix,
            "prefix" to prefix,
            "multiplier" to multiplier,
            "precision" to precision,
            "display" to display,
            "interval" to interval.key
        )
    }

    override fun toString(): String {
        return "$key: ${suffix ?: ""}, ${prefix ?: ""}"
    }


    fun withName(name: String): FinancialFormat {
        return copyWith(name = name)
    }

    companion object {
        fun fromJSON(json: Map<String, Any>): FinancialFormat {
            return FinancialFormat(
                key = json["key"].toString(),
                name = json["name"].toString(),
                description = json["description"]?.toString(),
                suffix = json["suffix"]?.toString(),
                prefix = json["prefix"]?.toString(),
                multiplier = json["multiplier"].toString().toDouble(),
                precision = json["precision"].toString().toInt(),
                display = json["display"].toString().toBoolean(),
                interval = _intervalTypeFromString(json["interval"].toString())
            )
        }

        fun fromChatBotRatiosJSON(json: Map<String, Any>): FinancialFormat {
            return FinancialFormat(
                key = json["slug"].toString(),
                name = json["name"].toString(),
                description = json["description"]?.toString(),
                suffix = json["suffix"]?.toString(),
                prefix = json["prefix"]?.toString(),
                multiplier = json["multiplier"].toString().toDouble(),
                precision = json["precision"].toString().toInt(),
                display = json["display"]?.toString()?.toBoolean() ?: true,
                interval = _intervalTypeFromString(json["interval"].toString())
            )
        }

        private fun _intervalTypeFromString(text: String): IntervalTypes {
            return when (text) {
                "live" -> IntervalTypes.live
                "quarterly" -> IntervalTypes.quarterly
                "yoy-quarterly" -> IntervalTypes.yearOverYearQuarterly
                "yearly" -> IntervalTypes.yearly
                "trailing" -> IntervalTypes.trailing
                else -> IntervalTypes.quarterly
            }
        }
    }
}

val IntervalTypes.key: String
    get() {
        return when (this) {
            IntervalTypes.live -> "live"
            IntervalTypes.quarterly -> "quarterly"
            IntervalTypes.yearOverYearQuarterly -> "yoy-quarterly"
            IntervalTypes.yearly -> "yearly"
            IntervalTypes.trailing -> "trailing"
        }
    }
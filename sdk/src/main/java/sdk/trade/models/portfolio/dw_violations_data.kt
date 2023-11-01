package sdk.trade.models.portfolio

import sdk.base.GenericModel
import java.time.LocalDateTime

data class DriveWealthViolationsData(
    val goodFaithViolationCount: Int,
    val patternDayTrades: List<DriveWealthPatternDayTradeData>
) : GenericModel {
    companion object {
        fun fromJSON(json: Map<String, Any?>): DriveWealthViolationsData {
            val goodFaithViolations:Map<String,Any> = json["goodFaithViolations"] as Map<String, Any>
            val patternDayTrades:List<Map<String, Any>> = json["patternDayTrades"] as List<Map<String, Any>>
            return DriveWealthViolationsData(
                goodFaithViolations["count"] as? Int ?: 0,
                (patternDayTrades).map {
                    DriveWealthPatternDayTradeData.fromJSON(it)
                }
            )
        }
    }

    override fun toJson(): Map<String, Any> {
        return mapOf(
            "goodFaithViolations" to mapOf("count" to goodFaithViolationCount),
            "patternDayTrades" to patternDayTrades.map { it.toJson() }
        )
    }
}

data class DriveWealthPatternDayTradeData(
    val id: String,
    val symbol: String,
    val quantity: Double,
    val amount: Double,
    val side: String,
    val createdAt: LocalDateTime?,
    val orderId: String,
    val orderNo: String,
    val legacyGoodFaithViolations: Int,
    val message: String,
    val violationSells: List<DriveWealthPatternDayTradeViolationSellData>
) : GenericModel {
    companion object {
        fun fromJSON(json: Map<String, Any?>): DriveWealthPatternDayTradeData {
            return DriveWealthPatternDayTradeData(
                json["id"] as String,
                json["symbol"] as String,
                (json["qty"] as Number).toDouble(),
                (json["amount"] as Number).toDouble(),
                json["side"] as String,
                (json["createdWhen"] as String).let { LocalDateTime.parse(it) },
                json["orderID"] as String,
                json["orderNo"] as String,
                json["legacyGoodFaithViolations"] as Int,
                json["message"] as String,
                (json["violationSells"] as List<Map<String, Any?>>).map {
                    DriveWealthPatternDayTradeViolationSellData.fromJSON(it)
                }
            )
        }
    }

    override fun toJson(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "symbol" to symbol,
            "qty" to quantity,
            "amount" to amount,
            "side" to side,
            "createdWhen" to createdAt?.toString(),
            "orderID" to orderId,
            "orderNo" to orderNo,
            "legacyGoodFaithViolations" to legacyGoodFaithViolations,
            "message" to message,
            "violationSells" to violationSells.map { it.toJson() }
        )
    }
}

data class DriveWealthPatternDayTradeViolationSellData(
    val quantity: Double,
    val amount: Double,
    val createdAt: LocalDateTime?,
    val orderId: String,
    val orderNo: String,
    val side: String
) : GenericModel {
    companion object {
        fun fromJSON(json: Map<String, Any?>): DriveWealthPatternDayTradeViolationSellData {
            return DriveWealthPatternDayTradeViolationSellData(
                (json["qty"] as Number).toDouble(),
                (json["amount"] as Number).toDouble(),
                (json["createdWhen"] as String).let { LocalDateTime.parse(it) },
                json["orderID"] as String,
                json["orderNo"] as String,
                json["side"] as String
            )
        }
    }

    override fun toJson(): Map<String, Any?> {
        return mapOf(
            "qty" to quantity,
            "amount" to amount,
            "createdWhen" to createdAt?.toString(),
            "orderID" to orderId,
            "orderNo" to orderNo,
            "side" to side
        )
    }
}

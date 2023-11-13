package sdk.models.data.account

import sdk.base.GenericModel

typealias ProfileImageGetter = suspend(String) -> Any

data class AccountData(
    val username: String,
    val email: String,
    val phoneNumber: String?,
    val firstName: String?,
    val lastName: String?,
    val referralCode: String?,
    val referralPoints: Double?,
    val referredUsers: List<String>?,
    val referredCode: String?,
    val realTradeAccountId: Int?,
    val driveWealthRealTradeAccountId: String?,
    val driveWealthRealTradeAccountNo: String?
) : GenericModel {
    var isPremium: Boolean = false
    var isTester: Boolean = false
    var isRealTradeTester: Boolean = false

    val hasRealTradeAccount: Boolean
        get() = realTradeAccountId != null
    val hasDriveWealthRealTradeAccount: Boolean
        get() = driveWealthRealTradeAccountId != null

    override fun toJson(): Map<String, Any?> {
        return mapOf(
            "username" to username,
            "email" to email,
            "phone_number" to phoneNumber,
            "first_name" to firstName,
            "last_name" to lastName,
            "referral_code" to referralCode,
            "referral_points" to referralPoints,
            "referred_users" to referredUsers,
            "referred_code" to referredCode,
            "account_id" to realTradeAccountId,
            "dw_account_id" to driveWealthRealTradeAccountId,
            "dw_account_no" to driveWealthRealTradeAccountNo
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any?>): AccountData {
            var name = json["first_name"]
            var surname = json["last_name"]
            if (name != null && name is String) {
                name = (name).trim()
            }
            if (surname != null && surname is String) {
                surname = (surname).trim()
            }
            return AccountData(
                json["username"] as String,
                json["email"] as String,
                json["phone_number"] as String,
                name as String?,
                surname as String?,
                json["referral_code"] as String,
                json["referral_points"] as Double,
                (json["referred_users"] as? List<String>) ?: emptyList(),
                json["referred_code"] as? String,
                json["account_id"] as? Int,
                json["dw_account_id"] as? String,
                json["dw_account_no"] as String
            )
        }
    }
}

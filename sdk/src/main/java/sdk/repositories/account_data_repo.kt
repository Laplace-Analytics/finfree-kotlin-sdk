package sdk.repositories

import sdk.api.AuthApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.models.data.account.AccountData

class AccountDataRepo(
    apiProvider: AuthApiProvider,
    storageHandler: GenericStorage
) : GenericRepository<AccountData, Map<String, Any>, AuthApiProvider>(storageHandler,apiProvider) {

    suspend fun requestLogin(identifier: String, password: String): Any? {
        val response = apiProvider.postLogin(identifier, password)
        return if (response.responseType == LoginResponseTypes.SUCCESS) {
            val accountDataResponse = apiProvider.getAccountData()
            if (accountDataResponse.data != null) {
                val accountData = getFromJson(accountDataResponse.data)
                saveData(accountData)
                accountData
            } else {
                logger.error("Successful login, unsuccessful account data request.\nidentifier: $identifier\nmessage:${accountDataResponse.message}")
                "Kullanıcı verisi alınırken hata oluştu!"
            }
        } else {
            response.message
        }
    }

    override suspend fun fetchData(identifier: Map<String, Any>?): AccountData? {
        val data = apiProvider.getAccountData().data
        return data?.let { getFromJson(it) }
    }

    override fun getFromJson(json: Map<String, Any?>): AccountData {
        return AccountData.fromJson(json)
    }

    override fun getPath(identifier: Map<String, Any>?): String {
        return "app_files_2.5/user_data/account_data.json"
    }

    override fun toJson(data: AccountData): Map<String, Any?> {
        return data.toJson()
    }

    override fun getIdentifier(data: AccountData): Map<String, Any>? {
        throw NotImplementedError()
    }
}

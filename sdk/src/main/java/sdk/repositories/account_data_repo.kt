package sdk.repositories

import sdk.api.AuthApiProvider
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.models.data.account.AccountData

class AccountDataRepo(
    apiProvider: AuthApiProvider,
    storageHandler: GenericStorage
) : GenericRepository<AccountData, Map<String, Any>, AuthApiProvider>(storageHandler,apiProvider) {

    override suspend fun fetchData(identifier: Map<String, Any>?): AccountData? {
        val data = apiProvider.getAccountData().data
        return data?.let { getFromJson(it) }
    }

    override fun getFromJson(json: Map<String, Any?>): AccountData {
        return AccountData.fromJson(json)
    }

    override fun getPath(identifier: Map<String, Any>?): String {
        return "user_data/account_data.json"
    }

    override fun toJson(data: AccountData): Map<String, Any?> {
        return data.toJson()
    }

    override fun getIdentifier(data: AccountData): Map<String, Any>? {
        throw NotImplementedError()
    }
}

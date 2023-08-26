package models.data.assets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sdk.models.Asset
import sdk.models.AssetType
import sdk.models.Region

class AssetModelCreationTests{
    val americanMarketAssetAsJson = mutableMapOf(
        "id" to "6203d1ba1e67487527555dea",
        "asset_type" to "stock",
        "name" to "Safety Insurance Group, Inc.",
        "symbol" to "SAFT",
        "sector_id" to "620515fc955b8cb965b68f97",
        "industry_id" to "6205161f955b8cb965b69046",
        "a" to true,
        "region" to "us"
    )

    val turkishMarketAssetAsJson = mutableMapOf(
        //ids is not same with original data
        "id" to "6203d1ba1e67487527555dea",
        "asset_type" to "stock",
        "name" to "Türk Hava Yolları",
        "symbol" to "THYAO",
        "sector_id" to "620515fc955b8cb965b68f97",
        "industry_id" to "6205161f955b8cb965b69046",
        "a" to true,
        "region" to "tr"
    )

    val expectedAmericanMarketAsset = Asset(
        id = "6203d1ba1e67487527555dea",
        industryId = "6205161f955b8cb965b69046",
        sectorId = "620515fc955b8cb965b68f97",
        name = "Safety Insurance Group, Inc.",
        symbol = "SAFT",
        tradable = true,
        isActive = true,
        type = AssetType.stock,  // Adjust if necessary
        region = Region.american  // Adjust if necessary
    )

    val expectedTurkishMarketAsset = Asset(
        //ids is not same with original data
        id = "6203d1ba1e67487527555dea",
        industryId = "6205161f955b8cb965b69046",
        sectorId = "620515fc955b8cb965b68f97",
        name = "Türk Hava Yolları",
        symbol = "THYAO",
        tradable = true,
        isActive = true,
        type = AssetType.stock,  // Adjust if necessary
        region = Region.turkish  // Adjust if necessary
    )
    @Test
    fun `Turkish market asset creation test`() {
        assertEquals(expectedTurkishMarketAsset, Asset.fromJson(turkishMarketAssetAsJson))

        turkishMarketAssetAsJson.remove("region")
        assertThrows<Exception> {
            Asset.fromJson(turkishMarketAssetAsJson)
        }
    }

    @Test
    fun `American market asset creation test`() {
        assertEquals(expectedAmericanMarketAsset, Asset.fromJson(americanMarketAssetAsJson))
    }
}
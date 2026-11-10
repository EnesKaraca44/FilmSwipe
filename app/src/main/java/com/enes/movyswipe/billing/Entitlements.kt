package com.enes.movyswipe.billing

/** Satın alma listesinden "reklamsız" hakkının olup olmadığına karar veren saf mantık. */
object Entitlements {
    const val REMOVE_ADS = "remove_ads"

    /** Basit satın alma görünümü (Purchase sınıfına bağımlı olmadan test edilebilsin diye). */
    data class OwnedProduct(val products: List<String>, val purchased: Boolean)

    fun hasRemoveAds(owned: List<OwnedProduct>): Boolean =
        owned.any { it.purchased && REMOVE_ADS in it.products }
}

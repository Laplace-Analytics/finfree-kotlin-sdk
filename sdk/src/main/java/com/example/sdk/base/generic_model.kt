package com.example.sdk.base

abstract class GenericModel {
    abstract fun toJson(): Map<String, Any?>
}
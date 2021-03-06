package com.apollographql.apollo.compiler.ir

data class Argument(
    val name: String,
    val value: Any,
    val type: String,
    val sourceLocation: SourceLocation = SourceLocation.UNKNOWN
)

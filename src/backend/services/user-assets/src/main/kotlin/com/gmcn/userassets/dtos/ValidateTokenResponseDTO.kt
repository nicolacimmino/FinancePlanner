package com.gmcn.userassets.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class ValidateTokenResponseDTO(
    @JsonProperty("subject")
    var subject: String,

    @JsonProperty("valid")
    var valid: Boolean,
)

package com.cimminonicola.finanaceplanneraccounts.datasource

import com.cimminonicola.finanaceplanneraccounts.model.Account
import org.springframework.data.mongodb.repository.MongoRepository

interface AccountDataSource : MongoRepository<Account, String> {
    fun existsByName(name: String): Boolean
    fun deleteByName(name: String)
}
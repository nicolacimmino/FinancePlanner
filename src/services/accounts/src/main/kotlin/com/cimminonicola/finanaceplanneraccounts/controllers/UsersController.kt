package com.cimminonicola.finanaceplanneraccounts.controllers

import com.cimminonicola.finanaceplanneraccounts.ApplicationStatus
import com.cimminonicola.finanaceplanneraccounts.datasource.UserDataSource
import com.cimminonicola.finanaceplanneraccounts.dtos.CreateUserDTO
import com.cimminonicola.finanaceplanneraccounts.errors.InputInvalidApiException
import com.cimminonicola.finanaceplanneraccounts.errors.UnauthorizedApiException
import com.cimminonicola.finanaceplanneraccounts.model.User
import com.cimminonicola.finanaceplanneraccounts.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api")
class UsersController(private val usersRepository: UserDataSource) {
    @Autowired
    lateinit var applicationStatus: ApplicationStatus

    @Autowired
    lateinit var userService: UserService

    @PostMapping("users")
    fun register(@RequestBody createUserRequest: CreateUserDTO): User {

        // TODO: this is supposed to be enforced by the model unique constraint but doesn't work!
        if (usersRepository.findByEmail(createUserRequest.email) != null) {
            throw InputInvalidApiException("duplicate email")
        }

        val user = User()
        user.name = createUserRequest.name
        user.email = createUserRequest.email
        user.password = createUserRequest.password

        return usersRepository.save(user)
    }

    @GetMapping("users/{user_id}")
    fun getUser(
        @PathVariable("user_id") userId: String?
    ): User {
        if (applicationStatus.authorizedUserId != userId) {
            throw UnauthorizedApiException()
        }

        return userService.getUser(userId)
    }
}
package com.cimminonicola.finanaceplanneraccounts.filters


import com.cimminonicola.finanaceplanneraccounts.ApplicationStatus
import com.cimminonicola.finanaceplanneraccounts.errors.UnauthorizedApiException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class AuthenticationFilter : OncePerRequestFilter() {

    @Autowired
    lateinit var applicationStatus: ApplicationStatus

    @Override
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // TODO: extract user_id from path and validate against JWT claim

        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION) ?: ""

        val jwt = authorizationHeader
            .substringAfter("Bearer ")
            .substringAfter("bearer ")

        try {
            var jwtBody = this.validateJwt(jwt)

            var results = "\\/api\\/users\\/([^\\/]*)".toRegex()
                .find(request.servletPath)?.destructured?.toList()
                ?: throw UnauthorizedApiException()

            if (results.isEmpty()) {
                throw UnauthorizedApiException()
            }

            val userId = results.first()

            // TODO: if we throw here we get to FilterChainExceptionHandler but with a servletException, needs fixing.
            if (jwtBody.subject != userId) {
                throw UnauthorizedApiException()
            }

            this.applicationStatus.authorizedUserId = jwtBody.subject
        } catch (e: Exception) {
            throw UnauthorizedApiException()
        }

        filterChain.doFilter(request, response);
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.servletPath.startsWith("/api/users/")
    }

    private fun validateJwt(jwt: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(this.applicationStatus.getJWTKey())
            .build()
            .parseClaimsJws(jwt).body
    }
}
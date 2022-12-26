package com.gmcn.tokens

import com.gmcn.tokens.dao.UserCredentialsDAO
import com.gmcn.tokens.dtos.NewUserCredentialsDTO
import com.gmcn.tokens.dtos.ValidateTokenDTO
import com.gmcn.tokens.dtos.ValidateTokenResponseDTO
import com.gmcn.tokens.model.UserCredentials
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.ClassMapper
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class TokensServiceInternalListener {
    @Autowired
    lateinit var userCredentialsDao: UserCredentialsDAO

    @Autowired
    lateinit var configProperties: ConfigProperties

    @Bean
    fun queue(): Queue? {
        return Queue(configProperties.serviceQueueName, false)
    }

    @Bean
    fun exchange(): TopicExchange? {
        return TopicExchange(configProperties.topicExchangeName)
    }

    @Bean
    fun binding(queue: Queue?, exchange: TopicExchange?): Binding? {
        return BindingBuilder.bind(queue).to(exchange).with(configProperties.userEventsRoutingKey)
    }

    @Bean
    fun classMapper(): ClassMapper {
        val classMapper = DefaultJackson2JavaTypeMapper()
        classMapper.setTrustedPackages("com.gmnc.isc")
        classMapper.idClassMapping = mapOf(
            "tokens.validate_token" to ValidateTokenDTO::class.java,
            "tokens.validate_token_response" to ValidateTokenResponseDTO::class.java,
            "tokens.new_user_credentials" to NewUserCredentialsDTO::class.java,
        )
        return classMapper

    }

    @Bean
    fun messageConverter(classMapper: ClassMapper): MessageConverter {
        val converter = Jackson2JsonMessageConverter()
        converter.setClassMapper(classMapper)

        return converter
    }

    @Bean
    fun container(
        connectionFactory: ConnectionFactory?,
        messageListenerAdapter: MessageListenerAdapter,
        messageConverter: MessageConverter
    ): SimpleMessageListenerContainer? {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory!!
        container.setQueueNames(configProperties.serviceQueueName)
        container.setMessageListener(messageListenerAdapter)
        messageListenerAdapter.setMessageConverter(messageConverter)

        return container
    }

    @Bean
    fun listenerAdapter(receiver: TokensServiceInternalListener?): MessageListenerAdapter? {
        return MessageListenerAdapter(receiver)
    }

    @RabbitHandler
    fun handleMessage(newUserCredentialsDto: NewUserCredentialsDTO) {
        println("Received <$newUserCredentialsDto>")
// TODO: this goes to the service layer, this is equivalent to a controller
        var userCredentials = UserCredentials()
        userCredentials.userId = newUserCredentialsDto.userId
        userCredentials.email = newUserCredentialsDto.email
        userCredentials.password = newUserCredentialsDto.password

        this.userCredentialsDao.save(userCredentials)
    }

    private fun validateJwt(jwt: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(configProperties.getTokenKey())
            .build()
            .parseClaimsJws(jwt).body
    }

    @RabbitHandler
    fun handleMessage(validateTokenDTO: ValidateTokenDTO): ValidateTokenResponseDTO {
        println("Received <$validateTokenDTO>")
        // TODO: this goes to the service layer, this is equivalent to a controller
        try {
            val jwtBody = validateJwt(validateTokenDTO.token)

            return ValidateTokenResponseDTO(
                jwtBody.subject, true
            )
        } catch (e: Exception) {
            return ValidateTokenResponseDTO(
                "", false
            )
        }
    }
}
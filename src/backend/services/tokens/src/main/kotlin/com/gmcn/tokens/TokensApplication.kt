package com.gmcn.tokens

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class, SecurityAutoConfiguration::class])
class TokensApplication {
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
    fun messageConverter(): MessageConverter? {
        return Jackson2JsonMessageConverter()
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
        messageListenerAdapter.setMessageConverter(Jackson2JsonMessageConverter())

        return container
    }

    @Bean
    fun listenerAdapter(receiver: NewUserCredentialsReceiver?): MessageListenerAdapter? {
        return MessageListenerAdapter(receiver, "receiveMessage")
    }

}

fun main(args: Array<String>) {
    runApplication<TokensApplication>(*args)
}


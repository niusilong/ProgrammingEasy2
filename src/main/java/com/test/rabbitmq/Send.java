package com.test.rabbitmq;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Send {
	// 队列名称
	private final static String QUEUE_NAME = "tv189";
//	private final static String HOST = "10.102.20.114";
//	private final static int PORT = 5672;
//	private final static String USERNAME = "test";
//	private final static String PASSWORD = "test";
	private final static String HOST = "192.168.23.164";
	private final static int PORT = 5672;
	private final static String USERNAME = "guest";
	private final static String PASSWORD = "guest";
	
	public static void main(String[] argv) throws java.io.IOException, TimeoutException {

		// 1.创建一个ConnectionFactory连接工厂connectionFactory
		ConnectionFactory connectionFactory = new ConnectionFactory();
		// 2.通过connectionFactory设置RabbitMQ所在IP等信息
		connectionFactory.setRequestedHeartbeat(58);
		connectionFactory.setConnectionTimeout(6000);
		connectionFactory.setHost(HOST);
		connectionFactory.setPort(PORT); // 指定端口
		connectionFactory.setUsername(USERNAME);// 用户名
		connectionFactory.setPassword(PASSWORD);// 密码
		// 3.通过connectionFactory创建一个连接connection
		Connection connection = connectionFactory.newConnection();
		// 4.通过connection创建一个频道channel
		Channel channel = connection.createChannel();
		// 5.通过channel指定一个队列
		channel.queueDeclare(QUEUE_NAME, true, false, false, null);
		// 发送的消息
		String message = "Hello world!bbccDD";
		// 6.通过channel向队列中添加消息
		channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
		System.out.println("向" + QUEUE_NAME + "中添加了一条消息:" + message);
		// 7.关闭频道
		channel.close();
		// 8.关闭连接
		connection.close();
	}
}

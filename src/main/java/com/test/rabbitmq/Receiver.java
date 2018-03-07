package com.test.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;

/**
 * RabbitMQ接收消息 Receiver
 * 
 */
public class Receiver {
	// 队列名称
	private final static String QUEUE_NAME = "hello";
//	private final static String HOST = "10.102.20.114";
//	private final static int PORT = 5672;
//	private final static String USERNAME = "test";
//	private final static String PASSWORD = "test";
	private final static String HOST = "192.168.23.164";
	private final static int PORT = 5672;
	private final static String USERNAME = "guest";
	private final static String PASSWORD = "guest";
	
	public static void main(String[] argv) throws Exception {
		// 1.创建一个ConnectionFactory连接工厂connectionFactory
		ConnectionFactory connectionFactory = new ConnectionFactory();
		// 2.通过connectionFactory设置RabbitMQ所在IP等信息
		connectionFactory.setHost(HOST);
		connectionFactory.setPort(PORT); // 指定端口
		connectionFactory.setUsername(USERNAME);// 用户名
		connectionFactory.setPassword(PASSWORD);// 密码
		// 3.通过connectionFactory创建一个连接connection
		Connection connection = connectionFactory.newConnection();
		// 4.通过connection创建一个频道channel
		Channel channel = connection.createChannel();
		// 5.通过channel指定队列
		channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		// 与发送消息不同的地方
		// 6.创建一个消费者队列consumer,并指定channel
		QueueingConsumer consumer = new QueueingConsumer(channel);
		// 7.为channel指定消费者
		channel.basicConsume(QUEUE_NAME, true, consumer);
		while (true) {
			// 从consumer中获取队列中的消息,nextDelivery是一个阻塞方法,如果队列中无内容,则等待
			Delivery delivery = consumer.nextDelivery();
			String message = new String(delivery.getBody());
			System.out.println("接收到了" + QUEUE_NAME + "中的消息:" + message);
		}

	}
}
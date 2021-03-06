/* Original work Copyright (c) 2015 Al Dispennette 
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */
package org.apache.kafka.clients.jms;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

/**
 * @author Al Dispennette
 * @since 0.8.2.2
 *
 */
public class KafkaMessageConsumer implements MessageConsumer {
	private Consumer<String, Message> consumer;
	private KafkaDestination destination;
	private MessageListener listener = new MessageListener() {
		@Override
		public void onMessage(Message message) {
			// Denault noop listener
		}
	};
	
	/**
	 * consumer config should define a group Id
	 * @throws JMSException 
	 */
	public KafkaMessageConsumer(Properties config, Destination destination) throws JMSException {
		consumer = new KafkaConsumer<String, Message>(config);
		this.destination = (KafkaDestination) destination;
		consumer.subscribe(Arrays.asList(this.destination.getName()));
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageConsumer#getMessageSelector()
	 */
	@Override
	public String getMessageSelector() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageConsumer#getMessageListener()
	 */
	@Override
	public MessageListener getMessageListener() throws JMSException {
		return listener;
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageConsumer#setMessageListener(javax.jms.MessageListener)
	 */
	@Override
	public void setMessageListener(MessageListener listener)
			throws JMSException {
		this.listener = listener;
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageConsumer#receive()
	 */
	@Override
	public Message receive() throws JMSException {
		subscribe();
		ConsumerRecords<String, Message> records = null;
		while(null == records) {
			records = consumer.poll(0);
		}
		return process(records);
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageConsumer#receive(long)
	 */
	@Override
	public Message receive(long timeout) throws JMSException {
		// Map<"topic name", ConsumerRecords<"trace_id", KafkaMessage>>
		subscribe();
		ConsumerRecords<String, Message> records = consumer.poll(timeout);
		return process(records);
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageConsumer#receiveNoWait()
	 */
	@Override
	public Message receiveNoWait() throws JMSException {
		return receive(0);
	}
	
	//private List<Message> process(ConsumerRecords<String, Message> consumerRecords) throws JMSException{
	private Message process(ConsumerRecords<String, Message> consumerRecords) throws JMSException{
		List<Message> results = new ArrayList<Message>();
		Iterable<ConsumerRecord<String, Message>> records = consumerRecords.records(destination.getName());
		// user specific logic to process record
		//processedOffsets.put(record.partition(), record.offset());
		//records.
		for(ConsumerRecord<String,Message> record:records){
			Message message = record.value();
			listener.onMessage(message);
			TopicPartition tp = new TopicPartition(record.topic(), record.partition());
			OffsetAndMetadata oam = new OffsetAndMetadata(record.offset());
			Map<TopicPartition,OffsetAndMetadata> map = new HashMap<TopicPartition, OffsetAndMetadata>();
			map.put(tp, oam);
			consumer.commitSync(map);
			unsubscribe();
			return message;
			//results.add(message);
		}
		return null;
	}
	
	public void commit(){
		// NOOP
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageConsumer#close()
	 */
	@Override
	public void close() throws JMSException {
		unsubscribe();
		consumer.close();
	}
	
	void subscribe() throws JMSException{
		consumer.subscribe(Arrays.asList(this.destination.getName()));
	}
	
	void unsubscribe() throws JMSException{
		consumer.unsubscribe();
	}

}

package titan.ccp.kiekerbridge.test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import kieker.analysis.plugin.reader.newio.deserializer.BinaryDeserializer;
import kieker.analysis.plugin.reader.newio.deserializer.IMonitoringRecordDeserializer;
import kieker.common.record.IMonitoringRecord;

public class Reader {

	private final String topicName = "test-soeren--3";
	
	//private final String deserializerClassName;
	
	//private final String readerClassName;
	
	//private final IRawDataReader reader;
	
	private final IMonitoringRecordDeserializer deserializer = new BinaryDeserializer(null, null);
	
	private final KafkaConsumer<String, byte[]> consumer;
	
	public Reader() {
		final Properties properties = new Properties();
		
		properties.put("bootstrap.servers", "127.0.0.1:9092");
		properties.put("group.id", "test-2");
		//properties.put("enable.auto.commit", this.enableAutoCommit);
		//properties.put("auto.commit.interval.ms", this.autoCommitIntervalMs);
		//properties.put("session.timeout.ms", this.sessionTimeoutMs);
		
		properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		
		this.consumer = new KafkaConsumer<String, byte[]>(properties);
		
	}
	
	public void run() {
		this.consumer.subscribe(Arrays.asList(this.topicName));

		while (true) { //TODO
			final ConsumerRecords<String, byte[]> records = this.consumer.poll(1000); //TODO
			
			this.processRecords(records);
		}
		
		//this.consumer.close();
			
		//return true;
	}
	
	// PMD thinks this is an unused private method (see: https://github.com/pmd/pmd/issues/521)
	private void processRecords(final ConsumerRecords<String, byte[]> records) { // NOPMD (false positive, see above)
		for (final ConsumerRecord<String, byte[]> record : records) {
			final byte[] data = record.value();
			decodeAndDeliverRecords(data);
		}
	}
	
	private void decodeAndDeliverRecords(final byte[] rawData) {
		this.decodeAndDeliverRecords(ByteBuffer.wrap(rawData), rawData.length);
	}

	private void decodeAndDeliverRecords(final ByteBuffer rawData, final int dataSize) {
		final List<IMonitoringRecord> monitoringRecords = this.deserializer.deserializeRecords(rawData, dataSize);

		for (final IMonitoringRecord monitoringRecord : monitoringRecords) {
			this.deliver(monitoringRecord);
		}
	}
	
	private void deliver(IMonitoringRecord monitoringRecord) {
		//TODO
		System.out.println(monitoringRecord);
	}

	public static void main(String[] args) {
		
		new Reader().run();
	}
	
}

package de.deepshore.kafka;

import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Xsd2AvroUtil {
    private Xsd2AvroUtil() {
        // hide the public constructor because this is only a util class
    }
    public static ConnectRecord<SinkRecord> getDummySinkRecord(String message){
        return new SinkRecord(
                "n.a.",
                1,
                null,
                null,
                Schema.BYTES_SCHEMA,
                message.getBytes(StandardCharsets.UTF_8),
                new Date().getTime()
        );
    }
}

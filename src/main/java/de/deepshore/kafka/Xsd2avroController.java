package de.deepshore.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jcustenborder.kafka.connect.transform.xml.FromXml;
import com.google.common.io.Files;
import io.confluent.connect.avro.AvroData;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.exceptions.HttpServerException;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.sink.SinkRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static de.deepshore.kafka.Xsd2AvroUtil.getDummySinkRecord;

@Controller("/xsd2avro")
public class Xsd2avroController {
    public static final String SCHEMA_PATH_CONFIG = "schema.path";
    static final ObjectMapper objectMapper = new ObjectMapper();


    @Get(uri = "/", produces = "plain/text")
    public String status() {
        return "Hello! I can convert xsd to avro.";
    }

    @Post(uri = "/connect/xsd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String convert(@Body XsdPack xsdpack, @QueryValue(defaultValue = "false") Boolean pretty) {
        File xsdFile = null;
        try {
            xsdFile = File.createTempFile("xsd", String.valueOf(LocalDateTime.now()));
            Files.write(xsdpack.getXsd().getBytes(StandardCharsets.UTF_8), xsdFile);
        } catch (IOException e) {
            throw new HttpServerException("Error while creating temporary File for XSD");
        }

        try(FromXml.Value transform = new FromXml.Value()) {
            transform.configure(
                    Map.of(SCHEMA_PATH_CONFIG, xsdFile.getAbsoluteFile().toURL().toString(), "xjc.options.strict.check.enabled", "false")
            );

            ConnectRecord<SinkRecord> transformedRecord = transform.apply(getDummySinkRecord(xsdpack.getXml()));
            final AvroData aa = new AvroData(20000);

            final org.apache.avro.Schema valueSchema = aa.fromConnectSchema(transformedRecord.valueSchema());
            return valueSchema.toString(pretty);

        } catch (Exception e) {
            throw new HttpServerException("Error while converting XSD to AVRO");
        }

    }
}
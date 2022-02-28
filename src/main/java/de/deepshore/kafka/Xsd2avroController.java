package de.deepshore.kafka;

import com.github.jcustenborder.kafka.connect.transform.xml.FromXml;
import com.google.common.io.Files;
import de.deepshore.kafka.models.XsdPack;
import io.confluent.connect.avro.AvroData;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static de.deepshore.kafka.Xsd2AvroUtil.getDummySinkRecord;

@Controller("/xsd2avro")
public class Xsd2avroController {
    public static final String SCHEMA_PATH_CONFIG = "schema.path";
    private static final Logger LOG = LoggerFactory.getLogger(Xsd2avroController.class);


    @Get(uri = "/", produces = "plain/text")
    public String status() {
        return "Hello! I can convert xsd to avro.";
    }

    @Post(uri = "/connect/xsd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse convert(@Body XsdPack xsdpack, @QueryValue(defaultValue = "false") Boolean pretty) {
        File xsdFile = null;
        try {
            xsdFile = File.createTempFile("xsd", String.valueOf(LocalDateTime.now()));
            Files.write(xsdpack.getXsd().getBytes(StandardCharsets.UTF_8), xsdFile);
        } catch (IOException e) {
            LOG.error("Error while creating temporary File for XSD", e);
            return HttpResponse.serverError("Internal Server Error while processing XSD");
        }

        try(FromXml.Value transform = new FromXml.Value()) {
            transform.configure(
                    Map.of(SCHEMA_PATH_CONFIG, xsdFile.getAbsoluteFile().toURL().toString(), "xjc.options.strict.check.enabled", "false")
            );

            ConnectRecord<SinkRecord> transformedRecord = transform.apply(getDummySinkRecord(xsdpack.getXml()));
            final AvroData aa = new AvroData(20000);

            final org.apache.avro.Schema valueSchema = aa.fromConnectSchema(transformedRecord.valueSchema());
            return HttpResponse.ok(valueSchema.toString(pretty));

        } catch (Exception e) {
            LOG.info("Error while converting XSD to AVRO", e);
            return HttpResponse.ok(String.format("Error while converting XSD to AVRO: %s", e.getMessage()));
        }

    }
}
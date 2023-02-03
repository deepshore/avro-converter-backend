package de.deepshore.kafka;

import com.github.jcustenborder.kafka.connect.transform.xml.FromXml;
import com.google.common.io.Files;
import de.deepshore.kafka.models.AvroPack;
import de.deepshore.kafka.models.XsdPack;
import io.confluent.connect.avro.AvroData;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.sink.SinkRecord;
import org.json.JSONObject;
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
    public static final String PACKAGE_CONFIG = "package";
    public static final String XPATH_FOR_RECORD_KEY = "xpath.for.record.key";
    public static final String XJC_OPTIONS_STRICT_CHECK_CONFIG = "xjc.options.strict.check.enabled";
    public static final String XJC_OPTIONS_STRICT_CHECK_CONFIG_VALUE = "false";
    private static final Logger LOG = LoggerFactory.getLogger(Xsd2avroController.class);

    @Get(produces = "plain/text")
    public String status() {
        return "Hello! I can convert xsd to avro.";
    }

    @Post(uri = "/connect/xsd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse convert(@Body XsdPack xsdpack) {
        File xsdFile;

        if(!xsdpack.getXsd().startsWith("<?xml") && !xsdpack.getXsd().startsWith("<xsd")){
            return HttpResponse.ok("Please provide a valid xml schema.");
        }
        if(!xsdpack.getXml().startsWith("<?xml")){
            return HttpResponse.ok("Please provide a valid xml file.");
        }

        try {
            xsdFile = File.createTempFile("xsd", String.valueOf(LocalDateTime.now()));
            Files.write(xsdpack.getXsd().getBytes(StandardCharsets.UTF_8), xsdFile);
        } catch (IOException e) {
            LOG.error("Error while creating temporary File for XSD", e);
            return HttpResponse.serverError("Internal Server Error while processing XSD");
        }

        try(FromXml.Value transform = new FromXml.Value()) {


            Map<String, String> transformConfig;

            if("".equals(xsdpack.getXpathRecordKey())){
                transformConfig = Map.of(
                        SCHEMA_PATH_CONFIG, xsdFile.getAbsoluteFile().toURL().toString(),
                        XJC_OPTIONS_STRICT_CHECK_CONFIG, XJC_OPTIONS_STRICT_CHECK_CONFIG_VALUE,
                        PACKAGE_CONFIG, this.getClass().getPackageName(),
                        XPATH_FOR_RECORD_KEY, xsdpack.getXpathRecordKey()
                );
            } else {
                transformConfig = Map.of(
                        SCHEMA_PATH_CONFIG, xsdFile.getAbsoluteFile().toURL().toString(),
                        XJC_OPTIONS_STRICT_CHECK_CONFIG, XJC_OPTIONS_STRICT_CHECK_CONFIG_VALUE,
                        PACKAGE_CONFIG, this.getClass().getPackageName()
                );
            }

            transform.configure(transformConfig);

            ConnectRecord<SinkRecord> transformedRecord = transform.apply(getDummySinkRecord(xsdpack.getXml()));
            final AvroData ad = new AvroData(20000);

            final org.apache.avro.Schema valueSchema = ad.fromConnectSchema(transformedRecord.valueSchema());
            final org.apache.avro.Schema keySchema = ad.fromConnectSchema(transformedRecord.keySchema());

            final Object oo = ad.fromConnectData(transformedRecord.valueSchema(), transformedRecord.value());

            final String valueSchemaString = valueSchema.toString().replace(this.getClass().getPackageName(), xsdpack.getNamespace());
            final String keySchemaString = keySchema.toString().replace(this.getClass().getPackageName(), xsdpack.getNamespace());

            final String optionalKeyString = (null != transformedRecord.key()) ? transformedRecord.key().toString() : null;

            final JSONObject valueJson = new JSONObject(oo.toString());


            final AvroPack ap = new AvroPack(keySchemaString,
                    optionalKeyString,
                    valueSchemaString,
                    valueJson.toString()
                    );

            return HttpResponse.ok(ap);

        } catch (NullPointerException npe) {
            LOG.info("Error while converting XSD to AVRO", npe);
            return HttpResponse.ok(String.format("Error while converting XSD to AVRO: %s", npe.getLocalizedMessage()));
        } catch (Exception e) {
            LOG.info("Error while converting XSD to AVRO", e.getStackTrace());
            return HttpResponse.ok(String.format("Error while converting XSD to AVRO: %s", e.getLocalizedMessage()));
        }

    }
}
package de.deepshore.kafka;

import com.google.common.io.Files;
import de.deepshore.kafka.models.XsdPack;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

public class Xsd2AvroUtil {

    public static final String SCHEMA_PATH_CONFIG = "schema.path";
    public static final String PACKAGE_CONFIG = "package";
    public static final String XPATH_FOR_RECORD_KEY = "xpath.for.record.key";
    public static final String XJC_OPTIONS_STRICT_CHECK_CONFIG = "xjc.options.strict.check.enabled";
    public static final String XJC_OPTIONS_STRICT_CHECK_CONFIG_VALUE = "false";
    public static final String ERROR_WHILE_CONVERTING_XSD_TO_AVRO = "Error while converting XSD to AVRO";
    public static final String ERROR_WHILE_CONVERTING_XSD_TO_AVRO_S = "Error while converting XSD to AVRO: %s";

    private Xsd2AvroUtil() {
        // hide the public constructor because this is only a util class
    }

    public static Map<String, String> createTransformConfig(XsdPack xsdpack, String packageName) throws IOException {
        final Map<String, String> transformConfig;

        final File xsdFile = File.createTempFile("xsd", String.valueOf(LocalDateTime.now()));
        Files.write(xsdpack.getXsd().getBytes(StandardCharsets.UTF_8), xsdFile);

        if("".equals(xsdpack.getXpathRecordKey())){
            transformConfig = Map.of(
                    SCHEMA_PATH_CONFIG, xsdFile.getAbsoluteFile().toURL().toString(),
                    XJC_OPTIONS_STRICT_CHECK_CONFIG, XJC_OPTIONS_STRICT_CHECK_CONFIG_VALUE,
                    PACKAGE_CONFIG, packageName
            );

        } else {
            transformConfig = Map.of(
                    SCHEMA_PATH_CONFIG, xsdFile.getAbsoluteFile().toURL().toString(),
                    XJC_OPTIONS_STRICT_CHECK_CONFIG, XJC_OPTIONS_STRICT_CHECK_CONFIG_VALUE,
                    PACKAGE_CONFIG, packageName,
                    XPATH_FOR_RECORD_KEY, xsdpack.getXpathRecordKey()
            );
        }

        return transformConfig;
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

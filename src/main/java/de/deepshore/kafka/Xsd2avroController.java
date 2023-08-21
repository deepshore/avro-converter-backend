package de.deepshore.kafka;

import com.github.jcustenborder.kafka.connect.transform.xml.FromXml;
import de.deepshore.kafka.models.AvroPack;
import de.deepshore.kafka.models.XsdPack;
import io.confluent.connect.avro.AvroData;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.validation.Validated;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.sink.SinkRecord;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static de.deepshore.kafka.Xsd2AvroUtil.*;

@Validated
@Controller("/xsd2avro")
public class Xsd2avroController {

    private static final Logger LOG = LoggerFactory.getLogger(Xsd2avroController.class);

    @Get(produces = "plain/text")
    public String status() {
        return "Hello! I can convert xsd to avro.";
    }

    @Post(uri = "/connect/xsd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse convert(@Body @Valid XsdPack xsdpack) {

        try(FromXml.Value transform = new FromXml.Value()) {

            final Map<String, String> transformConfig = createTransformConfig(xsdpack, this.getClass().getPackageName());

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
            LOG.info(ERROR_WHILE_CONVERTING_XSD_TO_AVRO, npe);
            return HttpResponse.serverError(String.format(ERROR_WHILE_CONVERTING_XSD_TO_AVRO_S, npe.getLocalizedMessage()));
        } catch (Exception e) {
            LOG.info(ERROR_WHILE_CONVERTING_XSD_TO_AVRO, e);
            return HttpResponse.serverError(String.format(ERROR_WHILE_CONVERTING_XSD_TO_AVRO_S, e.getLocalizedMessage()));
        }

    }

    @Post(uri = "/connect/java")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public HttpResponse getJava(@Body @Valid XsdPack xsdpack) {

        try(FromXml.Value transform = new FromXml.Value()) {

            final Map<String, String> transformConfig = createTransformConfig(xsdpack, this.getClass().getPackageName());

            transform.configure(transformConfig);

            File tempFile = File.createTempFile("compressed-java", String.valueOf(LocalDateTime.now()));

            try(final FileOutputStream fos = new FileOutputStream(tempFile);
                ZipOutputStream zipOut = new ZipOutputStream(fos)) {
                List<File> generatedSourceFiles = transform.getGeneratedSourceFiles();

                generatedSourceFiles.forEach(srcFile -> {
                    try(FileInputStream fis = new FileInputStream(srcFile)) {
                        final ZipEntry zipEntry = new ZipEntry(srcFile.getName());
                        zipOut.putNextEntry(zipEntry);
                        byte[] bytes = new byte[1024];
                        int length;
                        while((length = fis.read(bytes)) >= 0) {
                            zipOut.write(bytes, 0, length);
                        }
                    } catch (IOException e) {
                        LOG.warn("Error zipping", e);
                        return;
                    }
                });


                return HttpResponse.ok(new StreamedFile(new FileInputStream(tempFile), new MediaType(MediaType.APPLICATION_OCTET_STREAM)))
                        .header("Content-type", MediaType.APPLICATION_OCTET_STREAM)
                        .header("Content-disposition", "attachment; filename=\"java.zip\"");
            }
        } catch (IOException ioe) {
            LOG.info("Error while converting java files to zip", ioe);
            return HttpResponse.serverError(String.format("Error while converting java files to zip: %s", ioe.getLocalizedMessage()));
        } catch (NullPointerException npe) {
            LOG.info(ERROR_WHILE_CONVERTING_XSD_TO_AVRO, npe);
            return HttpResponse.serverError(String.format(ERROR_WHILE_CONVERTING_XSD_TO_AVRO_S, npe.getLocalizedMessage()));
        } catch (Exception e) {
            LOG.info(ERROR_WHILE_CONVERTING_XSD_TO_AVRO, e);
            return HttpResponse.serverError(String.format(ERROR_WHILE_CONVERTING_XSD_TO_AVRO_S, e.getLocalizedMessage()));
        }

    }
}
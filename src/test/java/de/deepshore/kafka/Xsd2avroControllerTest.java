package de.deepshore.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import de.deepshore.kafka.models.AvroPack;
import de.deepshore.kafka.models.XsdPack;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class Xsd2avroControllerTest {
    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ObjectMapper objectMapper;

    @Test
    void testHello() {
        final String result = client.toBlocking().retrieve(HttpRequest.GET("/xsd2avro/"), String.class);

        assertEquals(
                "Hello! I can convert xsd to avro.",
                result
        );
    }

    @Test
    void testConvert(ObjectMapper objectMapper) throws IOException {
        final String schema = Files.toString(new File("src/test/resources/testConvert/schema.xml"), StandardCharsets.UTF_8);
        final String value = Files.toString(new File("src/test/resources/testConvert/value.xml"), StandardCharsets.UTF_8);

        XsdPack bodyObj = new XsdPack(schema, value);

        final AvroPack result = client.toBlocking().retrieve(HttpRequest.POST("/xsd2avro/connect/xsd", objectMapper.writeValueAsString(bodyObj)), AvroPack.class);

        assertEquals(
                null,
                result.getKey()
        );
        assertEquals(
                "\"string\"",
                result.getKeySchema()
        );
        assertEquals(
                "{\"book\":[{\"pub_date\":\"2000-10-01\",\"author\":\"Writer\",\"price\":44.95,\"review\":\"An amazing story of nothing.\",\"genre\":\"Fiction\",\"id\":\"bk001\",\"title\":\"The First Book\"},{\"pub_date\":\"2000-10-01\",\"author\":\"Poet\",\"price\":24.95,\"review\":\"Least poetic poems.\",\"genre\":\"Poem\",\"id\":\"bk002\",\"title\":\"The Poet's First Poem\"}]}",
                result.getValue()
        );
        assertEquals(
                "[\"null\",{\"type\":\"record\",\"name\":\"BooksForm\",\"namespace\":\"de.deepshore.kafka\",\"fields\":[{\"name\":\"book\",\"type\":[\"null\",{\"type\":\"array\",\"items\":[\"null\",{\"type\":\"record\",\"name\":\"BookForm\",\"fields\":[{\"name\":\"author\",\"type\":\"string\"},{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"genre\",\"type\":\"string\"},{\"name\":\"price\",\"type\":[\"null\",\"float\"],\"default\":null},{\"name\":\"pub_date\",\"type\":\"string\"},{\"name\":\"review\",\"type\":\"string\"},{\"name\":\"id\",\"type\":[\"null\",\"string\"],\"default\":null}],\"connect.name\":\"de.deepshore.kafka.BookForm\"}]}],\"default\":null}],\"connect.name\":\"de.deepshore.kafka.BooksForm\"}]",
                result.getValueSchema()
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
            "testConvertFailure.json| [{\"message\":\"xsdpack.xml: XML must start with <?xml tag\"},{\"message\":\"xsdpack.xsd: XSD must start with <xsd or <?xml tag\"}]",
            "testConvertInvalidInput.json| [{\"message\":\"xsdpack.xml: XML must start with <?xml tag\"},{\"message\":\"xsdpack.xsd: XSD must start with <xsd or <?xml tag\"}]",
            "testConvertInvalidInputPartial.json| [{\"message\":\"xsdpack.xml: XML must start with <?xml tag\"},{\"message\":\"xsdpack.xsd: XSD must start with <xsd or <?xml tag\"}]",
    }, delimiterString = "|")
    void testConvertErrorInvalidInputs(String input, String expected) throws IOException {
        final String body = Files.toString(new File(String.format("src/test/resources/%s", input)), StandardCharsets.UTF_8);

        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
         client.toBlocking().retrieve(HttpRequest.POST("/xsd2avro/connect/xsd", body), String.class);
        });
        HttpResponse<?> response = e.getResponse();


        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatus()
        );
        JsonNode badRequestResponse = objectMapper.readValue(response.body().toString(), JsonNode.class);

        String errorsFromResponse = badRequestResponse.get("_embedded").get("errors").toString();

        assertEquals(
                expected,
                errorsFromResponse
        );
    }

    @Test
    void testConvertNamespace(ObjectMapper objectMapper) throws IOException {
        final String schema = Files.toString(new File("src/test/resources/testConvert/schema.xml"), StandardCharsets.UTF_8);
        final String value = Files.toString(new File("src/test/resources/testConvert/value.xml"), StandardCharsets.UTF_8);

        XsdPack bodyObj = new XsdPack(schema, value);
        bodyObj.setNamespace("de.mydomain.package");

        final String result = client.toBlocking().retrieve(HttpRequest.POST("/xsd2avro/connect/xsd", objectMapper.writeValueAsString(bodyObj)), String.class);

        assertEquals(
                "{\"keySchema\":\"\\\"string\\\"\",\"valueSchema\":\"[\\\"null\\\",{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"BooksForm\\\",\\\"namespace\\\":\\\"de.mydomain.package\\\",\\\"fields\\\":[{\\\"name\\\":\\\"book\\\",\\\"type\\\":[\\\"null\\\",{\\\"type\\\":\\\"array\\\",\\\"items\\\":[\\\"null\\\",{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"BookForm\\\",\\\"fields\\\":[{\\\"name\\\":\\\"author\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"title\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"genre\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"price\\\",\\\"type\\\":[\\\"null\\\",\\\"float\\\"],\\\"default\\\":null},{\\\"name\\\":\\\"pub_date\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"review\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"id\\\",\\\"type\\\":[\\\"null\\\",\\\"string\\\"],\\\"default\\\":null}],\\\"connect.name\\\":\\\"de.mydomain.package.BookForm\\\"}]}],\\\"default\\\":null}],\\\"connect.name\\\":\\\"de.mydomain.package.BooksForm\\\"}]\",\"value\":\"{\\\"book\\\":[{\\\"pub_date\\\":\\\"2000-10-01\\\",\\\"author\\\":\\\"Writer\\\",\\\"price\\\":44.95,\\\"review\\\":\\\"An amazing story of nothing.\\\",\\\"genre\\\":\\\"Fiction\\\",\\\"id\\\":\\\"bk001\\\",\\\"title\\\":\\\"The First Book\\\"},{\\\"pub_date\\\":\\\"2000-10-01\\\",\\\"author\\\":\\\"Poet\\\",\\\"price\\\":24.95,\\\"review\\\":\\\"Least poetic poems.\\\",\\\"genre\\\":\\\"Poem\\\",\\\"id\\\":\\\"bk002\\\",\\\"title\\\":\\\"The Poet's First Poem\\\"}]}\"}",
                result
        );
    }

    @Test
    void testXpathKey(ObjectMapper objectMapper) throws IOException {
        final String schema = Files.toString(new File("src/test/resources/testConvert/schema.xml"), StandardCharsets.UTF_8);
        final String value = Files.toString(new File("src/test/resources/testConvert/value.xml"), StandardCharsets.UTF_8);

        XsdPack bodyObj = new XsdPack(schema, value);
        bodyObj.setXpathRecordKey("//book[1]/author");

        final AvroPack result = client.toBlocking().retrieve(HttpRequest.POST("/xsd2avro/connect/xsd", objectMapper.writeValueAsString(bodyObj)), AvroPack.class);

        assertEquals(
                "Writer",
                result.getKey()
        );
    }

    @Test
    void testZipWithJava(ObjectMapper objectMapper) throws IOException {
        final String schema = Files.toString(new File("src/test/resources/testConvert/schema.xml"), StandardCharsets.UTF_8);
        final String value = Files.toString(new File("src/test/resources/testConvert/value.xml"), StandardCharsets.UTF_8);

        XsdPack bodyObj = new XsdPack(schema, value);
        bodyObj.setXpathRecordKey("//book[1]/author");


        final byte[] result = client.toBlocking().retrieve(HttpRequest.POST("/xsd2avro/connect/java", objectMapper.writeValueAsString(bodyObj)), byte[].class);

        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(result));
        ZipEntry zipEntry = zis.getNextEntry();

        List<String> fileNames = new ArrayList<>();

        while (zipEntry != null) {
            fileNames.add(zipEntry.getName());
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
        
        assertEquals(4, fileNames.size());
        assertThat(fileNames).contains("BookForm.java", "BooksForm.java", "ObjectFactory.java", "package-info.java");
    }
}
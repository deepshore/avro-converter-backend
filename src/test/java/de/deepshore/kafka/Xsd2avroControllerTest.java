package de.deepshore.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import de.deepshore.kafka.models.AvroPack;
import de.deepshore.kafka.models.XsdPack;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class Xsd2avroControllerTest {
    @Inject
    @Client("/")
    HttpClient client;

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
    @CsvSource({
            "testConvertFailure.json, Error while converting XSD to AVRO: Illegal character in: bo-ok",
            "testConvertInvalidInput.json, Please provide a valid xml schema.",
            "testConvertInvalidInputPartial.json, Please provide a valid xml file.",
    })
    void testConvertErrorInvalidInputs(String input, String expected) throws IOException {
        final String body = Files.toString(new File(String.format("src/test/resources/%s", input)), StandardCharsets.UTF_8);

        final String result = client.toBlocking().retrieve(HttpRequest.POST("/xsd2avro/connect/xsd", body), String.class);

        assertEquals(
                expected,
                result
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
}
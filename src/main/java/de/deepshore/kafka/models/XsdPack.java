package de.deepshore.kafka.models;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Introspected
public class    XsdPack {
    @NotBlank
    private String namespace = "de.deepshore.kafka";
    private String xpathRecordKey = "";
    @Pattern(regexp = "^<(xsd|\\?xml).*$", message = "XSD must start with <xsd or <?xml tag", flags = Pattern.Flag.DOTALL)
    private String xsd;
    @Pattern(regexp = "^<\\?xml.*$", message = "XML must start with <?xml tag", flags = Pattern.Flag.DOTALL)
    private String xml;

    public String getXsd() {
        return xsd;
    }

    public void setXsd(String xsd) {
        this.xsd = xsd;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getXpathRecordKey() {
        return xpathRecordKey;
    }

    public void setXpathRecordKey(String xpathRecordKey) {
        this.xpathRecordKey = xpathRecordKey;
    }
}

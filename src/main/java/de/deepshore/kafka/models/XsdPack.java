package de.deepshore.kafka.models;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class XsdPack {
    private String xsd;
    private String xml;

    public XsdPack(String xsd, String xml) {
        this.xsd = xsd;
        this.xml = xml;
    }

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
}

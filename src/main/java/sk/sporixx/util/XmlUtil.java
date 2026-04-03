package sk.sporixx.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Pomocná trieda pre bezpečné XML operácie.
 * Centralizuje bezpečnostné nastavenia proti XXE útokom.
 */
public final class XmlUtil {

    private XmlUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Vytvorí bezpečne nakonfigurovaný DocumentBuilderFactory.
     * Ochrana proti XXE (XML External Entity) útokom.
     * Použité v ExportServiceImpl aj ImportServiceImpl.
     */
    public static DocumentBuilderFactory createSecureFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        return factory;
    }
}

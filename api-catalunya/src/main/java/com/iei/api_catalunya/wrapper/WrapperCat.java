package com.iei.api_catalunya.wrapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;

@Component
public class WrapperCat {

    private final ObjectMapper mapper = new ObjectMapper();
    private int estacionId = 1;
    private int localidadId = 1;

    public JsonNode convertirXMLaJSON(String filePath) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filePath));
        doc.getDocumentElement().normalize();

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList rows = (NodeList) xPath.evaluate("/response/row/row", doc, XPathConstants.NODESET);

        ArrayNode estacionesArray = mapper.createArrayNode();

        for (int i = 0; i < rows.getLength(); i++) {
            Node row = rows.item(i);

            ObjectNode estacion = mapper.createObjectNode();

            String denominaci = getText(row, "denominaci");
            String nombre = "Estació de ITV de " + denominaci;
            String cp = getText(row, "cp");
            String direccion = getText(row, "adre_a");
            String longStr = getText(row, "long");
            String latStr = getText(row, "lat");
            String horario = getText(row, "horari_de_servei");
            String descripcion = direccion + " / " + horario;
            String correo = getText(row, "correu_electr_nic");
            String telefono = getText(row, "tel_atenc_public");
            String contacto = correo + " / " + telefono;
            String URL = getText(row, "web/@url");
            String nombre_provincia = getText(row, "serveis_territorials");

            estacion.put("cod_estacion", estacionId++);
            estacion.put("nombre", nombre);

            if (isNullOrEmpty(cp) && isNullOrEmpty(direccion)) {
                estacion.put("tipo", "Otros");
            } else {
                estacion.put("tipo", "Estacion_fija");
            }

            estacion.put("direccion", direccion);
            estacion.put("codigo_postal", cp);

            if (!isNullOrEmpty(longStr)) {
                estacion.put("longitud", Double.parseDouble(longStr) / 1_000_000d);
            }
            if (!isNullOrEmpty(latStr)) {
                estacion.put("latitud", Double.parseDouble(latStr) / 1_000_000d);
            }

            estacion.put("descripcion", descripcion);
            estacion.put("horario", horario);
            estacion.put("contacto", contacto);
            estacion.put("URL", URL);
            estacion.put("localidad_codigo", localidadId++);
            estacion.put("localidad_nombre", denominaci);

            if (!isNullOrEmpty(cp) && cp.length() >= 2) {
                estacion.put("provincia_codigo", cp.substring(0, 2));
            }

            estacion.put("provincia_nombre", nombre_provincia);

            estacionesArray.add(estacion);
        }
        return estacionesArray;
    }

    private String getText(Node row, String xpathExpr) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();

            if (xpathExpr.contains("@")) {
                String attrValue = xPath.evaluate(xpathExpr, row);
                return attrValue != null ? attrValue.trim() : "";
            }

            String result = xPath.evaluate(xpathExpr, row);
            return result != null ? result.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}

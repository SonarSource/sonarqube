/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * XML Parsing tool using XPATH. It's recommended to use StaxParser when parsing big XML files.
 *
 * @since 1.10
 * @deprecated since 5.6 plugins should use their own dependencies
 */
@Deprecated
public class XpathParser {

  private static final String CAN_NOT_PARSE_XML = "can not parse xml : ";
  private Element root = null;
  private Document doc = null;
  private DocumentBuilder builder;
  private XPath xpath;
  private Map<String, XPathExpression> compiledExprs = new HashMap<>();

  public XpathParser() {
    DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
    try {
      bf.setFeature("http://apache.org/xml/features/validation/schema", false);
      bf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      bf.setFeature("http://xml.org/sax/features/validation", false);
      bf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      bf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      bf.setFeature("http://apache.org/xml/features/allow-java-encodings", true);
    } catch (ParserConfigurationException e) {
      Logger log = Loggers.get(this.getClass().getName());
      log.error("Error occured during features set up.", e);
    }
    try {
      bf.setNamespaceAware(false);
      bf.setValidating(false);
      builder = bf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new XmlParserException("can not create a XML parser", e);
    }
  }

  public void parse(@Nullable File file) {
    if (file == null || !file.exists()) {
      throw new XmlParserException("File not found : " + file);
    }

    BufferedReader buffer = null;
    try {
      buffer = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
      parse(buffer);

    } catch (IOException e) {
      throw new XmlParserException("can not parse the file " + file.getAbsolutePath(), e);

    } finally {
      IOUtils.closeQuietly(buffer);
    }
  }

  public void parse(InputStream stream) {
    BufferedReader buffer = null;
    try {
      buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
      parse(buffer);

    } catch (IOException e) {
      throw new XmlParserException("can not parse the stream", e);

    } finally {
      IOUtils.closeQuietly(buffer);
    }
  }

  private void parse(BufferedReader buffer) throws IOException {
    parse(IOUtils.toString(buffer));
  }

  public void parse(String xml) {
    try {
      String fixedXml = fixUnicodeChar(xml);
      doc = builder.parse(new ByteArrayInputStream(fixedXml.getBytes(StandardCharsets.UTF_8)));
      XPathFactory factory = XPathFactory.newInstance();
      xpath = factory.newXPath();

    } catch (IOException | SAXException e) {
      throw new XmlParserException(CAN_NOT_PARSE_XML + xml, e);
    }
  }

  public Element getRoot() {
    if (root == null && doc != null) {
      root = doc.getDocumentElement();
    }
    return root;
  }

  public Document getDocument() {
    return doc;
  }

  public Element getChildElement(Element base, String elementName) {
    NodeList childrens = base.getElementsByTagName(elementName);
    for (int i = 0; i < childrens.getLength(); i++) {
      Node nde = childrens.item(i);
      if (nde.getNodeType() == Node.ELEMENT_NODE) {
        return (Element) nde;
      }
    }
    return null;
  }

  public Element getChildElement(String elementName) {
    NodeList childrens = getRoot().getElementsByTagName(elementName);
    for (int i = 0; i < childrens.getLength(); i++) {
      Node nde = childrens.item(i);
      if (nde.getNodeType() == Node.ELEMENT_NODE) {
        return (Element) nde;
      }
    }
    return null;
  }

  public List<Element> getChildElements(String elementName) {
    List<Element> rtrVal = new ArrayList<>();
    NodeList childrens = getRoot().getElementsByTagName(elementName);
    for (int i = 0; i < childrens.getLength(); i++) {
      Node nde = childrens.item(i);
      if (nde.getNodeType() == Node.ELEMENT_NODE) {
        rtrVal.add((Element) nde);
      }
    }
    return rtrVal;
  }

  public List<Element> getChildElements(Element base, String elementName) {
    List<Element> rtrVal = new ArrayList<>();
    NodeList childrens = base.getElementsByTagName(elementName);
    for (int i = 0; i < childrens.getLength(); i++) {
      Node nde = childrens.item(i);
      if (nde.getNodeType() == Node.ELEMENT_NODE) {
        rtrVal.add((Element) nde);
      }
    }
    return rtrVal;
  }

  public String getChildElementValue(Element base, String elementName) {
    NodeList childrens = base.getElementsByTagName(elementName);
    for (int i = 0; i < childrens.getLength(); i++) {
      if (childrens.item(i).getNodeType() == Node.ELEMENT_NODE) {
        return childrens.item(i).getFirstChild().getNodeValue();
      }
    }
    return null;
  }

  public String getElementValue(Node base) {
    if (base.getNextSibling() != null && base.getNextSibling().getNodeType() == Node.TEXT_NODE) {
      return base.getNextSibling().getNodeValue();
    } else if (base.getFirstChild() != null && base.getFirstChild().getNodeType() == Node.TEXT_NODE) {
      return base.getFirstChild().getNodeValue();
    }
    return null;
  }

  public String getChildElementValue(String elementName) {
    NodeList childrens = getRoot().getElementsByTagName(elementName);
    for (int i = 0; i < childrens.getLength(); i++) {
      if (childrens.item(i).getNodeType() == Node.ELEMENT_NODE) {
        return childrens.item(i).getFirstChild().getNodeValue();
      }
    }
    return null;
  }

  public Object executeXPath(Node node, QName qname, String xPathExpression) {
    XPathExpression expr = compiledExprs.get(xPathExpression);
    try {
      if (expr == null) {
        expr = xpath.compile(xPathExpression);
        compiledExprs.put(xPathExpression, expr);
      }
      return expr.evaluate(node, qname);

    } catch (XPathExpressionException e) {
      throw new XmlParserException("Unable to evaluate xpath expression :" + xPathExpression, e);
    }
  }

  public String executeXPath(String xPathExpression) {
    return (String) executeXPath(doc, XPathConstants.STRING, xPathExpression);
  }

  public String executeXPath(Node node, String xPathExpression) {
    return (String) executeXPath(node, XPathConstants.STRING, xPathExpression);
  }

  public NodeList executeXPathNodeList(String xPathExpression) {
    return (NodeList) executeXPath(doc, XPathConstants.NODESET, xPathExpression);
  }

  public NodeList executeXPathNodeList(Node node, String xPathExpression) {
    return (NodeList) executeXPath(node, XPathConstants.NODESET, xPathExpression);
  }

  public Node executeXPathNode(Node node, String xPathExpression) {
    return (Node) executeXPath(node, XPathConstants.NODE, xPathExpression);
  }

  /**
   * Fix the error occured when parsing a string containing unicode character
   * Example : {@code &u20ac;} will be replaced by {@code &#x20ac;}
   */
  protected String fixUnicodeChar(String text) {
    String unicode = "&u";
    StringBuilder replace = new StringBuilder(text);
    if (text.indexOf(unicode) >= 0) {
      Pattern p = Pattern.compile("&u([0-9a-fA-F]{1,4});");
      Matcher m = p.matcher(replace.toString());
      int nbFind = 0;
      while (m.find()) {
        // Add one index each time because we add one character each time (&u -> &#x)
        replace.replace(m.start() + nbFind, m.end() + nbFind, "&#x" + m.group(1) + ";");
        nbFind++;
      }
    }
    return replace.toString();
  }
}

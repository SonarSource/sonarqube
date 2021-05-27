/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.api.rules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.PropertyType;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Cardinality;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @since 2.3
 * @deprecated in 4.2. Replaced by org.sonar.api.server.rule.RulesDefinition and org.sonar.api.server.rule.RulesDefinitionXmlLoader
 */
@Deprecated
@ServerSide
@ComputeEngineSide
public final class XMLRuleParser {
  private static final Map<String, String> TYPE_MAP = typeMapWithDeprecatedValues();
  private static final String ELEMENT_RULES = "rules";
  private static final String ELEMENT_RULE = "rule";
  private static final String ELEMENT_PARAM = "param";

  public List<Rule> parse(File file) {
    try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), UTF_8)) {
      return parse(reader);

    } catch (IOException e) {
      throw new SonarException("Fail to load the file: " + file, e);
    }
  }

  /**
   * Warning : the input stream is closed in this method
   */
  public List<Rule> parse(InputStream input) {
    try (Reader reader = new InputStreamReader(input, UTF_8)) {
      return parse(reader);

    } catch (IOException e) {
      throw new SonarException("Fail to load the xml stream", e);
    }
  }

  public List<Rule> parse(Reader inputReader) {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    try {
      final XMLEventReader reader = xmlFactory.createXMLEventReader(inputReader);
      List<Rule> rules = new ArrayList<>();
      while (reader.hasNext()) {
        final XMLEvent event = reader.nextEvent();
        if (event.isStartElement() && event.asStartElement().getName()
          .getLocalPart().equals(ELEMENT_RULES)) {
          parseRules(rules, reader);
        }
      }
      return rules;

    } catch (XMLStreamException e) {
      throw new SonarException("XML is not valid", e);
    }
  }

  private void parseRules(List<Rule> rules, XMLEventReader reader) throws XMLStreamException {
    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_RULES)) {
        return;
      }
      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart();
        switch (elementName) {
          case ELEMENT_RULE:
            Rule rule = Rule.create();
            rules.add(rule);
            parseRule(rule, element, reader);
            break;
        }
      }
    }
  }

  private static void parseRule(Rule rule, StartElement ruleElement, XMLEventReader reader) throws XMLStreamException {
    /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT */
    Attribute keyAttribute = ruleElement.getAttributeByName(new QName("key"));
    if (keyAttribute != null && StringUtils.isNotBlank(keyAttribute.getValue())) {
      rule.setKey(StringUtils.trim(keyAttribute.getValue()));
    }

    /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT */
    Attribute priorityAttribute = ruleElement.getAttributeByName(new QName("priority"));
    if (priorityAttribute != null && StringUtils.isNotBlank(priorityAttribute.getValue())) {
      rule.setSeverity(RulePriority.valueOf(StringUtils.trim(priorityAttribute.getValue())));
    }

    List<String> tags = new ArrayList<>();
    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_RULE)) {
        return;
      }
      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart().toLowerCase(Locale.ENGLISH);
        switch (elementName) {
          case "name":
            rule.setName(StringUtils.trim(reader.getElementText()));
            break;
          case "description":
            rule.setDescription(StringUtils.trim(reader.getElementText()));
            break;
          case "key":
            rule.setKey(StringUtils.trim(reader.getElementText()));
            break;
          case "configkey":
            rule.setConfigKey(StringUtils.trim(reader.getElementText()));
            break;
          case "priority":
            rule.setSeverity(RulePriority.valueOf(StringUtils.trim(reader.getElementText())));
            break;
          case "cardinality":
            rule.setCardinality(Cardinality.valueOf(StringUtils.trim(reader.getElementText())));
            break;
          case "status":
            rule.setStatus(StringUtils.trim(reader.getElementText()));
            break;
          case ELEMENT_PARAM:
            processParameter(rule, element, reader);
            break;
          case "tag":
            tags.add(StringUtils.trim(reader.getElementText()));
            break;
        }
      }
    }
    if (rule.getKey() == null || rule.getKey().isEmpty()) {
      throw new SonarException("Node <key> is missing in <rule>");
    }
    rule.setTags(tags.toArray(new String[tags.size()]));
  }

  private static void processParameter(Rule rule, StartElement paramElement, XMLEventReader reader) throws XMLStreamException {
    RuleParam param = rule.createParameter();

    Attribute keyAttribute = paramElement.getAttributeByName(new QName("key"));
    if (keyAttribute != null && StringUtils.isNotBlank(keyAttribute.getValue())) {
      /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT */
      param.setKey(StringUtils.trim(keyAttribute.getValue()));
    }

    Attribute typeAttribute = paramElement.getAttributeByName(new QName("type"));
    if (typeAttribute != null && StringUtils.isNotBlank(typeAttribute.getValue())) {
      /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT */
      param.setType(type(StringUtils.trim(typeAttribute.getValue())));
    }

    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_PARAM)) {
        return;
      }
      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart().toLowerCase(Locale.ENGLISH);
        switch (elementName) {
          case "key":
            param.setKey(StringUtils.trim(reader.getElementText()));
            break;
          case "description":
            param.setDescription(StringUtils.trim(reader.getElementText()));
            break;
          case "type":
            param.setType(type(StringUtils.trim(reader.getElementText())));
            break;
          case "defaultvalue":
            param.setDefaultValue(StringUtils.trim(reader.getElementText()));
            break;
        }
      }
    }
    if (StringUtils.isEmpty(param.getKey())) {
      throw new SonarException("Node <key> is missing in <param>");
    }
  }

  private static Map<String, String> typeMapWithDeprecatedValues() {
    Map<String, String> map = new HashMap<>();
    map.put("i", PropertyType.INTEGER.name());
    map.put("s", PropertyType.STRING.name());
    map.put("b", PropertyType.BOOLEAN.name());
    map.put("r", PropertyType.REGULAR_EXPRESSION.name());
    map.put("s{}", "s{}");
    map.put("i{}", "i{}");
    for (PropertyType propertyType : PropertyType.values()) {
      map.put(propertyType.name(), propertyType.name());
    }
    return map;
  }

  static String type(String type) {
    String validType = TYPE_MAP.get(type);
    if (null != validType) {
      return validType;
    }

    if (type.matches(".\\[.+\\]")) {
      return type;
    }
    throw new SonarException("Invalid property type [" + type + "]");
  }

}

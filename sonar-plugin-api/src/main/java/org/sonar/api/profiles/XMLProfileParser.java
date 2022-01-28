/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.profiles;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.profile.BuiltInQualityProfileAnnotationLoader;
import org.sonar.api.utils.ValidationMessages;

/**
 * @since 2.3
 * @deprecated since 7.8 use {@link BuiltInQualityProfileAnnotationLoader}
 */
@Deprecated
@ServerSide
@ComputeEngineSide
public class XMLProfileParser {

  private static final String ELEMENT_PROFILE = "profile";
  private static final String ELEMENT_RULES = "rules";
  private static final String ELEMENT_RULE = "rule";
  private static final String ELEMENT_PARAMETERS = "parameters";
  private static final String ELEMENT_PARAMETER = "parameter";
  private final RuleFinder ruleFinder;

  /**
   * For backward compatibility.
   *
   * @deprecated since 2.5. Plugins shouldn't directly instantiate this class,
   * because it should be retrieved as an IoC dependency.
   */
  @Deprecated
  public XMLProfileParser(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  public RulesProfile parseResource(ClassLoader classloader, String xmlClassPath, ValidationMessages messages) {
    try (Reader reader = new InputStreamReader(classloader.getResourceAsStream(xmlClassPath), StandardCharsets.UTF_8)) {
      return parse(reader, messages);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to close stream", e);
    }
  }

  public RulesProfile parse(Reader inputReader, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create();
    XMLInputFactory inputFactory = initStax();
    try {
      final XMLEventReader reader = inputFactory.createXMLEventReader(inputReader);
      while (reader.hasNext()) {
        final XMLEvent event = reader.nextEvent();
        if (event.isStartElement() && event.asStartElement().getName()
          .getLocalPart().equals(ELEMENT_PROFILE)) {
          parseProfile(profile, reader, messages);
        }
      }
    } catch (XMLStreamException e) {
      messages.addErrorText("XML is not valid: " + e.getMessage());
    }
    checkProfile(profile, messages);
    return profile;
  }

  private void parseProfile(RulesProfile profile, final XMLEventReader reader, ValidationMessages messages) throws XMLStreamException {
    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_PROFILE)) {
        return;
      }
      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart();
        if (ELEMENT_RULES.equals(elementName)) {
          parseRules(profile, reader, messages);
        } else if ("name".equals(elementName)) {
          profile.setName(StringUtils.trim(reader.getElementText()));
        } else if ("language".equals(elementName)) {
          profile.setLanguage(StringUtils.trim(reader.getElementText()));
        }
      }
    }
  }

  private void parseRules(RulesProfile profile, XMLEventReader reader, ValidationMessages messages) throws XMLStreamException {
    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_RULES)) {
        return;
      }
      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart();
        if (ELEMENT_RULE.equals(elementName)) {
          parseRule(profile, reader, messages);
        }
      }
    }
  }

  private static void checkProfile(RulesProfile profile, ValidationMessages messages) {
    if (StringUtils.isBlank(profile.getName())) {
      messages.addErrorText("The mandatory node <name> is missing.");
    }
    if (StringUtils.isBlank(profile.getLanguage())) {
      messages.addErrorText("The mandatory node <language> is missing.");
    }
  }

  private static XMLInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return xmlFactory;
  }

  private void parseRule(RulesProfile profile, XMLEventReader reader, ValidationMessages messages) throws XMLStreamException {
    Map<String, String> parameters = new HashMap<>();
    String repositoryKey = null;
    String key = null;
    RulePriority priority = null;
    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_RULE)) {
        buildRule(profile, messages, parameters, repositoryKey, key, priority);
        return;
      }

      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart();
        if ("repositoryKey".equals(elementName)) {
          repositoryKey = StringUtils.trim(reader.getElementText());
        } else if ("key".equals(elementName)) {
          key = StringUtils.trim(reader.getElementText());
        } else if ("priority".equals(elementName)) {
          priority = RulePriority.valueOf(StringUtils.trim(reader.getElementText()));
        } else if (ELEMENT_PARAMETERS.equals(elementName)) {
          processParameters(parameters, reader);
        }
      }
    }
  }

  private void buildRule(RulesProfile profile, ValidationMessages messages, Map<String, String> parameters, String repositoryKey, String key, @Nullable RulePriority priority) {
    Rule rule = ruleFinder.findByKey(repositoryKey, key);
    if (rule == null) {
      messages.addWarningText("Rule not found: " + ruleToString(repositoryKey, key));
    } else {
      ActiveRule activeRule = profile.activateRule(rule, priority);
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        if (rule.getParam(entry.getKey()) == null) {
          messages.addWarningText("The parameter '" + entry.getKey() + "' does not exist in the rule: " + ruleToString(repositoryKey, key));
        } else {
          activeRule.setParameter(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  private static String ruleToString(String repositoryKey, String key) {
    return "[repository=" + repositoryKey + ", key=" + key + "]";
  }

  private static void processParameters(Map<String, String> parameters, XMLEventReader reader) throws XMLStreamException {
    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_PARAMETERS)) {
        return;
      }
      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart();
        if (ELEMENT_PARAMETER.equals(elementName)) {
          processParameter(parameters, reader);
        }
      }
    }
  }

  private static void processParameter(Map<String, String> parameters, XMLEventReader reader) throws XMLStreamException {
    String key = null;
    String value = null;
    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();
      if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ELEMENT_PARAMETER)) {
        if (key != null) {
          parameters.put(key, value);
        }
        return;
      }
      if (event.isStartElement()) {
        final StartElement element = event.asStartElement();
        final String elementName = element.getName().getLocalPart();
        if ("key".equals(elementName)) {
          key = StringUtils.trim(reader.getElementText());
        } else if ("value".equals(elementName)) {
          value = StringUtils.trim(reader.getElementText());
        }
      }
    }
  }

}

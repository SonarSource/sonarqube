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
package org.sonar.api.rules;

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
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

  public List<Rule> parse(File file) {
    try (Reader reader = new InputStreamReader(FileUtils.openInputStream(file), UTF_8)) {
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

  public List<Rule> parse(Reader reader) {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    SMInputFactory inputFactory = new SMInputFactory(xmlFactory);
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <rules>
      List<Rule> rules = new ArrayList<>();

      SMInputCursor rulesC = rootC.childElementCursor("rule");
      while (rulesC.getNext() != null) {
        // <rule>
        Rule rule = Rule.create();
        rules.add(rule);

        processRule(rule, rulesC);
      }
      return rules;

    } catch (XMLStreamException e) {
      throw new SonarException("XML is not valid", e);
    }
  }

  private static void processRule(Rule rule, SMInputCursor ruleC) throws XMLStreamException {
    /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT */
    String keyAttribute = ruleC.getAttrValue("key");
    if (StringUtils.isNotBlank(keyAttribute)) {
      rule.setKey(StringUtils.trim(keyAttribute));
    }

    /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT */
    String priorityAttribute = ruleC.getAttrValue("priority");
    if (StringUtils.isNotBlank(priorityAttribute)) {
      rule.setSeverity(RulePriority.valueOf(StringUtils.trim(priorityAttribute)));
    }

    List<String> tags = new ArrayList<>();
    SMInputCursor cursor = ruleC.childElementCursor();

    while (cursor.getNext() != null) {
      String nodeName = cursor.getLocalName();

      if (StringUtils.equalsIgnoreCase("name", nodeName)) {
        rule.setName(StringUtils.trim(cursor.collectDescendantText(false)));

      } else if (StringUtils.equalsIgnoreCase("description", nodeName)) {
        rule.setDescription(StringUtils.trim(cursor.collectDescendantText(false)));

      } else if (StringUtils.equalsIgnoreCase("key", nodeName)) {
        rule.setKey(StringUtils.trim(cursor.collectDescendantText(false)));

      } else if (StringUtils.equalsIgnoreCase("configKey", nodeName)) {
        rule.setConfigKey(StringUtils.trim(cursor.collectDescendantText(false)));

      } else if (StringUtils.equalsIgnoreCase("priority", nodeName)) {
        rule.setSeverity(RulePriority.valueOf(StringUtils.trim(cursor.collectDescendantText(false))));

      } else if (StringUtils.equalsIgnoreCase("cardinality", nodeName)) {
        rule.setCardinality(Cardinality.valueOf(StringUtils.trim(cursor.collectDescendantText(false))));

      } else if (StringUtils.equalsIgnoreCase("status", nodeName)) {
        rule.setStatus(StringUtils.trim(cursor.collectDescendantText(false)));

      } else if (StringUtils.equalsIgnoreCase("param", nodeName)) {
        processParameter(rule, cursor);

      } else if (StringUtils.equalsIgnoreCase("tag", nodeName)) {
        tags.add(StringUtils.trim(cursor.collectDescendantText(false)));
      }
    }
    if (Strings.isNullOrEmpty(rule.getKey())) {
      throw new SonarException("Node <key> is missing in <rule>");
    }
    rule.setTags(tags.toArray(new String[tags.size()]));
  }

  private static void processParameter(Rule rule, SMInputCursor ruleC) throws XMLStreamException {
    RuleParam param = rule.createParameter();

    String keyAttribute = ruleC.getAttrValue("key");
    if (StringUtils.isNotBlank(keyAttribute)) {
      /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT */
      param.setKey(StringUtils.trim(keyAttribute));
    }

    String typeAttribute = ruleC.getAttrValue("type");
    if (StringUtils.isNotBlank(typeAttribute)) {
      /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT */
      param.setType(type(StringUtils.trim(typeAttribute)));
    }

    SMInputCursor paramC = ruleC.childElementCursor();
    while (paramC.getNext() != null) {
      String propNodeName = paramC.getLocalName();
      String propText = StringUtils.trim(paramC.collectDescendantText(false));
      if (StringUtils.equalsIgnoreCase("key", propNodeName)) {
        param.setKey(propText);

      } else if (StringUtils.equalsIgnoreCase("description", propNodeName)) {
        param.setDescription(propText);

      } else if (StringUtils.equalsIgnoreCase("type", propNodeName)) {
        param.setType(type(propText));

      } else if (StringUtils.equalsIgnoreCase("defaultValue", propNodeName)) {
        param.setDefaultValue(propText);
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

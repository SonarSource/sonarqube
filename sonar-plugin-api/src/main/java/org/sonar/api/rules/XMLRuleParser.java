/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rules;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.SonarException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.3
 */
public final class XMLRuleParser implements ServerComponent {

  public List<Rule> parse(File file) {
    Reader reader = null;
    try {
      reader = new InputStreamReader(FileUtils.openInputStream(file), CharEncoding.UTF_8);
      return parse(reader);

    } catch (IOException e) {
      throw new SonarException("Fail to load the file: " + file, e);

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  /**
   * Warning : the input stream is closed in this method
   */
  public List<Rule> parse(InputStream input) {
    Reader reader = null;
    try {
      reader = new InputStreamReader(input, CharEncoding.UTF_8);
      return parse(reader);

    } catch (IOException e) {
      throw new SonarException("Fail to load the xml stream", e);

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  public List<Rule> parse(Reader reader) {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    SMInputFactory inputFactory = new SMInputFactory(xmlFactory);
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <rules>
      List<Rule> rules = new ArrayList<Rule>();

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
      rule.setPriority(RulePriority.valueOf(StringUtils.trim(priorityAttribute)));
    }

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
        rule.setPriority(RulePriority.valueOf(StringUtils.trim(cursor.collectDescendantText(false))));

      } else if (StringUtils.equalsIgnoreCase("category", nodeName)) {
        /* BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT : attribute "name" */
        String category = StringUtils.trim(StringUtils.defaultString(cursor.getAttrValue("name"), cursor.collectDescendantText(false)));
        rule.setRulesCategory(new RulesCategory(category));

      } else if (StringUtils.equalsIgnoreCase("cardinality", nodeName)) {
        rule.setCardinality(Rule.Cardinality.valueOf(StringUtils.trim(cursor.collectDescendantText(false))));

      } else if (StringUtils.equalsIgnoreCase("param", nodeName)) {
        processParameter(rule, cursor);
      }
    }
    if (StringUtils.isEmpty(rule.getKey())) {
      throw new SonarException("Node <key> is missing in <rule>");
    }
    if (StringUtils.isEmpty(rule.getName())) {
      throw new SonarException("Node <name> is missing in <rule>");
    }
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
      param.setType(StringUtils.trim(typeAttribute));
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
        param.setType(propText);

      } else if (StringUtils.equalsIgnoreCase("defaultValue", propNodeName)) {
        param.setDefaultValue(propText);
      }
    }
    if (StringUtils.isEmpty(param.getKey())) {
      throw new SonarException("Node <key> is missing in <param>");
    }
  }
}

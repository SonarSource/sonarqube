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
package org.sonar.api.profiles;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.ServerComponent;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 2.3
 */
public final class XMLProfileParser implements ServerComponent {

  private RuleFinder ruleFinder;

  public XMLProfileParser(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  public RulesProfile parseResource(ClassLoader classloader, String xmlClassPath, ValidationMessages messages) {
    Reader reader = new InputStreamReader(classloader.getResourceAsStream(xmlClassPath), Charset.forName(CharEncoding.UTF_8));
    try {
      return parse(reader, messages);

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  public RulesProfile parse(Reader reader, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create();
    SMInputFactory inputFactory = initStax();
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <profile>
      SMInputCursor cursor = rootC.childElementCursor();
      while (cursor.getNext() != null) {
        String nodeName = cursor.getLocalName();
        if (StringUtils.equals("rules", nodeName)) {
          SMInputCursor rulesCursor = cursor.childElementCursor("rule");
          processRules(rulesCursor, profile, messages);

        } else if (StringUtils.equals("name", nodeName)) {
          profile.setName(StringUtils.trim(cursor.collectDescendantText(false)));

        } else if (StringUtils.equals("language", nodeName)) {
          profile.setLanguage(StringUtils.trim(cursor.collectDescendantText(false)));
        }
      }
    } catch (XMLStreamException e) {
      messages.addErrorText("XML is not valid: " + e.getMessage());
    }
    checkProfile(profile, messages);
    return profile;
  }

  private void checkProfile(RulesProfile profile, ValidationMessages messages) {
    if (StringUtils.isBlank(profile.getName())) {
      messages.addErrorText("The mandatory node <name> is missing.");
    }
    if (StringUtils.isBlank(profile.getLanguage())) {
      messages.addErrorText("The mandatory node <language> is missing.");
    }
  }

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    SMInputFactory inputFactory = new SMInputFactory(xmlFactory);
    return inputFactory;
  }

  private void processRules(SMInputCursor rulesCursor, RulesProfile profile, ValidationMessages messages) throws XMLStreamException {
    Map<String, String> parameters = new HashMap<String, String>();
    while (rulesCursor.getNext() != null) {
      SMInputCursor ruleCursor = rulesCursor.childElementCursor();

      String repositoryKey = null, key = null;
      RulePriority priority = null;
      parameters.clear();

      while (ruleCursor.getNext() != null) {
        String nodeName = ruleCursor.getLocalName();

        if (StringUtils.equals("repositoryKey", nodeName)) {
          repositoryKey = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("key", nodeName)) {
          key = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("priority", nodeName)) {
          priority = RulePriority.valueOf(StringUtils.trim(ruleCursor.collectDescendantText(false)));

        } else if (StringUtils.equals("parameters", nodeName)) {
          SMInputCursor propsCursor = ruleCursor.childElementCursor("parameter");
          processParameters(propsCursor, parameters);
        }
      }

      Rule rule = ruleFinder.findByKey(repositoryKey, key);
      if (rule == null) {
        messages.addWarningText("Rule not found: [repository=" + repositoryKey + ", key=" + key + "]");

      } else {
        ActiveRule activeRule = profile.activateRule(rule, priority);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
          if (rule.getParam(entry.getKey()) == null) {
            messages.addWarningText("The parameter '" + entry.getKey() + "' does not exist in the rule: [repository=" + repositoryKey + ", key=" + key + "]");
          } else {
            activeRule.setParameter(entry.getKey(), entry.getValue());
          }
        }
      }
    }
  }

  private void processParameters(SMInputCursor propsCursor, Map<String, String> parameters) throws XMLStreamException {
    while (propsCursor.getNext() != null) {
      SMInputCursor propCursor = propsCursor.childElementCursor();
      String key = null;
      String value = null;
      while (propCursor.getNext() != null) {
        String nodeName = propCursor.getLocalName();
        if (StringUtils.equals("key", nodeName)) {
          key = StringUtils.trim(propCursor.collectDescendantText(false));

        } else if (StringUtils.equals("value", nodeName)) {
          value = StringUtils.trim(propCursor.collectDescendantText(false));
        }
      }
      if (key != null) {
        parameters.put(key, value);
      }
    }
  }


}

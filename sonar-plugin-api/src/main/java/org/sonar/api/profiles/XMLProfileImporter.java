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

import org.apache.commons.lang.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.Reader;

/**
 * @since 2.3
 */
public final class XMLProfileImporter {

  private XMLProfileImporter() {
    // support all repositories
  }

  public static XMLProfileImporter create() {
    return new XMLProfileImporter();
  }

  public ProfilePrototype importProfile(Reader reader, ValidationMessages messages) {
    ProfilePrototype profile = ProfilePrototype.create();

    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);

    SMInputFactory inputFactory = new SMInputFactory(xmlFactory);
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <profile>
      SMInputCursor cursor = rootC.childElementCursor();
      while (cursor.getNext() != null) {
        String nodeName = cursor.getLocalName();
        if (StringUtils.equals("rules", nodeName)) {
          SMInputCursor rulesCursor = cursor.childElementCursor("rule");
          processRules(rulesCursor, profile);
        }
      }
    } catch (XMLStreamException e) {
      messages.addErrorText("XML is not valid: " + e.getMessage());
    }
    return profile;
  }

  private void processRules(SMInputCursor rulesCursor, ProfilePrototype profile) throws XMLStreamException {
    while (rulesCursor.getNext() != null) {
      SMInputCursor ruleCursor = rulesCursor.childElementCursor();
      ProfilePrototype.RulePrototype rule = ProfilePrototype.RulePrototype.create();
      profile.activateRule(rule);

      while (ruleCursor.getNext() != null) {
        String nodeName = ruleCursor.getLocalName();

        if (StringUtils.equals("repositoryKey", nodeName)) {
          rule.setRepositoryKey(StringUtils.trim(ruleCursor.collectDescendantText(false)));

        } else if (StringUtils.equals("key", nodeName)) {
          rule.setKey(StringUtils.trim(ruleCursor.collectDescendantText(false)));

        } else if (StringUtils.equals("priority", nodeName)) {
          rule.setPriority(RulePriority.valueOf(StringUtils.trim(ruleCursor.collectDescendantText(false))));

        } else if (StringUtils.equals("parameters", nodeName)) {
          SMInputCursor propsCursor = ruleCursor.childElementCursor("parameter");
          processParameters(propsCursor, rule);
        }
      }
    }
  }

  private void processParameters(SMInputCursor propsCursor, ProfilePrototype.RulePrototype rule) throws XMLStreamException {
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
        rule.setParameter(key, value);
      }
    }
  }


}

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
package org.sonar.plugins.checkstyle;

import org.apache.commons.lang.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.ProfilePrototype;
import org.sonar.api.resources.Java;
import org.sonar.api.utils.ValidationMessages;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.Reader;

public class CheckstyleProfileImporter extends ProfileImporter {

  private static final String CHECKER_MODULE = "Checker";
  private static final String TREEWALKER_MODULE = "TreeWalker";
  private static final String MODULE_NODE = "module";

  public CheckstyleProfileImporter() {
    super(CheckstyleConstants.REPOSITORY_KEY, CheckstyleConstants.PLUGIN_NAME);
    setSupportedLanguages(Java.KEY);
  }

  @Override
  public ProfilePrototype importProfile(Reader reader, ValidationMessages messages) {
    SMInputFactory inputFactory = initStax();
    ProfilePrototype profile = ProfilePrototype.create();
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <module name="Checker">
      SMInputCursor rootModulesCursor = rootC.childElementCursor(MODULE_NODE);
      while (rootModulesCursor.getNext() != null) {
        String configKey = rootModulesCursor.getAttrValue("name");
        if (StringUtils.equals(TREEWALKER_MODULE, configKey)) {
          SMInputCursor treewalkerCursor = rootModulesCursor.childElementCursor(MODULE_NODE);
          while (treewalkerCursor.getNext() != null) {
            processModule(profile, CHECKER_MODULE + "/" + TREEWALKER_MODULE + "/", treewalkerCursor, messages);
          }
        } else {
          processModule(profile, CHECKER_MODULE + "/", rootModulesCursor, messages);
        }
      }
    } catch (XMLStreamException e) {
      messages.addError("unvalidXml", "XML is not valid: " + e.getMessage());
    }
    return profile;
  }

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    SMInputFactory inputFactory = new SMInputFactory(xmlFactory);
    return inputFactory;
  }

  private void processModule(ProfilePrototype profile, String path, SMInputCursor moduleCursor, ValidationMessages messages) throws XMLStreamException {
    String configKey = path + moduleCursor.getAttrValue("name");
    ProfilePrototype.RulePrototype rule = ProfilePrototype.RulePrototype.createByConfigKey(CheckstyleConstants.REPOSITORY_KEY, configKey);

    SMInputCursor propertyCursor = moduleCursor.childElementCursor("property");
    while (propertyCursor.getNext() != null) {
      processProperty(rule, propertyCursor, messages);
    }

    profile.activateRule(rule);

  }

  private void processProperty(ProfilePrototype.RulePrototype rule, SMInputCursor propertyCursor, ValidationMessages messages) throws XMLStreamException {
    String key = propertyCursor.getAttrValue("name");
    String value = propertyCursor.getAttrValue("value");
    if (StringUtils.equals("id", key)) {
      messages.addWarning("checkstyle.idPropertyNotSupported", "The property 'id' is not supported.");

    } else if (StringUtils.equals("severity", key)) {
      rule.setPriority(CheckstyleSeverityUtils.fromSeverity(value));

    } else {
      rule.setParameter(key, value);
    }
  }
}

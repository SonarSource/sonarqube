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

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.Reader;
import java.util.Map;

public class CheckstyleProfileImporter extends ProfileImporter {

  private static final String CHECKER_MODULE = "Checker";
  private static final String TREEWALKER_MODULE = "TreeWalker";
  private static final String MODULE_NODE = "module";
  private final RuleFinder ruleFinder;

  public CheckstyleProfileImporter(RuleFinder ruleFinder) {
    super(CheckstyleConstants.REPOSITORY_KEY, CheckstyleConstants.PLUGIN_NAME);
    setSupportedLanguages(Java.KEY);
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
    SMInputFactory inputFactory = initStax();
    RulesProfile profile = RulesProfile.create();
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
      messages.addErrorText("XML is not valid: " + e.getMessage());
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

  private void processModule(RulesProfile profile, String path, SMInputCursor moduleCursor, ValidationMessages messages) throws XMLStreamException {
    String moduleName = moduleCursor.getAttrValue("name");
    if (isFilter(moduleName)) {
      messages.addWarningText("Checkstyle filters are not imported: " + moduleName);

    } else if (!isIgnored(moduleName)) {
      processRule(profile, path, moduleName, moduleCursor, messages);
    }
  }

  static boolean isIgnored(String configKey) {
    return StringUtils.equals(configKey, "FileContentsHolder");
  }

  static boolean isFilter(String configKey) {
    return StringUtils.equals(configKey, "SuppressionCommentFilter") ||
        StringUtils.equals(configKey, "SeverityMatchFilter") ||
        StringUtils.equals(configKey, "SuppressionFilter") ||
        StringUtils.equals(configKey, "SuppressWithNearbyCommentFilter");
  }

  private void processRule(RulesProfile profile, String path, String moduleName, SMInputCursor moduleCursor, ValidationMessages messages) throws XMLStreamException {
    Map<String, String> properties = processProps(moduleCursor);

    Rule rule;
    String id = properties.get("id");
    String warning;
    if (StringUtils.isNotBlank(id)) {
      rule = ruleFinder.find(RuleQuery.create().withRepositoryKey(CheckstyleConstants.REPOSITORY_KEY).withKey(id));
      warning = "Checkstyle rule with key '" + id + "' not found";

    } else {
      String configKey = path + moduleName;
      rule = ruleFinder.find(RuleQuery.create().withRepositoryKey(CheckstyleConstants.REPOSITORY_KEY).withConfigKey(configKey));
      warning = "Checkstyle rule with config key '" + configKey + "' not found";
    }

    if (rule == null) {
      messages.addWarningText(warning);

    } else {
      ActiveRule activeRule = profile.activateRule(rule, null);
      activateProperties(activeRule, properties);
    }
  }

  private Map<String, String> processProps(SMInputCursor moduleCursor) throws XMLStreamException {
    Map<String, String> props = Maps.newHashMap();
    SMInputCursor propertyCursor = moduleCursor.childElementCursor("property");
    while (propertyCursor.getNext() != null) {
      String key = propertyCursor.getAttrValue("name");
      String value = propertyCursor.getAttrValue("value");
      props.put(key, value);
    }
    return props;
  }

  private void activateProperties(ActiveRule activeRule, Map<String, String> properties) {
    for (Map.Entry<String, String> property : properties.entrySet()) {
      if (StringUtils.equals("severity", property.getKey())) {
        activeRule.setPriority(CheckstyleSeverityUtils.fromSeverity(property.getValue()));

      } else if (!StringUtils.equals("id", property.getKey())) {
        activeRule.setParameter(property.getKey(), property.getValue());
      }
    }
  }
}

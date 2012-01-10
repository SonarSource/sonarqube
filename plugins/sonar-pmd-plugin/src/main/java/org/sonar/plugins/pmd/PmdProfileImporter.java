/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.pmd;

import java.io.Reader;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.pmd.xml.PmdProperty;
import org.sonar.plugins.pmd.xml.PmdRule;
import org.sonar.plugins.pmd.xml.PmdRuleset;

public class PmdProfileImporter extends ProfileImporter {

  private final RuleFinder ruleFinder;
  private static final Logger LOG = LoggerFactory.getLogger(PmdProfileImporter.class);

  public PmdProfileImporter(RuleFinder ruleFinder) {
    super(PmdConstants.REPOSITORY_KEY, PmdConstants.PLUGIN_NAME);
    setSupportedLanguages(Java.KEY);
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile importProfile(Reader pmdConfigurationFile, ValidationMessages messages) {
    PmdRuleset pmdRuleset = parsePmdRuleset(pmdConfigurationFile, messages);
    return createRuleProfile(pmdRuleset, messages);
  }

  protected RulesProfile createRuleProfile(PmdRuleset pmdRuleset, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create();
    for (PmdRule pmdRule : pmdRuleset.getPmdRules()) {
      if (PmdConstants.XPATH_CLASS.equals(pmdRule.getClazz())) {
        messages.addWarningText("PMD XPath rule '" + pmdRule.getName()
            + "' can't be imported automatically. The rule must be created manually through the Sonar web interface.");
        continue;
      }
      if (pmdRule.getRef() == null) {
        messages.addWarningText("A PMD rule without 'ref' attribute can't be imported. see '" + pmdRule.getClazz() + "'");
        continue;
      }
      Rule rule = ruleFinder.find(RuleQuery.create().withRepositoryKey(PmdConstants.REPOSITORY_KEY).withConfigKey(pmdRule.getRef()));
      if (rule != null) {
        ActiveRule activeRule = profile.activateRule(rule, PmdLevelUtils.fromLevel(pmdRule.getPriority()));
        if (pmdRule.getProperties() != null) {
          for (PmdProperty prop : pmdRule.getProperties()) {
            if (rule.getParam(prop.getName()) == null) {
              messages.addWarningText("The property '" + prop.getName() + "' is not supported in the pmd rule: " + pmdRule.getRef());
              continue;
            }
            activeRule.setParameter(prop.getName(), prop.getValue());
          }
        }
      } else {
        messages.addWarningText("Unable to import unknown PMD rule '" + pmdRule.getRef() + "'");
      }
    }
    return profile;
  }

  protected PmdRuleset parsePmdRuleset(Reader pmdConfigurationFile, ValidationMessages messages) {
    try {
      SAXBuilder parser = new SAXBuilder();
      Document dom = parser.build(pmdConfigurationFile);
      Element eltResultset = dom.getRootElement();
      Namespace namespace = eltResultset.getNamespace();
      PmdRuleset pmdResultset = new PmdRuleset();
      for (Element eltRule : getChildren(eltResultset, "rule", namespace)) {
        PmdRule pmdRule = new PmdRule(eltRule.getAttributeValue("ref"));
        pmdRule.setClazz(eltRule.getAttributeValue("class"));
        pmdRule.setName(eltRule.getAttributeValue("name"));
        pmdRule.setMessage(eltRule.getAttributeValue("message"));
        parsePmdPriority(eltRule, pmdRule, namespace);
        parsePmdProperties(eltRule, pmdRule, namespace);
        pmdResultset.addRule(pmdRule);
      }
      return pmdResultset;
    } catch (Exception e) {
      String errorMessage = "The PMD configuration file is not valid";
      messages.addErrorText(errorMessage + " : " + e.getMessage());
      LOG.error(errorMessage, e);
      return new PmdRuleset();
    }
  }

  private List<Element> getChildren(Element parent, String childName, Namespace namespace) {
    if (namespace == null) {
      return (List<Element>) parent.getChildren(childName);
    } else {
      return (List<Element>) parent.getChildren(childName, namespace);
    }
  }

  private void parsePmdProperties(Element eltRule, PmdRule pmdRule, Namespace namespace) {
    for (Element eltProperties : getChildren(eltRule, "properties", namespace)) {
      for (Element eltProperty : getChildren(eltProperties, "property", namespace)) {
        pmdRule.addProperty(new PmdProperty(eltProperty.getAttributeValue("name"), eltProperty.getAttributeValue("value")));
      }
    }
  }

  private void parsePmdPriority(Element eltRule, PmdRule pmdRule, Namespace namespace) {
    for (Element eltPriority : getChildren(eltRule, "priority", namespace)) {
      pmdRule.setPriority(eltPriority.getValue());
    }
  }
}

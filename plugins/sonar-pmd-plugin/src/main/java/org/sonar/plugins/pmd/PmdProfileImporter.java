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
package org.sonar.plugins.pmd;

import java.io.Reader;

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

import com.thoughtworks.xstream.XStream;

public class PmdProfileImporter extends ProfileImporter {

  private final RuleFinder ruleFinder;

  public PmdProfileImporter(RuleFinder ruleFinder) {
    super(PmdConstants.REPOSITORY_KEY, PmdConstants.PLUGIN_NAME);
    setSupportedLanguages(Java.KEY);
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile importProfile(Reader pmdConfigurationFile, ValidationMessages messages) {
    PmdRuleset pmdRuleset = parsePmdRuleset(pmdConfigurationFile, messages);
    RulesProfile profile = createRuleProfile(pmdRuleset, messages);
    return profile;
  }

  protected RulesProfile createRuleProfile(PmdRuleset pmdRuleset, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create();
    for (PmdRule pmdRule : pmdRuleset.getPmdRules()) {
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
      XStream xstream = new XStream();
      xstream.setClassLoader(getClass().getClassLoader());
      xstream.processAnnotations(PmdRuleset.class);
      xstream.processAnnotations(PmdRule.class);
      xstream.processAnnotations(PmdProperty.class);
      return (PmdRuleset) xstream.fromXML(pmdConfigurationFile);
    } catch (RuntimeException e) {
      messages.addErrorText("The PMD configuration file is not valide: " + e.getMessage());
      return new PmdRuleset();
    }
  }
}

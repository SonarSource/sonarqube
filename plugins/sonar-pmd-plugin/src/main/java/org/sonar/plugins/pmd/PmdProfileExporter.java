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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.pmd.xml.Property;
import org.sonar.plugins.pmd.xml.PmdRule;
import org.sonar.plugins.pmd.xml.PmdRuleset;

import com.thoughtworks.xstream.XStream;

public class PmdProfileExporter extends ProfileExporter {

  public PmdProfileExporter() {
    super(PmdConstants.REPOSITORY_KEY, PmdConstants.PLUGIN_NAME);
    setSupportedLanguages(Java.KEY);
    setMimeType("application/xml");
  }

  @Override
  public void exportProfile(RulesProfile profile, Writer writer) {
    try {
      PmdRuleset tree = buildModuleTree(profile.getActiveRulesByRepository(PmdConstants.REPOSITORY_KEY), profile.getName());
      String xmlModules = buildXmlFromModuleTree(tree);
      writer.append(xmlModules);
    } catch (IOException e) {
      throw new SonarException("Fail to export the profile " + profile, e);
    }
  }

  protected PmdRuleset buildModuleTree(List<ActiveRule> activeRules, String profileName) {
    PmdRuleset ruleset = new PmdRuleset(profileName);
    for (ActiveRule activeRule : activeRules) {
      if (activeRule.getRule().getPluginName().equals(CoreProperties.PMD_PLUGIN)) {
        String configKey = activeRule.getRule().getConfigKey();
        PmdRule rule = new PmdRule(configKey, to(activeRule.getPriority()));
        List<Property> properties = null;
        if (activeRule.getActiveRuleParams() != null && !activeRule.getActiveRuleParams().isEmpty()) {
          properties = new ArrayList<Property>();
          for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
            properties.add(new Property(activeRuleParam.getRuleParam().getKey(), activeRuleParam.getValue()));
          }
        }
        rule.setProperties(properties);
        ruleset.addRule(rule);
      }
    }
    return ruleset;
  }

  protected String buildXmlFromModuleTree(PmdRuleset tree) {
    XStream xstream = new XStream();
    xstream.setClassLoader(getClass().getClassLoader());
    xstream.processAnnotations(PmdRuleset.class);
    xstream.processAnnotations(PmdRule.class);
    xstream.processAnnotations(Property.class);
    return xstream.toXML(tree);
  }

  private String to(RulePriority priority) {
    if (priority.equals(RulePriority.BLOCKER)) {
      return "1";
    }
    if (priority.equals(RulePriority.CRITICAL)) {
      return "2";
    }
    if (priority.equals(RulePriority.MAJOR)) {
      return "3";
    }
    if (priority.equals(RulePriority.MINOR)) {
      return "4";
    }
    if (priority.equals(RulePriority.INFO)) {
      return "5";
    }
    throw new IllegalArgumentException("Level not supported: " + priority);
  }

}

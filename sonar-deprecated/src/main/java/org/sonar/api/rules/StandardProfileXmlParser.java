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

import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.xml.Profile;
import org.sonar.api.rules.xml.Property;
import org.sonar.api.utils.SonarException;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class StandardProfileXmlParser {

  private final List<Rule> rules;

  public StandardProfileXmlParser() {
    rules = new ArrayList<Rule>();
  }

  public StandardProfileXmlParser(List<Rule> rules) {
    this.rules = rules;
  }

  /**
   * see the XML format into the unit test src/test/java/.../StandardProfileXmlParserTest
   */
  public Profile parse(String xml) {
    return (Profile) getXStream().fromXML(xml);
  }

  private XStream getXStream() {
    XStream xstream = new XStream();
    xstream.processAnnotations(Profile.class);
    xstream.processAnnotations(Rule.class);
    xstream.processAnnotations(Property.class);
    return xstream;
  }

  public RulesProfile importConfiguration(String configuration) {
    RulesProfile rulesProfile = new RulesProfile();
    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();
    Profile profile = buildProfileFromXml(configuration);

    rulesProfile.setName(profile.getName());
    rulesProfile.setLanguage(profile.getLanguage());

    if (StringUtils.isBlank(rulesProfile.getName())) {
      throw new SonarException("Profile name can't be null or empty");
    }

    buildActiveRulesFromProfile(profile, activeRules);
    rulesProfile.setActiveRules(activeRules);
    return rulesProfile;
  }

  protected Profile buildProfileFromXml(String configuration) {
    StandardProfileXmlParser xstream = new StandardProfileXmlParser();
    return xstream.parse(configuration);
  }

  protected void buildActiveRulesFromProfile(Profile profile, List<ActiveRule> activeRules) {
    if (profile.getRules() != null && !profile.getRules().isEmpty()) {
      for (org.sonar.api.rules.xml.Rule module : profile.getRules()) {
        String ref = module.getKey();
        for (Rule rule : rules) {
          if (rule.getConfigKey().equals(ref)) {
            RulePriority rulePriority = getRulePriority(module);
            ActiveRule activeRule = new ActiveRule(null, rule, rulePriority);
            activeRule.setActiveRuleParams(getActiveRuleParams(module, rule, activeRule));
            activeRules.add(activeRule);
            break;
          }
        }
      }
    }
  }

  private RulePriority getRulePriority(org.sonar.api.rules.xml.Rule module) {
    return StringUtils.isBlank(module.getPriority()) ? null : RulePriority.valueOfString(module.getPriority());
  }

  private List<ActiveRuleParam> getActiveRuleParams(org.sonar.api.rules.xml.Rule module, Rule rule, ActiveRule activeRule) {
    List<ActiveRuleParam> activeRuleParams = new ArrayList<ActiveRuleParam>();
    if (module.getProperties() != null) {
      for (Property property : module.getProperties()) {
        if (rule.getParams() != null) {
          for (RuleParam ruleParam : rule.getParams()) {
            if (ruleParam.getKey().equals(property.getName())) {
              activeRuleParams.add(new ActiveRuleParam(activeRule, ruleParam, property.getValue()));
            }
          }
        }
      }
    }
    return activeRuleParams;
  }

}
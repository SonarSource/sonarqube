/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.findbugs;

import java.io.Reader;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.findbugs.xml.FindBugsFilter;

import com.thoughtworks.xstream.XStream;

public class FindbugsProfileImporter extends ProfileImporter {

  private final RuleFinder ruleFinder;
  private static final Logger LOG = LoggerFactory.getLogger(FindbugsProfileImporter.class);

  public FindbugsProfileImporter(RuleFinder ruleFinder) {
    super(FindbugsConstants.REPOSITORY_KEY, FindbugsConstants.PLUGIN_NAME);
    setSupportedLanguages(Java.KEY);
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile importProfile(Reader findbugsConf, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create();
    try {
      XStream xStream = FindBugsFilter.createXStream();
      FindBugsFilter filter = (FindBugsFilter) xStream.fromXML(findbugsConf);

      activateRulesByCategory(profile, filter, messages);
      activateRulesByCode(profile, filter, messages);
      activateRulesByPattern(profile, filter, messages);

      return profile;
    } catch (Exception e) {
      String errorMessage = "The Findbugs configuration file is not valid";
      messages.addErrorText(errorMessage + " : " + e.getMessage());
      LOG.error(errorMessage, e);
      return profile;
    }
  }

  private void activateRulesByPattern(RulesProfile profile, FindBugsFilter filter, ValidationMessages messages) {
    for (Map.Entry<String, RulePriority> patternLevel : filter.getPatternLevels(new FindbugsLevelUtils()).entrySet()) {
      Rule rule = ruleFinder.findByKey(FindbugsConstants.REPOSITORY_KEY, patternLevel.getKey());
      if (rule != null) {
        profile.activateRule(rule, patternLevel.getValue());
      } else {
        messages.addWarningText("Unable to activate unknown rule : '" + patternLevel.getKey() + "'");
      }
    }
  }

  private void activateRulesByCode(RulesProfile profile, FindBugsFilter filter, ValidationMessages messages) {
    for (Map.Entry<String, RulePriority> codeLevel : filter.getCodeLevels(new FindbugsLevelUtils()).entrySet()) {
      boolean someRulesHaveBeenActivated = false;
      for (Rule rule : ruleFinder.findAll(RuleQuery.create().withRepositoryKey(FindbugsConstants.REPOSITORY_KEY))) {
        if (rule.getKey().equals(codeLevel.getKey()) || StringUtils.startsWith(rule.getKey(), codeLevel.getKey() + "_")) {
          someRulesHaveBeenActivated = true;
          profile.activateRule(rule, codeLevel.getValue());
        }
      }
      if ( !someRulesHaveBeenActivated) {
        messages.addWarningText("Unable to find any rules associated to code  : '" + codeLevel.getKey() + "'");
      }
    }
  }

  private void activateRulesByCategory(RulesProfile profile, FindBugsFilter filter, ValidationMessages messages) {
    for (Map.Entry<String, RulePriority> categoryLevel : filter.getCategoryLevels(new FindbugsLevelUtils()).entrySet()) {
      boolean someRulesHaveBeenActivated = false;
      String sonarCateg = FindbugsCategory.findbugsToSonar(categoryLevel.getKey());
      for (Rule rule : ruleFinder.findAll(RuleQuery.create().withRepositoryKey(FindbugsConstants.REPOSITORY_KEY))) {
        if (sonarCateg != null && rule.getName().startsWith(sonarCateg)) {
          someRulesHaveBeenActivated = true;
          profile.activateRule(rule, categoryLevel.getValue());
        }
      }
      if ( !someRulesHaveBeenActivated) {
        messages.addWarningText("Unable to find any rules associated to category  : '" + categoryLevel.getKey() + "'");
      }
    }
  }
}

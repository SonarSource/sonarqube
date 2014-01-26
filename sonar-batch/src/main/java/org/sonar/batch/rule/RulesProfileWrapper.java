/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.rule;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.SonarException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This wrapper is used to try to preserve backward compatibility for plugins that used to
 * depends on {@link org.sonar.api.profiles.RulesProfile}
 *
 * @since 4.2
 */
public class RulesProfileWrapper extends RulesProfile {

  private static final Logger LOG = LoggerFactory.getLogger(RulesProfileWrapper.class);

  private final Collection<RulesProfile> profiles;
  private final RulesProfile singleLanguageProfile;

  RulesProfileWrapper(Collection<RulesProfile> profiles) {
    this.profiles = profiles;
    this.singleLanguageProfile = null;
  }

  RulesProfileWrapper(RulesProfile profile) {
    this.profiles = Lists.newArrayList(profile);
    this.singleLanguageProfile = profile;
  }

  @Override
  public Integer getId() {
    return getSingleProfileOrFail().getId();
  }

  private RulesProfile getSingleProfileOrFail() {
    if (singleLanguageProfile != null) {
      throw new SonarException("Please update your plugin to support multi-language analysis");
    }
    return singleLanguageProfile;
  }

  @Override
  public String getName() {
    return getSingleProfileOrFail().getName();
  }

  @Override
  public String getLanguage() {
    if (singleLanguageProfile == null) {
      // Multi-languages module
      // FIXME This is a hack for CommonChecksDecorator that call this method in its constructor
      LOG.debug("Please update your plugin to support multi-language analysis", new SonarException("Please update your plugin to support multi-language analysis"));
      return "";
    }
    return singleLanguageProfile.getName();
  }

  @Override
  public List<Alert> getAlerts() {
    List<Alert> result = new ArrayList<Alert>();
    for (RulesProfile profile : profiles) {
      result.addAll(profile.getAlerts());
    }
    return result;
  }

  @Override
  public List<ActiveRule> getActiveRules() {
    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();
    for (RulesProfile profile : profiles) {
      activeRules.addAll(profile.getActiveRules());
    }
    return activeRules;
  }

  @Override
  public int getVersion() {
    return getSingleProfileOrFail().getVersion();
  }

  @Override
  public ActiveRule getActiveRule(String repositoryKey, String ruleKey) {
    for (RulesProfile profile : profiles) {
      ActiveRule activeRule = profile.getActiveRule(repositoryKey, ruleKey);
      if (activeRule != null) {
        return activeRule;
      }
    }
    return null;
  }

  @Override
  public List<ActiveRule> getActiveRulesByRepository(String repositoryKey) {
    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();
    for (RulesProfile profile : profiles) {
      activeRules.addAll(profile.getActiveRulesByRepository(repositoryKey));
    }
    return activeRules;
  }

  @Override
  public List<ActiveRule> getActiveRules(boolean acceptDisabledRules) {
    List<ActiveRule> activeRules = new ArrayList<ActiveRule>();
    for (RulesProfile profile : profiles) {
      activeRules.addAll(profile.getActiveRules(acceptDisabledRules));
    }
    return activeRules;
  }

  @Override
  public ActiveRule getActiveRule(Rule rule) {
    for (RulesProfile profile : profiles) {
      ActiveRule activeRule = profile.getActiveRule(rule);
      if (activeRule != null) {
        return activeRule;
      }
    }
    return null;
  }

  @Override
  public Boolean getDefaultProfile() {
    return getSingleProfileOrFail().getDefaultProfile();
  }

}

/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.SonarException;

/**
 * This wrapper is used to try to preserve backward compatibility for plugins that used to
 * depends on {@link org.sonar.api.profiles.RulesProfile}
 *
 * @since 4.2
 */
public class RulesProfileWrapper extends RulesProfile {

  private static final Logger LOG = LoggerFactory.getLogger(RulesProfileWrapper.class);
  private static final String DEPRECATED_USAGE_MESSAGE = "Please update your plugin to support multi-language analysis";

  private final Collection<RulesProfile> profiles;
  private final RulesProfile singleLanguageProfile;

  public RulesProfileWrapper(Collection<RulesProfile> profiles) {
    this.profiles = profiles;
    this.singleLanguageProfile = null;
  }

  public RulesProfileWrapper(RulesProfile profile) {
    this.profiles = Collections.singletonList(profile);
    this.singleLanguageProfile = profile;
  }

  @Override
  public Integer getId() {
    return getSingleProfileOrFail().getId();
  }

  private RulesProfile getSingleProfileOrFail() {
    if (singleLanguageProfile == null) {
      throw new IllegalStateException(DEPRECATED_USAGE_MESSAGE);
    }
    return singleLanguageProfile;
  }

  @Override
  public String getName() {
    return singleLanguageProfile != null ? singleLanguageProfile.getName() : "SonarQube";
  }

  @Override
  public String getLanguage() {
    if (singleLanguageProfile == null) {
      // Multi-languages module
      // This is a hack for CommonChecksDecorator that call this method in its constructor
      LOG.debug(DEPRECATED_USAGE_MESSAGE, new SonarException(DEPRECATED_USAGE_MESSAGE));
      return "";
    }
    return singleLanguageProfile.getLanguage();
  }

  @Override
  public List<ActiveRule> getActiveRules() {
    List<ActiveRule> activeRules = new ArrayList<>();
    for (RulesProfile profile : profiles) {
      activeRules.addAll(profile.getActiveRules());
    }
    return activeRules;
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
    List<ActiveRule> activeRules = new ArrayList<>();
    for (RulesProfile profile : profiles) {
      activeRules.addAll(profile.getActiveRulesByRepository(repositoryKey));
    }
    return activeRules;
  }

  @Override
  public List<ActiveRule> getActiveRules(boolean acceptDisabledRules) {
    List<ActiveRule> activeRules = new ArrayList<>();
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

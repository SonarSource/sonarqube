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
package org.sonar.api.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.MessageException;

/**
 * This class is badly named. It should be "QualityProfile". Indeed it does not relate only to rules but to metric thresholds too.
 */
public class RulesProfile implements Cloneable {

  /**
   * Name of the default profile "Sonar Way"
   * @deprecated in 4.2. Use your own constant.
   */
  @Deprecated
  public static final String SONAR_WAY_NAME = "Sonar way";

  /**
   * Name of the default java profile "Sonar way with Findbugs"
   * @deprecated in 4.2. Use your own constant.
   */
  @Deprecated
  public static final String SONAR_WAY_FINDBUGS_NAME = "Sonar way with Findbugs";

  /**
   * Name of the default java profile "Sun checks"
   * @deprecated in 4.2. Use your own constant.
   */
  @Deprecated
  public static final String SUN_CONVENTIONS_NAME = "Sun checks";

  private String name;
  private Boolean defaultProfile = Boolean.FALSE;
  private String language;
  private List<ActiveRule> activeRules = new ArrayList<>();

  /**
   * @deprecated use the factory method create()
   */
  @Deprecated
  public RulesProfile() {
  }

  /**
   * @deprecated since 2.3. Use the factory method create()
   */
  @Deprecated
  public RulesProfile(String name, String language) {
    this.name = name;
    this.language = language;
    this.activeRules = new ArrayList<>();
  }

  /**
   * @deprecated since 2.3. Use the factory method create()
   */
  @Deprecated
  public RulesProfile(String name, String language, boolean defaultProfile, /* kept for backward-compatibility */boolean provided) {
    this(name, language);
    this.defaultProfile = defaultProfile;
  }

  public Integer getId() {
    return null;
  }

  /**
   * @return the profile name, unique by language.
   */
  public String getName() {
    return name;
  }

  /**
   * Set the profile name.
   */
  public RulesProfile setName(String s) {
    this.name = s;
    return this;
  }

  /**
   * @deprecated profile versioning is dropped in 4.4. Always returns -1.
   */
  @Deprecated
  public int getVersion() {
    return -1;
  }

  /**
   * @deprecated profile versioning is dropped in 4.4. Always returns -1.
   */
  @Deprecated
  public RulesProfile setVersion(int version) {
    // ignore
    return this;
  }

  /**
   * @deprecated profile versioning is dropped in 4.4. Always returns null.
   */
  @CheckForNull
  @Deprecated
  public Boolean getUsed() {
    return null;
  }

  /**
   * @deprecated profile versioning is dropped in 4.4. Always returns -1.
   */
  @Deprecated
  public RulesProfile setUsed(Boolean used) {
    return this;
  }

  /**
   * @return the list of active rules
   */
  public List<ActiveRule> getActiveRules() {
    return getActiveRules(false);
  }

  /**
   * @return the list of active rules
   */
  public List<ActiveRule> getActiveRules(boolean acceptDisabledRules) {
    if (acceptDisabledRules) {
      return activeRules;
    }
    List<ActiveRule> result = new ArrayList<>();
    for (ActiveRule activeRule : activeRules) {
      if (activeRule.isEnabled()) {
        result.add(activeRule);
      }
    }
    return result;
  }

  public RulesProfile removeActiveRule(ActiveRule activeRule) {
    activeRules.remove(activeRule);
    return this;
  }

  public RulesProfile addActiveRule(ActiveRule activeRule) {
    activeRules.add(activeRule);
    return this;
  }

  /**
   * Set the list of active rules
   */
  public void setActiveRules(List<ActiveRule> activeRules) {
    this.activeRules = activeRules;
  }

  /**
   * @return whether this is the default profile for the language
   */
  public Boolean getDefaultProfile() {
    return defaultProfile;
  }

  /**
   * Set whether this is the default profile for the language. The default profile is used when none is explicitly defined when auditing a
   * project.
   */
  public void setDefaultProfile(Boolean b) {
    this.defaultProfile = b;
  }

  /**
   * @return the profile language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Set the profile language
   */
  public RulesProfile setLanguage(String s) {
    this.language = s;
    return this;
  }

  /**
   * Does nothing.
   *
   * @return {@code null}
   * @deprecated in 6.5
   */
  @Deprecated
  @CheckForNull
  public String getParentName() {
    return null;
  }

  /**
   * Does nothing.
   *
   * @deprecated in 6.5
   */
  @Deprecated
  public void setParentName(String parentName) {
    // does nothing
  }

  /**
   * Note: disabled rules are excluded.
   *
   * @return the list of active rules for a given severity
   */
  public List<ActiveRule> getActiveRules(RulePriority severity) {
    List<ActiveRule> result = new ArrayList<>();
    for (ActiveRule activeRule : activeRules) {
      if (activeRule.getSeverity().equals(severity) && activeRule.isEnabled()) {
        result.add(activeRule);
      }
    }
    return result;
  }

  /**
   * Get the active rules of a specific repository.
   * Only enabled rules are selected. Disabled rules are excluded.
   */
  public List<ActiveRule> getActiveRulesByRepository(String repositoryKey) {
    List<ActiveRule> result = new ArrayList<>();
    for (ActiveRule activeRule : activeRules) {
      if (repositoryKey.equals(activeRule.getRepositoryKey()) && activeRule.isEnabled()) {
        result.add(activeRule);
      }
    }
    return result;
  }

  /**
   * Note: disabled rules are excluded.
   *
   * @return an active rule from a plugin key and a rule key if the rule is activated, null otherwise
   */
  @CheckForNull
  public ActiveRule getActiveRule(String repositoryKey, String ruleKey) {
    for (ActiveRule activeRule : activeRules) {
      if (StringUtils.equals(activeRule.getRepositoryKey(), repositoryKey) && StringUtils.equals(activeRule.getRuleKey(), ruleKey) && activeRule.isEnabled()) {
        return activeRule;
      }
    }
    return null;
  }

  /**
   * Note: disabled rules are excluded.
   */
  @CheckForNull
  public ActiveRule getActiveRuleByConfigKey(String repositoryKey, String configKey) {
    for (ActiveRule activeRule : activeRules) {
      if (StringUtils.equals(activeRule.getRepositoryKey(), repositoryKey) && StringUtils.equals(activeRule.getConfigKey(), configKey) && activeRule.isEnabled()) {
        return activeRule;
      }
    }
    return null;
  }

  /**
   * Note: disabled rules are excluded.
   */
  @CheckForNull
  public ActiveRule getActiveRule(Rule rule) {
    return getActiveRule(rule.getRepositoryKey(), rule.getKey());
  }

  /**
   * @param optionalSeverity if null, then the default rule severity is used
   */
  public ActiveRule activateRule(final Rule rule, @Nullable RulePriority optionalSeverity) {
    if (activeRules.stream().anyMatch(ar -> ar.getRule().equals(rule))) {
      throw MessageException.of(String.format(
        "The definition of the profile '%s' (language '%s') contains multiple occurrences of the '%s:%s' rule. The plugin which declares this profile should fix this.",
        getName(), getLanguage(), rule.getRepositoryKey(), rule.getKey()));
    }
    ActiveRule activeRule = new ActiveRule(this, rule, optionalSeverity);
    activeRules.add(activeRule);
    return activeRule;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RulesProfile)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RulesProfile other = (RulesProfile) obj;
    return new EqualsBuilder().append(language, other.getLanguage()).append(name, other.getName()).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(language).append(name).toHashCode();
  }

  @Override
  public Object clone() {
    RulesProfile clone = RulesProfile.create(getName(), getLanguage());
    clone.setDefaultProfile(getDefaultProfile());
    if (activeRules != null && !activeRules.isEmpty()) {
      clone.setActiveRules(activeRules.stream()
        .map(ar -> (ActiveRule) ar.clone())
        .collect(Collectors.toList()));
    }
    return clone;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("[name=").append(name).append(",language=").append(language).append("]").toString();
  }

  public static RulesProfile create(String name, String language) {
    return new RulesProfile().setName(name).setLanguage(language);
  }

  public static RulesProfile create() {
    return new RulesProfile();
  }

}

/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Cache of the data required to activate/deactivate
 * multiple rules on a Quality profile, including
 * the rule definitions, the rule parameters, the tree
 * of profiles hierarchy and its related active rules.
 */
class RuleActivationContext {

  private final long date;

  // the profiles
  private RulesProfileDto baseRulesProfile;
  @Nullable
  private QProfileDto baseProfile;
  private final Map<String, QProfileDto> profilesByUuid = new HashMap<>();
  private final ListMultimap<String, QProfileDto> profilesByParentUuid = ArrayListMultimap.create();
  private final List<QProfileDto> builtInAliases = new ArrayList<>();

  // the rules
  private final Map<Integer, RuleWrapper> rulesById;
  private final Map<ActiveRuleKey, ActiveRuleWrapper> activeRulesByKey;

  // cursor, moved in the tree of profiles
  private boolean cascading = false;
  private RulesProfileDto currentRulesProfile;
  @Nullable
  private QProfileDto currentProfile;
  @Nullable
  private RuleWrapper currentRule;
  @Nullable
  private ActiveRuleWrapper currentActiveRule;
  @Nullable
  private ActiveRuleWrapper currentParentActiveRule;

  private RuleActivationContext(Builder builder) {
    this.date = builder.date;

    // rules
    this.rulesById = Maps.newHashMapWithExpectedSize(builder.rules.size());
    ListMultimap<Integer, RuleParamDto> paramsByRuleId = builder.ruleParams.stream().collect(index(RuleParamDto::getRuleId));
    for (RuleDefinitionDto rule : builder.rules) {
      RuleWrapper wrapper = new RuleWrapper(rule, paramsByRuleId.get(rule.getId()));
      rulesById.put(rule.getId(), wrapper);
    }

    // profiles
    this.baseProfile = builder.baseProfile;
    this.baseRulesProfile = builder.baseRulesProfile;
    for (QProfileDto profile : builder.profiles) {
      profilesByUuid.put(profile.getKee(), profile);
      if (profile.isBuiltIn()) {
        builtInAliases.add(profile);
      } else if (profile.getParentKee() != null) {
        profilesByParentUuid.put(profile.getParentKee(), profile);
      }
    }

    // active rules
    this.activeRulesByKey = Maps.newHashMapWithExpectedSize(builder.activeRules.size());
    ListMultimap<Integer, ActiveRuleParamDto> paramsByActiveRuleId = builder.activeRuleParams.stream().collect(index(ActiveRuleParamDto::getActiveRuleId));
    for (ActiveRuleDto activeRule : builder.activeRules) {
      ActiveRuleWrapper wrapper = new ActiveRuleWrapper(activeRule, paramsByActiveRuleId.get(activeRule.getId()));
      this.activeRulesByKey.put(activeRule.getKey(), wrapper);
    }
  }

  long getDate() {
    return date;
  }

  RuleWrapper getRule() {
    return currentRule;
  }

  @CheckForNull
  String getRequestedParamValue(RuleActivation request, String key) {
    if (currentRule.rule.isCustomRule()) {
      return null;
    }
    return request.getParameter(key);
  }

  boolean hasRequestedParamValue(RuleActivation request, String key) {
    return request.hasParameter(key);
  }

  RulesProfileDto getRulesProfile() {
    return currentRulesProfile;
  }

  @CheckForNull
  ActiveRuleWrapper getActiveRule() {
    return currentActiveRule;
  }

  @CheckForNull
  ActiveRuleWrapper getParentActiveRule() {
    return currentParentActiveRule;
  }

  boolean isCascading() {
    return cascading;
  }

  @CheckForNull
  QProfileDto getProfile() {
    return currentProfile;
  }

  Collection<QProfileDto> getChildProfiles() {
    if (currentProfile != null) {
      return profilesByParentUuid.get(currentProfile.getKee());
    }
    // on built-in profile
    checkState(currentRulesProfile.isBuiltIn());
    return builtInAliases.stream()
      .flatMap(a -> profilesByParentUuid.get(a.getKee()).stream())
      .collect(Collectors.toList());
  }

  public void reset(int ruleId) {
    this.cascading = false;
    doSwitch(this.baseProfile, this.baseRulesProfile, ruleId);
  }

  /**
   * Moves cursor to a child profile
   */
  void switchToChild(QProfileDto to) {
    checkState(!to.isBuiltIn());
    requireNonNull(this.currentRule, "can not switch profile if rule is not set");
    RuleDefinitionDto rule = this.currentRule.get();

    QProfileDto qp = requireNonNull(this.profilesByUuid.get(to.getKee()), () -> "No profile with uuid " + to.getKee());
    RulesProfileDto rulesProfile = RulesProfileDto.from(qp);

    this.cascading = true;
    doSwitch(qp, rulesProfile, rule.getId());
  }

  private void doSwitch(@Nullable QProfileDto qp, RulesProfileDto rulesProfile, int ruleId) {
    this.currentRule = rulesById.get(ruleId);
    checkRequest(this.currentRule != null, "Rule not found: %s", ruleId);
    RuleKey ruleKey = currentRule.get().getKey();
    checkRequest(rulesProfile.getLanguage().equals(currentRule.get().getLanguage()),
      "%s rule %s cannot be activated on %s profile %s", currentRule.get().getLanguage(), ruleKey, rulesProfile.getLanguage(), rulesProfile.getName());
    this.currentRulesProfile = rulesProfile;
    this.currentProfile = qp;
    this.currentActiveRule = this.activeRulesByKey.get(ActiveRuleKey.of(rulesProfile, ruleKey));
    this.currentParentActiveRule = null;
    if (this.currentProfile != null) {
      String parentUuid = this.currentProfile.getParentKee();
      if (parentUuid != null) {
        QProfileDto parent = requireNonNull(this.profilesByUuid.get(parentUuid), () -> "No profile with uuid " + parentUuid);
        this.currentParentActiveRule = this.activeRulesByKey.get(ActiveRuleKey.of(parent, ruleKey));
      }
    }
  }

  static final class Builder {
    private long date = System.currentTimeMillis();
    private RulesProfileDto baseRulesProfile;
    private QProfileDto baseProfile;
    private Collection<RuleDefinitionDto> rules;
    private Collection<RuleParamDto> ruleParams;
    private Collection<QProfileDto> profiles;
    private Collection<ActiveRuleDto> activeRules;
    private Collection<ActiveRuleParamDto> activeRuleParams;

    Builder setDate(long l) {
      this.date = l;
      return this;
    }

    Builder setBaseProfile(RulesProfileDto p) {
      this.baseRulesProfile = p;
      this.baseProfile = null;
      return this;
    }

    Builder setBaseProfile(QProfileDto p) {
      this.baseRulesProfile = RulesProfileDto.from(p);
      this.baseProfile = p;
      return this;
    }

    Builder setRules(Collection<RuleDefinitionDto> rules) {
      this.rules = rules;
      return this;
    }

    Builder setRuleParams(Collection<RuleParamDto> ruleParams) {
      this.ruleParams = ruleParams;
      return this;
    }

    /**
     * All the profiles involved in the activation workflow, including the
     * parent profile, even if it's not updated.
     */
    Builder setProfiles(Collection<QProfileDto> profiles) {
      this.profiles = profiles;
      return this;
    }

    Builder setActiveRules(Collection<ActiveRuleDto> activeRules) {
      this.activeRules = activeRules;
      return this;
    }

    Builder setActiveRuleParams(Collection<ActiveRuleParamDto> activeRuleParams) {
      this.activeRuleParams = activeRuleParams;
      return this;
    }

    RuleActivationContext build() {
      checkArgument(date > 0, "date is not set");
      requireNonNull(baseRulesProfile, "baseRulesProfile is null");
      requireNonNull(rules, "rules is null");
      requireNonNull(ruleParams, "ruleParams is null");
      requireNonNull(profiles, "profiles is null");
      requireNonNull(activeRules, "activeRules is null");
      requireNonNull(activeRuleParams, "activeRuleParams is null");
      return new RuleActivationContext(this);
    }
  }

  static final class RuleWrapper {
    private final RuleDefinitionDto rule;
    private final Map<String, RuleParamDto> paramsByKey;

    private RuleWrapper(RuleDefinitionDto rule, Collection<RuleParamDto> params) {
      this.rule = rule;
      this.paramsByKey = params.stream().collect(uniqueIndex(RuleParamDto::getName));
    }

    RuleDefinitionDto get() {
      return rule;
    }

    Collection<RuleParamDto> getParams() {
      return paramsByKey.values();
    }

    @CheckForNull
    RuleParamDto getParam(String key) {
      return paramsByKey.get(key);
    }

    @CheckForNull
    String getParamDefaultValue(String key) {
      RuleParamDto param = getParam(key);
      return param != null ? param.getDefaultValue() : null;
    }
  }

  static final class ActiveRuleWrapper {
    private final ActiveRuleDto activeRule;
    private final Map<String, ActiveRuleParamDto> paramsByKey;

    private ActiveRuleWrapper(ActiveRuleDto activeRule, Collection<ActiveRuleParamDto> params) {
      this.activeRule = activeRule;
      this.paramsByKey = params.stream().collect(uniqueIndex(ActiveRuleParamDto::getKey));
    }

    ActiveRuleDto get() {
      return activeRule;
    }

    Collection<ActiveRuleParamDto> getParams() {
      return paramsByKey.values();
    }

    @CheckForNull
    ActiveRuleParamDto getParam(String key) {
      return paramsByKey.get(key);
    }

    @CheckForNull
    String getParamValue(String key) {
      ActiveRuleParamDto param = paramsByKey.get(key);
      return param != null ? param.getValue() : null;
    }
  }
}

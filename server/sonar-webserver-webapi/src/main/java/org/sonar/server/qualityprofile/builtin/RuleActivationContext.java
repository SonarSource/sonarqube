/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualityprofile.builtin;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.RuleActivation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

/**
 * Cache of the data required to activate/deactivate
 * multiple rules on a Quality profile, including
 * the rule definitions, the rule parameters, the tree
 * of profiles hierarchy and its related active rules.
 */
public class RuleActivationContext {

  private final long date;

  // The profile that is initially targeted by the operation
  private final RulesProfileDto baseRulesProfile;

  private final Map<String, QProfileDto> profilesByUuid = new HashMap<>();
  private final ListMultimap<String, QProfileDto> profilesByParentUuid = ArrayListMultimap.create();

  // The rules/active rules involved in the group of activations/de-activations
  private final Map<String, RuleWrapper> rulesByUuid = new HashMap<>();
  private final Map<ActiveRuleKey, ActiveRuleWrapper> activeRulesByKey = new HashMap<>();

  // Cursors used to move in the rules and in the tree of profiles.

  private RulesProfileDto currentRulesProfile;
  // Cardinality is zero-to-many when cursor is on a built-in rules profile,
  // otherwise it's always one, and only one (cursor on descendants or on non-built-in base profile).
  private Collection<QProfileDto> currentProfiles;
  private RuleWrapper currentRule;
  private ActiveRuleWrapper currentActiveRule;
  private ActiveRuleWrapper currentParentActiveRule;

  private boolean descendantsLoaded = false;
  private final DescendantProfilesSupplier descendantProfilesSupplier;

  private RuleActivationContext(Builder builder) {
    this.date = builder.date;
    this.descendantProfilesSupplier = builder.descendantProfilesSupplier;

    ListMultimap<String, RuleParamDto> paramsByRuleId = builder.ruleParams.stream().collect(index(RuleParamDto::getRuleUuid));
    for (RuleDto rule : builder.rules) {
      RuleWrapper wrapper = new RuleWrapper(rule, paramsByRuleId.get(rule.getUuid()));
      rulesByUuid.put(rule.getUuid(), wrapper);
    }

    this.baseRulesProfile = builder.baseRulesProfile;
    register(builder.profiles);
    register(builder.activeRules, builder.activeRuleParams);
  }

  private void register(Collection<QProfileDto> profiles) {
    for (QProfileDto profile : profiles) {
      profilesByUuid.put(profile.getKee(), profile);
      if (profile.getParentKee() != null) {
        profilesByParentUuid.put(profile.getParentKee(), profile);
      }
    }
  }

  private void register(Collection<ActiveRuleDto> activeRules, Collection<ActiveRuleParamDto> activeRuleParams) {
    ListMultimap<String, ActiveRuleParamDto> paramsByActiveRuleUuid = activeRuleParams.stream().collect(index(ActiveRuleParamDto::getActiveRuleUuid));
    for (ActiveRuleDto activeRule : activeRules) {
      register(activeRule, paramsByActiveRuleUuid.get(activeRule.getUuid()));
    }
  }

  void register(ActiveRuleDto activeRule, Collection<ActiveRuleParamDto> activeRuleParams) {
    ActiveRuleWrapper wrapper = new ActiveRuleWrapper(activeRule, activeRuleParams);
    this.activeRulesByKey.put(activeRule.getKey(), wrapper);
  }

  long getDate() {
    return date;
  }

  /**
   * The rule currently selected.
   */
  public RuleWrapper getRule() {
    checkState(currentRule != null, "Rule has not been set yet");
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

  /**
   * The rules profile being selected.
   */
  RulesProfileDto getRulesProfile() {
    checkState(currentRulesProfile != null, "Rule profile has not been set yet");
    return currentRulesProfile;
  }

  /**
   * The active rule related to the selected profile and rule.
   * @return null if the selected rule is not activated on the selected profile.
   * @see #getRulesProfile()
   * @see #getRule()
   */
  @CheckForNull
  ActiveRuleWrapper getActiveRule() {
    return currentActiveRule;
  }

  /**
   * The active rule related to the rule and the parent of the selected profile.
   * @return null if the selected rule is not activated on the parent profile.
   * @see #getRule()
   */
  @CheckForNull
  ActiveRuleWrapper getParentActiveRule() {
    return currentParentActiveRule;
  }

  /**
   * Whether the profile cursor is on the base profile or not.
   */
  boolean isCascading() {
    return currentRulesProfile != null && !currentRulesProfile.getUuid().equals(baseRulesProfile.getUuid());
  }

  /**
   * The profiles being selected. Can be zero or many if {@link #getRulesProfile()} is built-in.
   * Else the collection always contains a single profile.
   */
  Collection<QProfileDto> getProfiles() {
    checkState(currentProfiles != null, "Profiles have not been set yet");
    return currentProfiles;
  }

  /**
   * The children of {@link #getProfiles()}
   */
  Collection<QProfileDto> getChildProfiles() {
    loadDescendants();
    return getProfiles().stream()
      .flatMap(p -> profilesByParentUuid.get(p.getKee()).stream())
      .toList();
  }

  private void loadDescendants() {
    if (descendantsLoaded) {
      return;
    }
    Collection<QProfileDto> baseProfiles = profilesByUuid.values().stream()
      .filter(p -> p.getRulesProfileUuid().equals(baseRulesProfile.getUuid()))
      .toList();
    DescendantProfilesSupplier.Result result = descendantProfilesSupplier.get(baseProfiles, rulesByUuid.keySet());
    register(result.profiles());
    register(result.activeRules(), result.activeRuleParams());
    descendantsLoaded = true;
  }

  /**
   * Move the cursor to the given rule and back to the base profile.
   */
  public void reset(String ruleUuid) {
    doSwitch(this.baseRulesProfile, ruleUuid);
  }

  /**
   * Moves cursor to a child profile
   */
  void selectChild(QProfileDto to) {
    checkState(!to.isBuiltIn());
    QProfileDto qp = requireNonNull(this.profilesByUuid.get(to.getKee()), () -> "No profile with uuid " + to.getKee());

    RulesProfileDto ruleProfile = RulesProfileDto.from(qp);
    doSwitch(ruleProfile, getRule().get().getUuid());
  }

  private void doSwitch(RulesProfileDto ruleProfile, String ruleUuid) {
    this.currentRule = rulesByUuid.get(ruleUuid);
    checkRequest(this.currentRule != null, "Rule with UUID %s not found", ruleUuid);
    RuleKey ruleKey = currentRule.get().getKey();

    this.currentRulesProfile = ruleProfile;
    this.currentProfiles = profilesByUuid.values().stream()
      .filter(p -> p.getRulesProfileUuid().equals(ruleProfile.getUuid()))
      .toList();
    this.currentActiveRule = this.activeRulesByKey.get(ActiveRuleKey.of(ruleProfile, ruleKey));
    this.currentParentActiveRule = this.currentProfiles.stream()
      .map(QProfileDto::getParentKee)
      .filter(Objects::nonNull)
      .map(profilesByUuid::get)
      .filter(Objects::nonNull)
      .findFirst()
      .map(profile -> activeRulesByKey.get(ActiveRuleKey.of(profile, ruleKey)))
      .orElse(null);
  }

  static final class Builder {
    private long date = System.currentTimeMillis();
    private RulesProfileDto baseRulesProfile;
    private Collection<RuleDto> rules;
    private Collection<RuleParamDto> ruleParams;
    private Collection<QProfileDto> profiles;
    private Collection<ActiveRuleDto> activeRules;
    private Collection<ActiveRuleParamDto> activeRuleParams;
    private DescendantProfilesSupplier descendantProfilesSupplier;

    Builder setDate(long l) {
      this.date = l;
      return this;
    }

    Builder setBaseProfile(RulesProfileDto p) {
      this.baseRulesProfile = p;
      return this;
    }

    Builder setRules(Collection<RuleDto> rules) {
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

    Builder setDescendantProfilesSupplier(DescendantProfilesSupplier d) {
      this.descendantProfilesSupplier = d;
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
      requireNonNull(descendantProfilesSupplier, "descendantProfilesSupplier is null");
      return new RuleActivationContext(this);
    }
  }

  public static final class RuleWrapper {
    private final RuleDto rule;
    private final Map<String, RuleParamDto> paramsByKey;

    private RuleWrapper(RuleDto rule, Collection<RuleParamDto> params) {
      this.rule = rule;
      this.paramsByKey = params.stream().collect(Collectors.toMap(RuleParamDto::getName, Function.identity()));
    }

    public RuleDto get() {
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
      this.paramsByKey = params.stream().collect(Collectors.toMap(ActiveRuleParamDto::getKey, Function.identity()));
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

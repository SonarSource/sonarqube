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

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgQProfileDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.qualityprofile.QProfileImpactSeverityMapper;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.builtin.RuleActivationContext.ActiveRuleWrapper;
import org.sonar.server.qualityprofile.builtin.RuleActivationContext.RuleWrapper;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.TRUE;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

/**
 * Activation and deactivation of rules in Quality profiles
 */
@ServerSide
public class RuleActivator {

  private final System2 system2;
  private final DbClient db;
  private final UuidFactory uuidFactory;
  private final TypeValidations typeValidations;
  private final UserSession userSession;
  private final Configuration configuration;
  private final SonarQubeVersion sonarQubeVersion;

  public RuleActivator(System2 system2, DbClient db, UuidFactory uuidFactory, TypeValidations typeValidations, UserSession userSession,
    Configuration configuration, SonarQubeVersion sonarQubeVersion) {
    this.system2 = system2;
    this.db = db;
    this.uuidFactory = uuidFactory;
    this.typeValidations = typeValidations;
    this.userSession = userSession;
    this.configuration = configuration;
    this.sonarQubeVersion = sonarQubeVersion;
  }

  public List<ActiveRuleChange> activate(DbSession dbSession, Collection<RuleActivation> activations, RuleActivationContext context) {
    return activations.stream().map(a -> activate(dbSession, a, context))
      .flatMap(List::stream)
      .toList();
  }

  public List<ActiveRuleChange> activate(DbSession dbSession, RuleActivation activation, RuleActivationContext context) {
    context.reset(activation.getRuleUuid());
    return doActivateRecursively(dbSession, activation, context);
  }

  private List<ActiveRuleChange> doActivateRecursively(DbSession dbSession, RuleActivation activation, RuleActivationContext context) {
    RuleDto rule = context.getRule().get();
    checkRequest(RuleStatus.REMOVED != rule.getStatus(), "Rule was removed: %s", rule.getKey());
    checkRequest(!rule.isTemplate(), "Rule template can't be activated on a Quality profile: %s", rule.getKey());
    checkRequest(context.getRulesProfile().getLanguage().equals(rule.getLanguage()),
      "%s rule %s cannot be activated on %s profile %s", rule.getLanguage(), rule.getKey(), context.getRulesProfile().getLanguage(),
      context.getRulesProfile().getName());
    List<ActiveRuleChange> changes = new ArrayList<>();
    ActiveRuleChange change = null;

    ActiveRuleWrapper activeRule = context.getActiveRule();
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(context.getRulesProfile(), rule.getKey());
    if (activeRule == null) {
      if (activation.isReset()) {
        // ignore reset when rule is not activated
        return changes;
      }
      change = handleNewRuleActivation(activation, context, rule, activeRuleKey);
    } else {
      // already activated

      // No change if propagating to descendants, but child profile already overrides rule
      if (!context.isCascading() || !activeRule.get().doesOverride()) {
        change = new ActiveRuleChange(ActiveRuleChange.Type.UPDATED, activeRuleKey, rule);
        change.setRule(context.getRule().get());
        handleUpdatedRuleActivation(activation, context, change, activeRule);

        if (isSame(change, activeRule) || (context.isCascading() && activeRule.get().getInheritance() != null && !isSameAsParent(change,
          context))) {
          // The rule config hasn't changed; or the rule is being propagated but the parent has a different config,
          // which means the rule was overridden by a profile in the inheritance chain
          change = null;
        }
      }
    }

    if (change != null) {
      changes.add(change);
      persist(change, context, dbSession);
    }

    if (!changes.isEmpty()) {
      updateProfileDates(dbSession, context);
    }

    // get all inherited profiles
    for (QProfileDto child : context.getChildProfiles()) {
      context.selectChild(child);
      changes.addAll(doActivateRecursively(dbSession, activation, context));
    }

    return changes;
  }

  private void handleUpdatedRuleActivation(RuleActivation activation, RuleActivationContext context, ActiveRuleChange change,
    ActiveRuleWrapper activeRule) {
    if (context.isCascading() && activeRule.get().getInheritance() == null) {
      // The rule is being propagated, but it was activated directly on this profile before
      change.setSeverity(activeRule.get().getSeverityString());
      change.setNewImpacts(activeRule.get().getImpacts());
      for (ActiveRuleParamDto activeParam : activeRule.getParams()) {
        change.setParameter(activeParam.getKey(), activeParam.getValue());
      }
      change.setInheritance(isSameAsParent(change, context) ? ActiveRuleInheritance.INHERITED : ActiveRuleInheritance.OVERRIDES);
    } else {
      applySeverityAndPrioritizedRuleAndParamToChange(activation, context, change);
      if (!context.isCascading() && context.getParentActiveRule() != null) {
        // override rule which is already declared on parents
        change.setInheritance(isSameAsParent(change, context) ? ActiveRuleInheritance.INHERITED : ActiveRuleInheritance.OVERRIDES);
      }
    }
  }

  private ActiveRuleChange handleNewRuleActivation(RuleActivation activation, RuleActivationContext context, RuleDto rule,
    ActiveRuleKey activeRuleKey) {
    ActiveRuleChange change = new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, activeRuleKey, rule);
    applySeverityAndPrioritizedRuleAndParamToChange(activation, context, change);
    if (context.isCascading() || context.getParentActiveRule() != null) {
      change.setInheritance(isSameAsParent(change, context) ? ActiveRuleInheritance.INHERITED : ActiveRuleInheritance.OVERRIDES);
    }
    return change;
  }

  private void updateProfileDates(DbSession dbSession, RuleActivationContext context) {
    RulesProfileDto ruleProfile = context.getRulesProfile();
    ruleProfile.setRulesUpdatedAtAsDate(new Date(context.getDate()));
    db.qualityProfileDao().update(dbSession, ruleProfile);

    if (userSession.isLoggedIn()) {
      context.getProfiles().forEach(p -> db.qualityProfileDao().update(dbSession,
        OrgQProfileDto.from(p).setUserUpdatedAt(context.getDate())));
    }
  }

  /**
   * Update severity, prioritizedRule and params
   */
  private void applySeverityAndPrioritizedRuleAndParamToChange(RuleActivation request, RuleActivationContext context,
    ActiveRuleChange change) {
    RuleWrapper rule = context.getRule();
    ActiveRuleWrapper activeRule = context.getActiveRule();
    ActiveRuleWrapper parentActiveRule = context.getParentActiveRule();

    if (request.isReset()) {
      applySeverityAndPrioritizedRuleAndParamsWhenResetRequested(change, rule, activeRule, parentActiveRule);
    } else if (context.getRulesProfile().isBuiltIn()) {
      applySeverityAndPrioritizedRuleAndParamsWhenBuiltInProfile(request, context, change, rule, activeRule);
    } else {
      applySeverityAndPrioritizedRuleAndParamsWhenNonBuiltInProfile(request, context, change, rule, activeRule, parentActiveRule);
    }
  }

  private void applySeverityAndPrioritizedRuleAndParamsWhenResetRequested(ActiveRuleChange change, RuleWrapper rule, @Nullable ActiveRuleWrapper currentActiveRule,
    @Nullable ActiveRuleWrapper parentActiveRule) {
    String severity = firstNonNull(
      parentActiveRule != null ? parentActiveRule.get().getSeverityString() : null,
      rule.get().getSeverityString());
    change.setSeverity(severity);

    Map<SoftwareQuality, Severity> oldImpactSeverities = currentActiveRule != null ? currentActiveRule.get().getImpacts() : Map.of();
    Map<SoftwareQuality, Severity> impactsSeverities = parentActiveRule != null ? parentActiveRule.get().getImpacts()
      : rule.get().getDefaultImpactsMap();
    change.setOldImpacts(oldImpactSeverities);
    change.setNewImpacts(impactsSeverities);

    change.setPrioritizedRule(parentActiveRule != null && parentActiveRule.get().isPrioritizedRule());

    for (RuleParamDto ruleParamDto : rule.getParams()) {
      String paramKey = ruleParamDto.getName();
      // load params from parent profile, else from default values
      String paramValue = firstNonNull(
        parentActiveRule != null ? parentActiveRule.getParamValue(paramKey) : null,
        rule.getParamDefaultValue(paramKey));

      change.setParameter(paramKey, validateParam(ruleParamDto, paramValue));
    }
  }

  private void applySeverityAndPrioritizedRuleAndParamsWhenBuiltInProfile(RuleActivation request, RuleActivationContext context,
    ActiveRuleChange change, RuleWrapper rule, @Nullable ActiveRuleWrapper currentActiveRule) {
    // for builtin quality profiles, the severity from profile, when null use the default severity of the rule
    SeverityConfiguration severityConfiguration = determineSeverityConfiguration(request, rule.get(), null, null);
    change.setSeverity(severityConfiguration.severity());

    Map<SoftwareQuality, Severity> oldImpactSeverities = currentActiveRule != null ? currentActiveRule.get().getImpacts() : Map.of();
    change.setOldImpacts(oldImpactSeverities);
    change.setNewImpacts(severityConfiguration.impacts());

    boolean prioritizedRule = TRUE.equals(request.isPrioritizedRule());
    change.setPrioritizedRule(prioritizedRule);

    for (RuleParamDto ruleParamDto : rule.getParams()) {
      String paramKey = ruleParamDto.getName();
      // use the value defined in the profile definition, else the rule default value
      String paramValue = firstNonNull(
        context.getRequestedParamValue(request, paramKey),
        rule.getParamDefaultValue(paramKey));
      change.setParameter(paramKey, validateParam(ruleParamDto, paramValue));
    }
  }

  private static SeverityConfiguration determineSeverityConfiguration(RuleActivation request, RuleDto ruleDto, @Nullable ActiveRuleWrapper activeRule,
    @Nullable ActiveRuleWrapper parentActiveRule) {
    String requestSeverity = request.getSeverity();
    if (requestSeverity != null) {
      Map<SoftwareQuality, Severity> impactSeverities = request.getImpactSeverities();
      if (impactSeverities != null && !impactSeverities.isEmpty()) {
        return new SeverityConfiguration(requestSeverity, impactSeverities);
      }
      // When standard severity is requested to be overridden, we translate it to the impact to override
      return new SeverityConfiguration(requestSeverity,
        QProfileImpactSeverityMapper.mapImpactSeverities(requestSeverity, ruleDto.getDefaultImpactsMap(), ruleDto.getEnumType()));
    } else if (!request.getImpactSeverities().isEmpty()) {
      // If an impact is request to be overridden, we translate it to the standard severity
      return new SeverityConfiguration(
        QProfileImpactSeverityMapper.mapSeverity(request.getImpactSeverities(), ruleDto.getEnumType(), ruleDto.getSeverityString()),
        request.getImpactSeverities());
    } else if (activeRule != null && activeRule.get().doesOverride()) {
      return new SeverityConfiguration(activeRule.get().getSeverityString(), activeRule.get().getImpacts());
    } else if (parentActiveRule != null) {
      return new SeverityConfiguration(parentActiveRule.get().getSeverityString(), parentActiveRule.get().getImpacts());
    } else if (activeRule != null) {
      return new SeverityConfiguration(activeRule.get().getSeverityString(), activeRule.get().getImpacts());
    } else {
      return new SeverityConfiguration(ruleDto.getSeverityString(), ruleDto.getDefaultImpactsMap());
    }
  }

  /**
   * 1. apply requested severity and param
   * 2. if rule activated and overridden - apply user value
   * 3. apply parent value
   * 4. apply defaults
   */
  private void applySeverityAndPrioritizedRuleAndParamsWhenNonBuiltInProfile(RuleActivation request, RuleActivationContext context,
    ActiveRuleChange change,
    RuleWrapper rule, @Nullable ActiveRuleWrapper currentActiveRule, @Nullable ActiveRuleWrapper parentActiveRule) {
    SeverityConfiguration severityConfiguration = getSeverityConfigurationForNonBuiltInProfile(request, rule, currentActiveRule, parentActiveRule);
    boolean prioritizedRule = getPrioritizedRuleForNonBuiltInProfile(request, currentActiveRule, parentActiveRule);
    change.setSeverity(severityConfiguration.severity());

    Map<SoftwareQuality, Severity> oldImpactSeverities = currentActiveRule != null ? currentActiveRule.get().getImpacts() : Map.of();
    change.setOldImpacts(oldImpactSeverities);
    change.setNewImpacts(severityConfiguration.impacts());
    change.setPrioritizedRule(prioritizedRule);

    for (RuleParamDto ruleParamDto : rule.getParams()) {
      String paramKey = ruleParamDto.getName();
      String parentValue = parentActiveRule != null ? parentActiveRule.getParamValue(paramKey) : null;
      String paramValue;
      if (context.hasRequestedParamValue(request, paramKey)) {
        // If the request contains the parameter then we're using either value from request, or parent value, or default value
        paramValue = firstNonNull(
          context.getRequestedParamValue(request, paramKey),
          parentValue,
          rule.getParamDefaultValue(paramKey));
      } else if (currentActiveRule != null) {
        // If the request doesn't contain the parameter, then we're using either user value from db, or parent value if rule inherited,
        // or default value
        paramValue = firstNonNull(
          currentActiveRule.get().doesOverride() ? currentActiveRule.getParamValue(paramKey) : null,
          parentValue == null ? currentActiveRule.getParamValue(paramKey) : parentValue,
          rule.getParamDefaultValue(paramKey));
      } else {
        paramValue = firstNonNull(
          parentValue,
          rule.getParamDefaultValue(paramKey));
      }
      change.setParameter(paramKey, validateParam(ruleParamDto, paramValue));
    }
  }

  private record SeverityConfiguration(@Nullable String severity, Map<SoftwareQuality, Severity> impacts) {
  }

  private static SeverityConfiguration getSeverityConfigurationForNonBuiltInProfile(RuleActivation request, RuleWrapper rule, @Nullable ActiveRuleWrapper activeRule,
    @Nullable ActiveRuleWrapper parentActiveRule) {
    return determineSeverityConfiguration(request, rule.get(), activeRule, parentActiveRule);
  }

  private static boolean getPrioritizedRuleForNonBuiltInProfile(RuleActivation request, @Nullable ActiveRuleWrapper activeRule,
    @Nullable ActiveRuleWrapper parentActiveRule) {
    boolean prioritizedRule;
    if (activeRule != null) {
      ActiveRuleDto activeRuleDto = activeRule.get();
      // load prioritizedRule from request, else keep existing one (if overridden), else from parent if rule inherited, else 'false'
      prioritizedRule = firstNonNull(
        request.isPrioritizedRule(),
        activeRuleDto.doesOverride() ? activeRuleDto.isPrioritizedRule() : null,
        parentActiveRule != null && parentActiveRule.get().isPrioritizedRule());
    } else {
      // load prioritizedRule from request, else from parent, else 'false'
      prioritizedRule = firstNonNull(
        request.isPrioritizedRule(),
        parentActiveRule != null && parentActiveRule.get().isPrioritizedRule());
    }
    return prioritizedRule;
  }

  private void persist(ActiveRuleChange change, RuleActivationContext context, DbSession dbSession) {
    ActiveRuleDto activeRule = null;
    if (change.getType() == ActiveRuleChange.Type.ACTIVATED) {
      activeRule = doInsert(change, context, dbSession);
    } else if (change.getType() == ActiveRuleChange.Type.DEACTIVATED) {
      ActiveRuleDao dao = db.activeRuleDao();
      activeRule = dao.delete(dbSession, change.getKey()).orElse(null);

    } else if (change.getType() == ActiveRuleChange.Type.UPDATED) {
      activeRule = doUpdate(change, context, dbSession);
    }
    change.setActiveRule(activeRule);

    QProfileChangeDto dto = change.toDto(userSession.getUuid());

    if (dto.getRuleChange() != null) {
      dto.getRuleChange().setUuid(uuidFactory.create());
      dto.getRuleChange().getRuleImpactChanges().forEach(e -> e.setRuleChangeUuid(dto.getRuleChange().getUuid()));
      db.ruleChangeDao().insert(dbSession, dto.getRuleChange());
    }

    dto.setSqVersion(sonarQubeVersion.toString());

    db.qProfileChangeDao().insert(dbSession, dto);
  }

  private ActiveRuleDto doInsert(ActiveRuleChange change, RuleActivationContext context, DbSession dbSession) {
    ActiveRuleDao dao = db.activeRuleDao();
    RuleWrapper rule = context.getRule();

    ActiveRuleDto activeRule = new ActiveRuleDto();
    activeRule.setProfileUuid(context.getRulesProfile().getUuid());
    activeRule.setRuleUuid(rule.get().getUuid());
    activeRule.setKey(ActiveRuleKey.of(context.getRulesProfile(), rule.get().getKey()));
    String severity = change.getSeverity();
    if (severity != null) {
      activeRule.setSeverity(severity);
    }
    activeRule.setImpacts(change.getNewImpacts());
    activeRule.setPrioritizedRule(TRUE.equals(change.isPrioritizedRule()));
    ActiveRuleInheritance inheritance = change.getInheritance();
    if (inheritance != null) {
      activeRule.setInheritance(inheritance.name());
    }
    activeRule.setUpdatedAt(system2.now());
    activeRule.setCreatedAt(system2.now());
    dao.insert(dbSession, activeRule);
    List<ActiveRuleParamDto> params = new ArrayList<>();
    for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
      if (param.getValue() != null) {
        ActiveRuleParamDto paramDto = ActiveRuleParamDto.createFor(rule.getParam(param.getKey()));
        paramDto.setValue(param.getValue());
        params.add(paramDto);
        dao.insertParam(dbSession, activeRule, paramDto);
      }
    }
    context.register(activeRule, params);
    return activeRule;
  }

  private ActiveRuleDto doUpdate(ActiveRuleChange change, RuleActivationContext context, DbSession dbSession) {
    ActiveRuleWrapper activeRule = context.getActiveRule();
    if (activeRule == null) {
      return null;
    }
    ActiveRuleDao dao = db.activeRuleDao();
    String severity = change.getSeverity();
    ActiveRuleDto ruleDto = activeRule.get();
    if (severity != null) {
      ruleDto.setSeverity(severity);
    }
    ruleDto.setImpacts(change.getNewImpacts());
    Boolean prioritizedRule = change.isPrioritizedRule();
    if (prioritizedRule != null) {
      ruleDto.setPrioritizedRule(prioritizedRule);
    }
    ActiveRuleInheritance inheritance = change.getInheritance();
    if (inheritance != null) {
      ruleDto.setInheritance(inheritance.name());
    }
    ruleDto.setUpdatedAt(system2.now());
    dao.update(dbSession, ruleDto);

    for (Map.Entry<String, String> param : change.getParameters().entrySet()) {
      ActiveRuleParamDto activeRuleParamDto = activeRule.getParam(param.getKey());
      if (activeRuleParamDto == null) {
        // did not exist
        if (param.getValue() != null) {
          activeRuleParamDto = ActiveRuleParamDto.createFor(context.getRule().getParam(param.getKey()));
          activeRuleParamDto.setValue(param.getValue());
          dao.insertParam(dbSession, ruleDto, activeRuleParamDto);
        }
      } else {
        if (param.getValue() != null) {
          activeRuleParamDto.setValue(param.getValue());
          dao.updateParam(dbSession, activeRuleParamDto);
        } else {
          dao.deleteParam(dbSession, activeRuleParamDto);
        }
      }
    }
    return ruleDto;
  }

  public List<ActiveRuleChange> deactivate(DbSession dbSession, RuleActivationContext context, String ruleUuid, boolean force) {
    context.reset(ruleUuid);
    return doDeactivateRecursively(dbSession, context, force);
  }

  private List<ActiveRuleChange> doDeactivateRecursively(DbSession dbSession, RuleActivationContext context, boolean force) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    ActiveRuleWrapper activeRule = context.getActiveRule();
    if (activeRule != null) {
      checkRequest(force || context.isCascading() || activeRule.get().getInheritance() == null || isAllowDisableInheritedRules(),
        "Cannot deactivate inherited rule '%s'", context.getRule().get().getKey());

      ActiveRuleChange change = new ActiveRuleChange(ActiveRuleChange.Type.DEACTIVATED, activeRule.get(), context.getRule().get());
      changes.add(change);
      persist(change, context, dbSession);
    }

    // get all inherited profiles (they are not built-in by design)
    context.getChildProfiles().forEach(child -> {
      context.selectChild(child);
      changes.addAll(doDeactivateRecursively(dbSession, context, force));
    });

    if (!changes.isEmpty()) {
      updateProfileDates(dbSession, context);
    }

    return changes;
  }

  private boolean isAllowDisableInheritedRules() {
    return configuration.getBoolean(CorePropertyDefinitions.ALLOW_DISABLE_INHERITED_RULES).orElse(true);
  }

  @CheckForNull
  private String validateParam(RuleParamDto ruleParam, @Nullable String value) {
    if (value != null) {
      RuleParamType ruleParamType = RuleParamType.parse(ruleParam.getType());
      if (ruleParamType.multiple()) {
        List<String> values = Splitter.on(",").splitToList(value);
        typeValidations.validate(values, ruleParamType.type(), ruleParamType.values());
      } else {
        typeValidations.validate(value, ruleParamType.type(), ruleParamType.values());
      }
    }
    return value;
  }

  public RuleActivationContext createContextForBuiltInProfile(DbSession dbSession, RulesProfileDto builtInProfile,
    Collection<String> ruleUuids) {
    checkArgument(builtInProfile.isBuiltIn(), "Rules profile with UUID %s is not built-in", builtInProfile.getUuid());

    RuleActivationContext.Builder builder = new RuleActivationContext.Builder();
    builder.setDescendantProfilesSupplier(createDescendantProfilesSupplier(dbSession));

    // load rules
    completeWithRules(dbSession, builder, ruleUuids);

    // load org profiles. Their parents are null by nature.
    List<QProfileDto> profiles = db.qualityProfileDao().selectQProfilesByRuleProfile(dbSession, builtInProfile);
    builder.setProfiles(profiles);
    builder.setBaseProfile(builtInProfile);

    // load active rules
    Collection<String> ruleProfileUuids = Stream
      .concat(Stream.of(builtInProfile.getUuid()), profiles.stream().map(QProfileDto::getRulesProfileUuid))
      .collect(Collectors.toSet());
    completeWithActiveRules(dbSession, builder, ruleUuids, ruleProfileUuids);
    return builder.build();
  }

  public RuleActivationContext createContextForUserProfile(DbSession dbSession, QProfileDto profile, Collection<String> ruleUuids) {
    checkArgument(!profile.isBuiltIn(), "Profile with UUID %s is built-in", profile.getKee());
    RuleActivationContext.Builder builder = new RuleActivationContext.Builder();
    builder.setDescendantProfilesSupplier(createDescendantProfilesSupplier(dbSession));

    // load rules
    completeWithRules(dbSession, builder, ruleUuids);

    // load profiles
    List<QProfileDto> profiles = new ArrayList<>();
    profiles.add(profile);
    if (profile.getParentKee() != null) {
      profiles.add(db.qualityProfileDao().selectByUuid(dbSession, profile.getParentKee()));
    }
    builder.setProfiles(profiles);
    builder.setBaseProfile(RulesProfileDto.from(profile));

    // load active rules
    Collection<String> ruleProfileUuids = profiles.stream()
      .map(QProfileDto::getRulesProfileUuid)
      .collect(Collectors.toSet());
    completeWithActiveRules(dbSession, builder, ruleUuids, ruleProfileUuids);

    return builder.build();
  }

  DescendantProfilesSupplier createDescendantProfilesSupplier(DbSession dbSession) {
    return (parents, ruleUuids) -> {
      Collection<QProfileDto> profiles = db.qualityProfileDao().selectDescendants(dbSession, parents);
      Set<String> ruleProfileUuids = profiles.stream()
        .map(QProfileDto::getRulesProfileUuid)
        .collect(Collectors.toSet());
      Collection<ActiveRuleDto> activeRules = db.activeRuleDao().selectByRulesAndRuleProfileUuids(dbSession, ruleUuids, ruleProfileUuids);
      List<String> activeRuleUuids = activeRules.stream().map(ActiveRuleDto::getUuid).toList();
      List<ActiveRuleParamDto> activeRuleParams = db.activeRuleDao().selectParamsByActiveRuleUuids(dbSession, activeRuleUuids);
      return new DescendantProfilesSupplier.Result(profiles, activeRules, activeRuleParams);
    };
  }

  private void completeWithRules(DbSession dbSession, RuleActivationContext.Builder builder, Collection<String> ruleUuids) {
    List<RuleDto> rules = db.ruleDao().selectByUuids(dbSession, ruleUuids);
    builder.setRules(rules);
    builder.setRuleParams(db.ruleDao().selectRuleParamsByRuleUuids(dbSession, ruleUuids));
  }

  private void completeWithActiveRules(DbSession dbSession, RuleActivationContext.Builder builder, Collection<String> ruleUuids,
    Collection<String> ruleProfileUuids) {
    Collection<ActiveRuleDto> activeRules = db.activeRuleDao().selectByRulesAndRuleProfileUuids(dbSession, ruleUuids, ruleProfileUuids);
    builder.setActiveRules(activeRules);
    List<String> activeRuleUuids = activeRules.stream().map(ActiveRuleDto::getUuid).toList();
    builder.setActiveRuleParams(db.activeRuleDao().selectParamsByActiveRuleUuids(dbSession, activeRuleUuids));
  }

  private static boolean isSame(ActiveRuleChange change, ActiveRuleWrapper activeRule) {
    ActiveRuleInheritance inheritance = change.getInheritance();
    if (inheritance != null && !inheritance.name().equals(activeRule.get().getInheritance())) {
      return false;
    }
    String severity = change.getSeverity();
    if (severity != null && !severity.equals(activeRule.get().getSeverityString())) {
      return false;
    }
    Map<SoftwareQuality, Severity> impactSeverities = change.getNewImpacts();
    if (!impactSeverities.equals(activeRule.get().getImpacts())) {
      return false;
    }
    Boolean prioritizedRule = change.isPrioritizedRule();
    if (prioritizedRule != null && prioritizedRule != activeRule.get().isPrioritizedRule()) {
      return false;
    }
    for (Map.Entry<String, String> changeParam : change.getParameters().entrySet()) {
      String activeParamValue = activeRule.getParamValue(changeParam.getKey());
      if (changeParam.getValue() == null && activeParamValue != null) {
        return false;
      }
      if (changeParam.getValue() != null && (activeParamValue == null || !StringUtils.equals(changeParam.getValue(), activeParamValue))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isSameAsParent(ActiveRuleChange change, RuleActivationContext context) {
    ActiveRuleWrapper parentActiveRule = context.getParentActiveRule();
    if (parentActiveRule == null) {
      return false;
    }
    if (!StringUtils.equals(change.getSeverity(), parentActiveRule.get().getSeverityString())) {
      return false;
    }
    if (!change.getNewImpacts().equals(parentActiveRule.get().getImpacts())) {
      return false;
    }
    if (change.isPrioritizedRule() != null && !Objects.equals(change.isPrioritizedRule(), parentActiveRule.get().isPrioritizedRule())) {
      return false;
    }
    for (Map.Entry<String, String> entry : change.getParameters().entrySet()) {
      if (entry.getValue() != null && !entry.getValue().equals(parentActiveRule.getParamValue(entry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  @CheckForNull
  private static String firstNonNull(String... strings) {
    for (String s : strings) {
      if (s != null) {
        return s;
      }
    }
    return null;
  }

  private static boolean firstNonNull(Boolean... booleans) {
    for (Boolean b : booleans) {
      if (b != null) {
        return b;
      }
    }
    return false;
  }
}

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

package org.sonar.server.qualityprofile;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.collect.Iterables;
import org.sonar.api.PropertyType;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileActiveRuleOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final QualityProfileDao profileDao;
  private final RuleRegistry ruleRegistry;
  private final ProfilesManager profilesManager;

  private final System2 system;

  public QProfileActiveRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, QualityProfileDao profileDao, RuleRegistry ruleRegistry,
                                      ProfilesManager profilesManager) {
    this(myBatis, activeRuleDao, ruleDao, profileDao, ruleRegistry, profilesManager, System2.INSTANCE);
  }

  @VisibleForTesting
  QProfileActiveRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, QualityProfileDao profileDao, RuleRegistry ruleRegistry,
                               ProfilesManager profilesManager, System2 system) {
    this.myBatis = myBatis;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.profileDao = profileDao;
    this.ruleRegistry = ruleRegistry;
    this.profilesManager = profilesManager;
    this.system = system;
  }

  public void activateRule(int profileId, int ruleId, String severity, UserSession userSession) {
    validatePermission(userSession);
    validateSeverity(severity);

    SqlSession session = myBatis.openSession();
    try {
      QualityProfileDto profile = findProfileNotNull(profileId, session);
      RuleDto rule = findRuleNotNull(ruleId, session);
      ActiveRuleDto activeRule = findActiveRule(profileId, ruleId, session);
      if (activeRule == null) {
        createActiveRule(profile.getId(), rule.getId(), severity, userSession, session);
      } else {
        updateSeverity(activeRule, severity, userSession, session);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private ActiveRuleDto createActiveRule(int profileId, int ruleId, String severity, UserSession userSession, SqlSession session) {
    ActiveRuleDto activeRule = new ActiveRuleDto()
      .setProfileId(profileId)
      .setRuleId(ruleId)
      .setSeverity(getSeverityOrdinal(severity));
    activeRuleDao.insert(activeRule, session);

    List<RuleParamDto> ruleParams = ruleDao.selectParameters(ruleId, session);
    List<ActiveRuleParamDto> activeRuleParams = newArrayList();
    for (RuleParamDto ruleParam : ruleParams) {
      ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto()
        .setActiveRuleId(activeRule.getId())
        .setRulesParameterId(ruleParam.getId())
        .setKey(ruleParam.getName())
        .setValue(ruleParam.getDefaultValue());
      activeRuleParams.add(activeRuleParam);
      activeRuleDao.insert(activeRuleParam, session);
    }
    session.commit();
    ProfilesManager.RuleInheritanceActions actions = profilesManager.activated(profileId, activeRule.getId(), userSession.name());
    reindexInheritanceResult(actions, session);
    return activeRule;
  }

  private void updateSeverity(ActiveRuleDto activeRule, String newSeverity, UserSession userSession, SqlSession session) {
    Integer oldSeverity = activeRule.getSeverity();
    activeRule.setSeverity(getSeverityOrdinal(newSeverity));
    activeRuleDao.update(activeRule, session);
    session.commit();

    notifySeverityChanged(activeRule, newSeverity, getSeverityFromOrdinal(oldSeverity), session, userSession);
  }

  public void activateRules(int profileId, List<Integer> ruleIdsToActivate, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      for (Integer ruleId : ruleIdsToActivate) {
        RuleDto rule = findRuleNotNull(ruleId, session);
        createActiveRule(profileId, ruleId, getSeverityFromOrdinal(rule.getSeverity()), userSession, session);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deactivateRule(int profileId, int ruleId, UserSession userSession) {
    validatePermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      ActiveRuleDto activeRule = findActiveRule(profileId, ruleId, session);
      deactivateRule(activeRule, userSession, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void deactivateRule(ActiveRuleDto activeRule, UserSession userSession, SqlSession session) {
    ProfilesManager.RuleInheritanceActions actions = profilesManager.deactivated(activeRule.getProfileId(), activeRule.getId(), userSession.name());

    activeRuleDao.deleteParameters(activeRule.getId(), session);
    activeRuleDao.delete(activeRule.getId(), session);
    actions.addToDelete(activeRule.getId());
    session.commit();

    reindexInheritanceResult(actions, session);
  }

  public void deactivateRules(int profileId, List<Integer> activeRuleIdsToDeactivate, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      for (int activeRuleId : activeRuleIdsToDeactivate) {
        ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId, session);
        deactivateRule(activeRule, userSession, session);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateActiveRuleParam(int activeRuleId, String key, @Nullable String value, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      String sanitizedValue = Strings.emptyToNull(value);
      ActiveRuleParamDto activeRuleParam = findActiveRuleParam(activeRuleId, key, session);
      ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId, session);
      if (activeRuleParam == null && sanitizedValue != null) {
        createActiveRuleParam(activeRule, key, value, userSession, session);
      } else if (activeRuleParam != null && sanitizedValue == null) {
        deleteActiveRuleParam(activeRule, activeRuleParam, userSession, session);
      } else if (activeRuleParam != null) {
        updateActiveRuleParam(activeRule, activeRuleParam, value, userSession, session);
      }
      // If no active rule param and no value -> do nothing

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void createActiveRuleParam(ActiveRuleDto activeRule, String key, String value, UserSession userSession, SqlSession session) {
    RuleParamDto ruleParam = findRuleParamNotNull(activeRule.getRulId(), key, session);
    validateParam(ruleParam.getType(), value);
    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setActiveRuleId(activeRule.getId()).setKey(key).setValue(value).setRulesParameterId(ruleParam.getId());
    activeRuleDao.insert(activeRuleParam, session);
    session.commit();

    ProfilesManager.RuleInheritanceActions actions = profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), key, null, value, userSession.name());
    reindexInheritanceResult(actions, session);
  }

  private void deleteActiveRuleParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, UserSession userSession, SqlSession session) {
    activeRuleDao.deleteParameter(activeRuleParam.getId(), session);
    session.commit();
    notifyParamsDeleted(activeRule, newArrayList(activeRuleParam), session, userSession);
  }

  private void updateActiveRuleParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, String value, UserSession userSession, SqlSession session) {
    RuleParamDto ruleParam = findRuleParamNotNull(activeRule.getRulId(), activeRuleParam.getKey(), session);
    validateParam(ruleParam.getType(), value);

    String sanitizedValue = Strings.emptyToNull(value);
    String oldValue = activeRuleParam.getValue();
    activeRuleParam.setValue(sanitizedValue);
    activeRuleDao.update(activeRuleParam, session);
    session.commit();

    ProfilesManager.RuleInheritanceActions actions = profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), activeRuleParam.getKey(), oldValue,
      sanitizedValue, getLoggedName(userSession));
    reindexInheritanceResult(actions, session);
  }

  public void revertActiveRule(int activeRuleId, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      ActiveRuleDto activeRule = findActiveRuleNotNull(activeRuleId, session);
      if (activeRule.doesOverride()) {
        revertActiveRule(activeRule, userSession, session);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void revertActiveRule(ActiveRuleDto activeRule, UserSession userSession, SqlSession session) {
    ProfilesManager.RuleInheritanceActions actions = new ProfilesManager.RuleInheritanceActions();
    ActiveRuleDto parent = getParent(activeRule, session);

    List<ActiveRuleParamDto> newParams = restoreActiveParametersFromActiveRuleParent(activeRule, parent, actions, userSession, session);
    restoreSeverityFromActiveRuleParent(activeRule, parent, actions, userSession, session);
    reindexInheritanceResult(actions, session);

    // Update inheritance
    activeRule.setInheritance(ActiveRuleDto.INHERITED);
    activeRuleDao.update(activeRule, session);
    session.commit();
    reindexActiveRule(activeRule, newParams);
  }

  private ActiveRuleDto getParent(ActiveRuleDto activeRule, SqlSession session) {
    Integer parentId = activeRule.getParentId();
    if (parentId != null) {
      ActiveRuleDto parent = activeRuleDao.selectById(parentId, session);
      if (parent != null) {
        return parent;
      }
    }
    throw new IllegalStateException("Can't find parent of active rule : " + activeRule.getId());
  }

  private List<ActiveRuleParamDto> restoreActiveParametersFromActiveRuleParent(ActiveRuleDto activeRule, ActiveRuleDto parent, ProfilesManager.RuleInheritanceActions actions,
                                                                               UserSession userSession, SqlSession session) {
    // Restore all parameters from parent
    List<ActiveRuleParamDto> parentParams = activeRuleDao.selectParamsByActiveRuleId(parent.getId(), session);
    List<ActiveRuleParamDto> activeRuleParams = activeRuleDao.selectParamsByActiveRuleId(activeRule.getId(), session);
    List<ActiveRuleParamDto> newParams = newArrayList();
    List<String> paramKeys = newArrayList();
    for (ActiveRuleParamDto param : activeRuleParams) {
      final String key = param.getKey();
      ActiveRuleParamDto parentParam = Iterables.find(parentParams, new Predicate<ActiveRuleParamDto>() {
        @Override
        public boolean apply(ActiveRuleParamDto activeRuleParamDto) {
          return activeRuleParamDto.getKey().equals(key);
        }
      }, null);
      if (parentParam != null && !Strings.isNullOrEmpty(parentParam.getValue())) {
        String oldValue = param.getValue();
        String newValue = parentParam.getValue();
        param.setValue(newValue);
        activeRuleDao.update(param, session);
        session.commit();
        newParams.add(param);
        actions.add(profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), key, oldValue, newValue, getLoggedName(userSession)));
      } else {
        activeRuleDao.deleteParameter(param.getId(), session);
        session.commit();
        actions.add(profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), key, param.getValue(), null, userSession.name()));
      }
      paramKeys.add(key);
    }
    for (ActiveRuleParamDto parentParam : parentParams) {
      if (!paramKeys.contains(parentParam.getKey())) {
        ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setActiveRuleId(activeRule.getId())
          .setKey(parentParam.getKey()).setValue(parentParam.getValue()).setRulesParameterId(parentParam.getRulesParameterId());
        activeRuleDao.insert(activeRuleParam, session);
        session.commit();
        newParams.add(activeRuleParam);
        actions.add(profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), parentParam.getKey(), null, parentParam.getValue(), userSession.name()));
      }
    }
    return newParams;
  }

  private void restoreSeverityFromActiveRuleParent(ActiveRuleDto activeRule, ActiveRuleDto parent, ProfilesManager.RuleInheritanceActions actions, UserSession userSession, SqlSession session) {
    Integer oldSeverity = activeRule.getSeverity();
    Integer newSeverity = parent.getSeverity();
    if (!oldSeverity.equals(newSeverity)) {
      activeRule.setSeverity(newSeverity);
      activeRuleDao.update(activeRule, session);
      session.commit();
      actions.add(profilesManager.ruleSeverityChanged(activeRule.getProfileId(), activeRule.getId(),
        RulePriority.valueOf(getSeverityFromOrdinal(oldSeverity)), RulePriority.valueOf(getSeverityFromOrdinal(newSeverity)), userSession.name()));
    }
  }

  public void updateActiveRuleNote(ActiveRuleDto activeRule, String note, UserSession userSession) {
    validatePermission(userSession);
    Date now = new Date(system.now());
    SqlSession session = myBatis.openSession();
    try {
      if (activeRule.getNoteData() == null) {
        activeRule.setNoteCreatedAt(now);
        activeRule.setNoteUserLogin(userSession.login());
      }
      activeRule.setNoteUpdatedAt(now);
      activeRule.setNoteData(note);
      activeRuleDao.update(activeRule, session);
      session.commit();

      reindexActiveRule(activeRule, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteActiveRuleNote(ActiveRuleDto activeRule, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      activeRule.setNoteData(null);
      activeRule.setNoteUserLogin(null);
      activeRule.setNoteCreatedAt(null);
      activeRule.setNoteUpdatedAt(null);
      activeRuleDao.update(activeRule);
      session.commit();

      reindexActiveRule(activeRule, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void notifyParamsDeleted(ActiveRuleDto activeRule, List<ActiveRuleParamDto> params, SqlSession session, UserSession userSession) {
    ProfilesManager.RuleInheritanceActions actions = new ProfilesManager.RuleInheritanceActions();
    for (ActiveRuleParamDto activeRuleParam : params) {
      actions.add(profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), activeRuleParam.getKey(), activeRuleParam.getValue(),
        null, userSession.name()));
    }
    reindexInheritanceResult(actions, session);
  }

  private void notifySeverityChanged(ActiveRuleDto activeRule, String newSeverity, String oldSeverity, SqlSession session, UserSession userSession) {
    ProfilesManager.RuleInheritanceActions actions = profilesManager.ruleSeverityChanged(activeRule.getProfileId(), activeRule.getId(),
      RulePriority.valueOf(oldSeverity), RulePriority.valueOf(newSeverity),
      userSession.name());
    reindexInheritanceResult(actions, session);
  }

  private void reindexInheritanceResult(ProfilesManager.RuleInheritanceActions actions, SqlSession session) {
    ruleRegistry.deleteActiveRules(actions.idsToDelete());
    ruleRegistry.bulkIndexActiveRules(actions.idsToIndex(), session);
  }

  private void reindexActiveRule(ActiveRuleDto activeRuleDto, SqlSession session) {
    reindexActiveRule(activeRuleDto, activeRuleDao.selectParamsByActiveRuleId(activeRuleDto.getId(), session));
  }

  private void reindexActiveRule(ActiveRuleDto activeRuleDto, List<ActiveRuleParamDto> params) {
    ruleRegistry.save(activeRuleDto, params);
  }

  private void validatePermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private void validateSeverity(String severity) {
    if (!Severity.ALL.contains(severity)) {
      throw new BadRequestException("The severity is not valid");
    }
  }

  @VisibleForTesting
  void validateParam(String type, String value) {
    if (type.equals(PropertyType.INTEGER.name()) && !NumberUtils.isDigits(value)) {
      throw new BadRequestException(String.format("Value '%s' must be an integer.", value));
    } else if (type.equals(PropertyType.BOOLEAN.name()) && !Boolean.parseBoolean(value)) {
      throw new BadRequestException(String.format("Value '%s' must be one of : true,false.", value));
    }
  }

  private String getLoggedName(UserSession userSession) {
    String name = userSession.name();
    if (Strings.isNullOrEmpty(name)) {
      throw new BadRequestException("User name can't be null");
    }
    return name;
  }

  private RuleParamDto findRuleParamNotNull(Integer ruleId, String key, SqlSession session) {
    RuleParamDto ruleParam = ruleDao.selectParamByRuleAndKey(ruleId, key, session);
    if (ruleParam == null) {
      throw new IllegalArgumentException("No rule param found");
    }
    return ruleParam;
  }

  private QualityProfileDto findProfileNotNull(int profileId, SqlSession session) {
    QualityProfileDto profile = profileDao.selectById(profileId, session);
    QProfileValidations.checkProfileIsNotNull(profile);
    return profile;
  }

  private RuleDto findRuleNotNull(int ruleId, SqlSession session) {
    RuleDto rule = ruleDao.selectById(ruleId, session);
    QProfileValidations.checkRuleIsNotNull(rule);
    return rule;
  }

  @CheckForNull
  private ActiveRuleDto findActiveRule(int profileId, int ruleId, SqlSession session) {
    return activeRuleDao.selectByProfileAndRule(profileId, ruleId, session);
  }

  private ActiveRuleDto findActiveRuleNotNull(int activeRuleId, SqlSession session) {
    ActiveRuleDto activeRule = activeRuleDao.selectById(activeRuleId, session);
    if (activeRule == null) {
      throw new NotFoundException("This active rule does not exists.");
    }
    return activeRule;
  }

  @CheckForNull
  private ActiveRuleParamDto findActiveRuleParam(int activeRuleId, String key, SqlSession session) {
    return activeRuleDao.selectParamByActiveRuleAndKey(activeRuleId, key, session);
  }

  private static String getSeverityFromOrdinal(int ordinal) {
    return Severity.ALL.get(ordinal);
  }

  private static int getSeverityOrdinal(String severity) {
    return Severity.ALL.indexOf(severity);
  }

}

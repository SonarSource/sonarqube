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
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.ProfileRules;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileActiveRuleOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final QProfileLookup profileLookup;
  private final RuleRegistry ruleRegistry;
  private final ProfilesManager profilesManager;
  private final ProfileRules rulesLookup;

  private final System2 system;

  public QProfileActiveRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, QProfileLookup profileLookup, RuleRegistry ruleRegistry,
                                      ProfilesManager profilesManager, ProfileRules rulesLookup) {
    this(myBatis, activeRuleDao, ruleDao, profileLookup, ruleRegistry, profilesManager, rulesLookup, System2.INSTANCE);
  }

  @VisibleForTesting
  QProfileActiveRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, QProfileLookup profileLookup, RuleRegistry ruleRegistry,
                               ProfilesManager profilesManager, ProfileRules rulesLookup, System2 system) {
    this.myBatis = myBatis;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.profileLookup = profileLookup;
    this.ruleRegistry = ruleRegistry;
    this.profilesManager = profilesManager;
    this.rulesLookup = rulesLookup;
    this.system = system;
  }

//  public ProfileRuleChanged activateRule(int profileId, int ruleId, String severity) {
//    QProfile profile = profileLookup.profile(profileId);
//    RuleDto rule = findRuleNotNull(ruleId);
//    ActiveRuleDto activeRule = findActiveRule(qualityProfile, rule);
//    if (activeRule == null) {
//      activeRule = activeRuleOperations.createActiveRule(qualityProfile, rule, severity, UserSession.get());
//    } else {
//      activeRuleOperations.updateSeverity(activeRule, severity, UserSession.get());
//    }
//    return activeRuleChanged(qualityProfile, activeRule);
//  }

  public ActiveRuleDto createActiveRule(QualityProfileDto qualityProfile, RuleDto rule, String severity, UserSession userSession) {
    validatePermission(userSession);
    validateSeverity(severity);
    SqlSession session = myBatis.openSession();
    try {
      ActiveRuleDto activeRule = new ActiveRuleDto()
        .setProfileId(qualityProfile.getId())
        .setRuleId(rule.getId())
        .setSeverity(getSeverityOrdinal(severity));
      activeRuleDao.insert(activeRule, session);

      List<RuleParamDto> ruleParams = ruleDao.selectParameters(rule.getId(), session);
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

      ProfilesManager.RuleInheritanceActions actions = profilesManager.activated(qualityProfile.getId(), activeRule.getId(), userSession.name());
      reindexInheritanceResult(actions, session);

      return activeRule;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateSeverity(ActiveRuleDto activeRule, String newSeverity, UserSession userSession) {
    validatePermission(userSession);
    validateSeverity(newSeverity);
    SqlSession session = myBatis.openSession();
    try {
      Integer oldSeverity = activeRule.getSeverity();
      activeRule.setSeverity(getSeverityOrdinal(newSeverity));
      activeRuleDao.update(activeRule, session);
      session.commit();

      notifySeverityChanged(activeRule, newSeverity, getSeverityFromOrdinal(oldSeverity), session, userSession);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deactivateRule(ActiveRuleDto activeRule, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      ProfilesManager.RuleInheritanceActions actions = profilesManager.deactivated(activeRule.getProfileId(), activeRule.getId(), userSession.name());

      activeRuleDao.deleteParameters(activeRule.getId(), session);
      activeRuleDao.delete(activeRule.getId(), session);
      actions.addToDelete(activeRule.getId());
      session.commit();

      reindexInheritanceResult(actions, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void createActiveRuleParam(ActiveRuleDto activeRule, String key, String value, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      RuleParamDto ruleParam = findRuleParamNotNull(activeRule.getRulId(), key, session);
      validateParam(ruleParam.getType(), value);
      ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setActiveRuleId(activeRule.getId()).setKey(key).setValue(value).setRulesParameterId(ruleParam.getId());
      activeRuleDao.insert(activeRuleParam, session);
      session.commit();

      ProfilesManager.RuleInheritanceActions actions = profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), key, null, value, userSession.name());
      reindexInheritanceResult(actions, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteActiveRuleParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      activeRuleDao.deleteParameter(activeRuleParam.getId(), session);
      session.commit();

      notifyParamsDeleted(activeRule, newArrayList(activeRuleParam), session, userSession);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateActiveRuleParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, String value, UserSession userSession) {
    validatePermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
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
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void revertActiveRule(ActiveRuleDto activeRule, UserSession userSession) {
    validatePermission(userSession);

    if (activeRule.doesOverride()) {
      SqlSession session = myBatis.openSession();
      try {
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
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
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

  private static String getSeverityFromOrdinal(int ordinal) {
    return Severity.ALL.get(ordinal);
  }

  private static int getSeverityOrdinal(String severity) {
    return Severity.ALL.indexOf(severity);
  }

  public static class ProfileRuleChanged {

    private QProfile profile;
    private QProfile parentProfile;
    private QProfileRule rule;

    public ProfileRuleChanged(QProfile profile, @Nullable QProfile parentProfile, QProfileRule rule) {
      this.profile = profile;
      this.parentProfile = parentProfile;
      this.rule = rule;
    }

    public QProfile profile() {
      return profile;
    }

    public QProfile parentProfile() {
      return parentProfile;
    }

    public QProfileRule rule() {
      return rule;
    }
  }

}

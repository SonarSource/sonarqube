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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.PropertyType;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import java.util.Date;
import java.util.List;

public class QProfileActiveRuleOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final PreviewCache dryRunCache;
  private final RuleRegistry ruleRegistry;
  private final ProfilesManager profilesManager;

  private final System2 system;

  public QProfileActiveRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, PreviewCache dryRunCache, RuleRegistry ruleRegistry, ProfilesManager profilesManager) {
    this(myBatis, activeRuleDao, ruleDao, dryRunCache, ruleRegistry,
      profilesManager, System2.INSTANCE);
  }

  @VisibleForTesting
  QProfileActiveRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, PreviewCache dryRunCache, RuleRegistry ruleRegistry,
                               ProfilesManager profilesManager, System2 system) {
    this.myBatis = myBatis;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.dryRunCache = dryRunCache;
    this.ruleRegistry = ruleRegistry;
    this.profilesManager = profilesManager;
    this.system = system;
  }

  public ActiveRuleDto createActiveRule(QualityProfileDto qualityProfile, RuleDto rule, String severity, UserSession userSession) {
    checkPermission(userSession);
    checkSeverity(severity);
    SqlSession session = myBatis.openSession();
    try {
      ActiveRuleDto activeRule = new ActiveRuleDto()
        .setProfileId(qualityProfile.getId())
        .setRuleId(rule.getId())
        .setSeverity(Severity.ordinal(severity));
      activeRuleDao.insert(activeRule, session);

      List<RuleParamDto> ruleParams = ruleDao.selectParameters(rule.getId(), session);
      List<ActiveRuleParamDto> activeRuleParams = Lists.newArrayList();
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

      RuleInheritanceActions actions = profilesManager.activated(qualityProfile.getId(), activeRule.getId(), userSession.name());
      reindexInheritanceResult(actions, session);

      return activeRule;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateSeverity(ActiveRuleDto activeRule, String newSeverity, UserSession userSession) {
    checkPermission(userSession);
    checkSeverity(newSeverity);
    SqlSession session = myBatis.openSession();
    try {
      Integer oldSeverity = activeRule.getSeverity();
      activeRule.setSeverity(Severity.ordinal(newSeverity));
      activeRuleDao.update(activeRule, session);
      session.commit();

      RuleInheritanceActions actions = profilesManager.ruleSeverityChanged(activeRule.getProfileId(), activeRule.getId(),
        RulePriority.valueOfInt(oldSeverity), RulePriority.valueOf(newSeverity),
        userSession.name());
      reindexInheritanceResult(actions, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deactivateRule(ActiveRuleDto activeRule, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      RuleInheritanceActions actions = profilesManager.deactivated(activeRule.getProfileId(), activeRule.getId(), userSession.name());

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
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      RuleParamDto ruleParam = findRuleParamNotNull(activeRule.getRulId(), key, session);
      validateParam(ruleParam.getType(), value);
      ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setActiveRuleId(activeRule.getId()).setKey(key).setValue(value).setRulesParameterId(ruleParam.getId());
      activeRuleDao.insert(activeRuleParam, session);
      session.commit();

      RuleInheritanceActions actions = profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), key, null, value, userSession.name());
      reindexInheritanceResult(actions, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteActiveRuleParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      activeRuleDao.deleteParameter(activeRuleParam.getId(), session);
      session.commit();

      RuleInheritanceActions actions = profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), activeRuleParam.getKey(), activeRuleParam.getValue(),
        null, userSession.name());
      reindexInheritanceResult(actions, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateActiveRuleParam(ActiveRuleDto activeRule, ActiveRuleParamDto activeRuleParam, String value, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      RuleParamDto ruleParam = findRuleParamNotNull(activeRule.getRulId(), activeRuleParam.getKey(), session);
      validateParam(ruleParam.getType(), value);

      String sanitizedValue = Strings.emptyToNull(value);
      String oldValue = activeRuleParam.getValue();
      activeRuleParam.setValue(sanitizedValue);
      activeRuleDao.update(activeRuleParam, session);
      session.commit();

      RuleInheritanceActions actions = profilesManager.ruleParamChanged(activeRule.getProfileId(), activeRule.getId(), activeRuleParam.getKey(), oldValue,
        sanitizedValue, getLoggedName(userSession));
      reindexInheritanceResult(actions, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateActiveRuleNote(ActiveRuleDto activeRule, String note, UserSession userSession) {
    checkPermission(userSession);
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
    checkPermission(userSession);

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

  private void reindexInheritanceResult(RuleInheritanceActions actions, SqlSession session) {
    ruleRegistry.deleteActiveRules(actions.idsToDelete());
    List<ActiveRuleDto> activeRules = activeRuleDao.selectByIds(actions.idsToIndex(), session);
    Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
    for (ActiveRuleParamDto param : activeRuleDao.selectParamsByActiveRuleIds(actions.idsToIndex(), session)) {
      paramsByActiveRule.put(param.getActiveRuleId(), param);
    }
    ruleRegistry.bulkIndexActiveRules(activeRules, paramsByActiveRule);
  }

  private void reindexActiveRule(ActiveRuleDto activeRuleDto, SqlSession session) {
    ruleRegistry.save(activeRuleDto, activeRuleDao.selectParamsByActiveRuleId(activeRuleDto.getId(), session));
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private String getLoggedName(UserSession userSession) {
    String name = userSession.name();
    if (Strings.isNullOrEmpty(name)) {
      throw new BadRequestException("User name can't be null");
    }
    return name;
  }

  private void checkSeverity(String severity) {
    if (!Severity.ALL.contains(severity)) {
      throw new BadRequestException("The severity is not valid");
    }
  }

  private RuleParamDto findRuleParamNotNull(Integer ruleId, String key, SqlSession session) {
    RuleParamDto ruleParam = ruleDao.selectParamByRuleAndKey(ruleId, key, session);
    if (ruleParam == null) {
      throw new IllegalArgumentException("No rule param found");
    }
    return ruleParam;
  }

  private void validateParam(String type, String value) {
    if (type.equals(PropertyType.INTEGER.name()) && !NumberUtils.isDigits(value)) {
      throw new BadRequestException(String.format("Value '%s' must be an integer.", value));
    }
  }

}

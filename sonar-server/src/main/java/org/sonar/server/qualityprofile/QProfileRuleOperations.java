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
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.System2;
import org.sonar.check.Cardinality;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileRuleOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final RuleRegistry ruleRegistry;

  private final System2 system;

  public QProfileRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, RuleRegistry ruleRegistry) {
    this(myBatis, activeRuleDao, ruleDao, ruleRegistry, System2.INSTANCE);
  }

  @VisibleForTesting
  QProfileRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, RuleRegistry ruleRegistry, System2 system) {
    this.myBatis = myBatis;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.ruleRegistry = ruleRegistry;
    this.system = system;
  }

  public void updateRuleNote(RuleDto rule, String note, UserSession userSession) {
    checkPermission(userSession);
    Date now = new Date(system.now());

    SqlSession session = myBatis.openSession();
    try {
      if (rule.getNoteData() == null) {
        rule.setNoteCreatedAt(now);
        rule.setNoteUserLogin(getLoggedLogin(userSession));
      }
      rule.setNoteUpdatedAt(now);
      rule.setNoteData(note);
      ruleDao.update(rule);
      session.commit();

      reindexRule(rule, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteRuleNote(RuleDto rule, UserSession userSession) {
    checkPermission(userSession);

    SqlSession session = myBatis.openSession();
    try {
      rule.setNoteData(null);
      rule.setNoteUserLogin(null);
      rule.setNoteCreatedAt(null);
      rule.setNoteUpdatedAt(null);
      ruleDao.update(rule);
      session.commit();

      reindexRule(rule, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public RuleDto createRule(RuleDto templateRule, String name, String severity, String description, Map<String, String> paramsByKey,
                            UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      RuleDto rule = new RuleDto()
        .setParentId(templateRule.getId())
        .setName(name)
        .setDescription(description)
        .setSeverity(Severity.ordinal(severity))
        .setRepositoryKey(templateRule.getRepositoryKey())
        .setConfigKey(templateRule.getConfigKey())
        .setRuleKey(templateRule.getRuleKey() + "_" + system.now())
        .setCardinality(Cardinality.SINGLE)
        .setStatus(Rule.STATUS_READY)
        .setLanguage(templateRule.getLanguage())
        .setCreatedAt(new Date(system.now()))
        .setUpdatedAt(new Date(system.now()));
      ruleDao.insert(rule, session);

      List<RuleParamDto> templateRuleParams = ruleDao.selectParameters(templateRule.getId(), session);
      List<RuleParamDto> ruleParams = newArrayList();
      for (RuleParamDto templateRuleParam : templateRuleParams) {
        String key = templateRuleParam.getName();
        String value = paramsByKey.get(key);

        RuleParamDto param = new RuleParamDto()
          .setRuleId(rule.getId())
          .setName(key)
          .setDescription(templateRuleParam.getDescription())
          .setType(templateRuleParam.getType())
          .setDefaultValue(Strings.emptyToNull(value));
        ruleDao.insert(param, session);
        ruleParams.add(param);
      }
      session.commit();
      reindexRule(rule, ruleParams);
      return rule;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateRule(RuleDto rule, String name, String severity, String description, Map<String, String> paramsByKey,
                         UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      rule.setName(name)
        .setDescription(description)
        .setSeverity(Severity.ordinal(severity))
        .setUpdatedAt(new Date(system.now()));
      ruleDao.update(rule, session);

      List<RuleParamDto> ruleParams = ruleDao.selectParameters(rule.getId(), session);
      for (RuleParamDto ruleParam : ruleParams) {
        String value = paramsByKey.get(ruleParam.getName());
        ruleParam.setDefaultValue(Strings.emptyToNull(value));
        ruleDao.update(ruleParam, session);
      }
      session.commit();
      reindexRule(rule, ruleParams);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteRule(RuleDto rule, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      // Set status REMOVED on rule
      rule.setStatus(Rule.STATUS_REMOVED)
        .setUpdatedAt(new Date(system.now()));
      ruleDao.update(rule, session);
      session.commit();
      reindexRule(rule, session);

      // Delete all active rules and active rule params linked to the rule
      List<ActiveRuleDto> activeRules = activeRuleDao.selectByRuleId(rule.getId());
      for (ActiveRuleDto activeRule : activeRules) {
        activeRuleDao.deleteParameters(activeRule.getId(), session);
      }
      activeRuleDao.deleteFromRule(rule.getId(), session);
      session.commit();
      ruleRegistry.deleteActiveRules(newArrayList(Iterables.transform(activeRules, new Function<ActiveRuleDto, Integer>() {
        @Override
        public Integer apply(ActiveRuleDto input) {
          return input.getId();
        }
      })));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void reindexRule(RuleDto rule, SqlSession session) {
    reindexRule(rule, ruleDao.selectParameters(rule.getId(), session));
  }

  private void reindexRule(RuleDto rule, List<RuleParamDto> ruleParams) {
    ruleRegistry.save(rule, ruleParams);
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private String getLoggedLogin(UserSession userSession) {
    String login = userSession.login();
    if (Strings.isNullOrEmpty(login)) {
      throw new BadRequestException("User login can't be null");
    }
    return login;
  }

}

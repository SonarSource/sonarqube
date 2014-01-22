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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
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
import org.sonar.core.rule.*;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileRuleOperations implements ServerComponent {

  private final MyBatis myBatis;
  private final ActiveRuleDao activeRuleDao;
  private final RuleDao ruleDao;
  private final RuleTagDao ruleTagDao;
  private final RuleRegistry ruleRegistry;

  private final System2 system;

  public QProfileRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, RuleTagDao ruleTagDao, RuleRegistry ruleRegistry) {
    this(myBatis, activeRuleDao, ruleDao, ruleTagDao, ruleRegistry, System2.INSTANCE);
  }

  @VisibleForTesting
  QProfileRuleOperations(MyBatis myBatis, ActiveRuleDao activeRuleDao, RuleDao ruleDao, RuleTagDao ruleTagDao, RuleRegistry ruleRegistry, System2 system) {
    this.myBatis = myBatis;
    this.activeRuleDao = activeRuleDao;
    this.ruleDao = ruleDao;
    this.ruleTagDao = ruleTagDao;
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
        .setSeverity(getSeverityOrdinal(severity))
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

      List<RuleRuleTagDto> templateRuleTags = ruleDao.selectTags(templateRule.getId(), session);
      List<RuleRuleTagDto> ruleTags = newArrayList();
      for (RuleRuleTagDto tag: templateRuleTags) {
        RuleRuleTagDto newTag = new RuleRuleTagDto()
          .setRuleId(rule.getId())
          .setTagId(tag.getTagId())
          .setTag(tag.getTag())
          .setType(tag.getType());
        ruleDao.insert(newTag, session);
        ruleTags.add(newTag);
      }

      session.commit();
      reindexRule(rule, ruleParams, ruleTags);
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
        .setSeverity(getSeverityOrdinal(severity))
        .setUpdatedAt(new Date(system.now()));
      ruleDao.update(rule, session);

      List<RuleParamDto> ruleParams = ruleDao.selectParameters(rule.getId(), session);
      for (RuleParamDto ruleParam : ruleParams) {
        String value = paramsByKey.get(ruleParam.getName());
        ruleParam.setDefaultValue(Strings.emptyToNull(value));
        ruleDao.update(ruleParam, session);
      }
      List<RuleRuleTagDto> ruleTags = ruleDao.selectTags(rule.getId(), session);
      session.commit();
      reindexRule(rule, ruleParams, ruleTags);
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

  public void updateTags(RuleDto rule, List<String> newTags, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      Map<String, Long> neededTagIds = validateAndGetTagIds(newTags, session);

      final Integer ruleId = rule.getId();

      boolean ruleChanged = synchronizeTags(ruleId, newTags, neededTagIds, session);
      if (ruleChanged) {
        rule.setUpdatedAt(new Date(system.now()));
        ruleDao.update(rule, session);
        session.commit();
        reindexRule(rule, session);
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private Map<String, Long> validateAndGetTagIds(List<String> newTags, SqlSession session) {
    Map<String, Long> neededTagIds = Maps.newHashMap();
    Set<String> unknownTags = Sets.newHashSet();
    for (String tag: newTags) {
      Long tagId = ruleTagDao.selectId(tag, session);
      if (tagId == null) {
        unknownTags.add(tag);
      } else {
        neededTagIds.put(tag, tagId);
      }
    }
    if (!unknownTags.isEmpty()) {
      throw new NotFoundException("The following tags are unknown and must be created before association: "
        + StringUtils.join(unknownTags, ", "));
    }
    return neededTagIds;
  }

  private boolean synchronizeTags(final Integer ruleId, List<String> newTags, Map<String, Long> neededTagIds, SqlSession session) {
    boolean ruleChanged = false;

    Set<String> tagsToKeep = Sets.newHashSet();
    List<RuleRuleTagDto> currentTags = ruleDao.selectTags(ruleId, session);
    for (RuleRuleTagDto existingTag: currentTags) {
      if(existingTag.getType() == RuleTagType.ADMIN && !newTags.contains(existingTag.getTag())) {
        ruleDao.deleteTag(existingTag, session);
        ruleChanged = true;
      } else {
        tagsToKeep.add(existingTag.getTag());
      }
    }

    for (String tag: newTags) {
      if (! tagsToKeep.contains(tag)) {
        ruleDao.insert(new RuleRuleTagDto().setRuleId(ruleId).setTagId(neededTagIds.get(tag)).setType(RuleTagType.ADMIN), session);
        ruleChanged = true;
      }
    }

    return ruleChanged;
  }

  private void reindexRule(RuleDto rule, SqlSession session) {
    reindexRule(rule, ruleDao.selectParameters(rule.getId(), session), ruleDao.selectTags(rule.getId(), session));
  }

  private void reindexRule(RuleDto rule, List<RuleParamDto> ruleParams, List<RuleRuleTagDto> ruleTags) {
    ruleRegistry.save(rule, ruleParams, ruleTags);
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

  private static int getSeverityOrdinal(String severity) {
    return Severity.ALL.indexOf(severity);
  }
}

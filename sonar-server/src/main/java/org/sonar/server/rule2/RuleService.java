/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.rule2;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule2.index.RuleIndex;
import org.sonar.server.rule2.index.RuleNormalizer;
import org.sonar.server.rule2.index.RuleQuery;
import org.sonar.server.rule2.index.RuleResult;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @since 4.4
 */
public class RuleService implements ServerComponent {

  private final RuleIndex index;
  private final ActiveRuleIndex activeRuleIndex;
  private final DbClient db;

  public RuleService(ActiveRuleIndex activeRuleIndex, RuleIndex index, DbClient db) {
    this.index = index;
    this.activeRuleIndex = activeRuleIndex;
    this.db = db;
  }

  @CheckForNull
  public Rule getByKey(RuleKey key) {
    return index.getByKey(key);
  }

  public RuleQuery newRuleQuery() {
    return new RuleQuery();
  }

  public RuleResult search(RuleQuery query, QueryOptions options) {

    /** keep only supported fields and add the fields to always return */
    options.filterFieldsToReturn(RuleIndex.PUBLIC_FIELDS);

    RuleResult result = index.search(query, options);

    /** Check for activation */
    if (query.getActivation() != null && !query.getActivation().isEmpty()) {
      if (query.getActivation().equalsIgnoreCase("true")) {
        for (Rule rule : result.getHits()) {
          if(query.getqProfileKey() == null){
            throw new IllegalStateException("\"activation=true\" requires a profile key!");
          }
          QualityProfileKey qualityProfileKey =  QualityProfileKey.parse(query.getqProfileKey());
          result.getActiveRules().put(rule.key().toString(),
            activeRuleIndex.getByRuleKeyAndProfileKey(rule.key(),qualityProfileKey));
        }
      } else if (query.getActivation().equalsIgnoreCase("all")) {
        for (Rule rule : result.getHits()) {
          List<ActiveRule> activeRules = activeRuleIndex.findByRule(rule.key());
          for (ActiveRule activeRule : activeRules) {
            result.getActiveRules().put(rule.key().toString(), activeRule);
          }
        }
      }
    }

    return result;
  }

  /**
   * List all tags
   */
  public Set<String> listTags() {
    return index.terms(RuleNormalizer.RuleField.TAGS.key(), RuleNormalizer.RuleField.SYSTEM_TAGS.key());
  }

  public void setTags(RuleKey ruleKey, Set<String> tags) {

    checkAdminPermission(UserSession.get());

    DbSession dbSession = db.openSession(false);
    try {
      RuleDto rule = db.ruleDao().getNonNullByKey(ruleKey, dbSession);
      boolean changed = RuleTagHelper.applyTags(rule, tags);
      if (changed) {
        db.ruleDao().update(rule, dbSession);
        dbSession.commit();
      }
    } finally {
      dbSession.close();
    }
  }

  /**
   * Extend rule description by adding a note.
   *
   * @param ruleKey      the required key
   * @param markdownNote markdown text. <code>null</code> to remove current note.
   */
  public void setNote(RuleKey ruleKey, @Nullable String markdownNote) {
    UserSession userSession = UserSession.get();
    checkAdminPermission(userSession);
    DbSession dbSession = db.openSession(false);
    try {
      RuleDto rule = db.ruleDao().getNonNullByKey(ruleKey, dbSession);
      if (StringUtils.isBlank(markdownNote)) {
        rule.setNoteData(null);
        rule.setNoteCreatedAt(null);
        rule.setNoteUpdatedAt(null);
        rule.setNoteUserLogin(null);
      } else {
        rule.setNoteData(markdownNote);
        rule.setNoteCreatedAt(rule.getNoteCreatedAt() != null ? rule.getNoteCreatedAt() : new Date());
        rule.setNoteUpdatedAt(new Date());
        rule.setNoteUserLogin(userSession.login());
      }
      db.ruleDao().update(rule, dbSession);
      dbSession.commit();
    } finally {
      dbSession.close();
    }
  }

  private void checkAdminPermission(UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  public RuleService refresh() {
    this.index.refresh();
    return this;
  }
}

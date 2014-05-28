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
package org.sonar.server.rule;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.rule.index.RuleResult;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Set;

/**
 * @since 4.4
 */
public class RuleService implements ServerComponent {

  private final RuleIndex index;
  private final DbClient db;
  private final RuleUpdater ruleUpdater;

  public RuleService(RuleIndex index, DbClient db, RuleUpdater ruleUpdater) {
    this.index = index;
    this.db = db;
    this.ruleUpdater = ruleUpdater;
  }

  @CheckForNull
  public org.sonar.server.rule.Rule getByKey(RuleKey key) {
    return index.getByKey(key);
  }

  public RuleQuery newRuleQuery() {
    return new RuleQuery();
  }

  public RuleResult search(RuleQuery query, QueryOptions options) {
    return index.search(query, options);
  }

  /**
   * List all tags, including system tags, defined on rules
   */
  public Set<String> listTags() {
    /** using combined _TAGS field of ES until ES update that has multiTerms aggregation */
    return index.terms(RuleNormalizer.RuleField._TAGS.field());
  }

  /**
   * Set tags for rule.
   *
   * @param ruleKey the required key
   * @param tags    Set of tags. <code>null</code> to remove all tags.
   */
  public void setTags(RuleKey ruleKey, Set<String> tags) {
    checkAdminPermission(UserSession.get());

    DbSession dbSession = db.openSession(false);
    try {
      RuleDto rule = db.ruleDao().getNonNullByKey(dbSession, ruleKey);
      boolean changed = RuleTagHelper.applyTags(rule, tags);
      if (changed) {
        db.ruleDao().update(dbSession, rule);
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
      RuleDto rule = db.ruleDao().getNonNullByKey(dbSession, ruleKey);
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
      db.ruleDao().update(dbSession, rule);
      dbSession.commit();
    } finally {
      dbSession.close();
    }
  }

  public void update(RuleUpdate update) {
    ruleUpdater.update(update, UserSession.get());
  }

  private void checkAdminPermission(UserSession userSession) {
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }
}

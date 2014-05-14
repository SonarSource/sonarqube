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

import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule2.index.RuleIndex;
import org.sonar.server.rule2.index.RuleNormalizer;
import org.sonar.server.rule2.index.RuleQuery;
import org.sonar.server.rule2.index.RuleResult;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import java.util.Set;

/**
 * @since 4.4
 */
public class RuleService implements ServerComponent {

  private final RuleIndex index;
  private final DbClient db;

  public RuleService(RuleIndex index, DbClient db) {
    this.index = index;
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
    // keep only supported fields and add the fields to always return
    options.filterFieldsToReturn(RuleIndex.PUBLIC_FIELDS);
    options.addFieldsToReturn(RuleNormalizer.RuleField.REPOSITORY.key(), RuleNormalizer.RuleField.KEY.key());
    return index.search(query, options);
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
      RuleDto rule = db.ruleDao().getByKey(ruleKey, dbSession);
      if (rule == null) {
        throw new NotFoundException(String.format("Rule %s not found", ruleKey));
      }
      boolean changed = RuleTagHelper.applyTags(rule, tags);
      if (changed) {
        db.ruleDao().update(rule, dbSession);
        dbSession.commit();
      }
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

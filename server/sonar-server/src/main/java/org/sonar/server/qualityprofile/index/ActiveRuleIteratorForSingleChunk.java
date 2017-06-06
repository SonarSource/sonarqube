/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.db.rule.SeverityUtil;
import org.sonar.server.qualityprofile.ActiveRule;

import static org.apache.commons.lang.StringUtils.repeat;

/**
 * Scrolls over table ISSUES and reads documents to populate
 * the issues index
 */
class ActiveRuleIteratorForSingleChunk implements ActiveRuleIterator {

  private static final String[] COLUMNS = {
    "ar.id",
    "ar.failure_level",
    "ar.inheritance",
    "r.plugin_name",
    "r.plugin_rule_key",
    "rp.kee"
  };

  private static final String SQL_ALL = "select " + StringUtils.join(COLUMNS, ",") + " from active_rules ar " +
    " inner join rules_profiles rp on rp.id = ar.profile_id " +
    " inner join rules r on r.id = ar.rule_id ";

  private final PreparedStatement stmt;
  private final ResultSetIterator<ActiveRuleDoc> iterator;

  ActiveRuleIteratorForSingleChunk(DbClient dbClient, DbSession dbSession) {
    try {
      stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, SQL_ALL);
      iterator = new ActiveRuleIteratorInternal(stmt);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all active_rules", e);
    }
  }

  ActiveRuleIteratorForSingleChunk(DbClient dbClient, DbSession dbSession, RulesProfileDto ruleProfile) {
    try {
      stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, SQL_ALL + " where rp.kee = ?");
      stmt.setString(1, ruleProfile.getKee());
      iterator = new ActiveRuleIteratorInternal(stmt);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to prepare SQL request to select active_rules of profile " + ruleProfile.getKee(), e);
    }
  }

  ActiveRuleIteratorForSingleChunk(DbClient dbClient, DbSession dbSession, List<Integer> activeRuleIds) {
    try {
      stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, SQL_ALL + " where ar.id in (" + repeat("?", ",", activeRuleIds.size()) + ")");
      for (int i = 0; i < activeRuleIds.size(); i++) {
        stmt.setInt(i + 1, activeRuleIds.get(i));
      }
      iterator = new ActiveRuleIteratorInternal(stmt);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to prepare SQL request to select active_rules", e);
    }
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public ActiveRuleDoc next() {
    return iterator.next();
  }

  @Override
  public void close() {
    try {
      iterator.close();
    } finally {
      DatabaseUtils.closeQuietly(stmt);
    }
  }

  private static final class ActiveRuleIteratorInternal extends ResultSetIterator<ActiveRuleDoc> {

    ActiveRuleIteratorInternal(PreparedStatement stmt) throws SQLException {
      super(stmt);
    }

    @Override
    protected ActiveRuleDoc read(ResultSet rs) throws SQLException {
      long activeRuleId = rs.getLong(1);
      int severity = rs.getInt(2);
      String inheritance = rs.getString(3);
      RuleKey ruleKey = RuleKey.of(rs.getString(4), rs.getString(5));
      String ruleProfileUuid = rs.getString(6);

      return new ActiveRuleDoc(String.valueOf(activeRuleId))
        .setRuleProfileUuid(ruleProfileUuid)
        .setRuleKey(ruleKey)
        // all the fields must be present, even if value is null
        .setSeverity(SeverityUtil.getSeverityFromOrdinal(severity))
        .setInheritance(inheritance == null ? ActiveRule.Inheritance.NONE.name() : inheritance);
    }
  }
}

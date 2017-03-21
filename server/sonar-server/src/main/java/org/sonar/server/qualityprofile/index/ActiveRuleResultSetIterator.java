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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.rule.SeverityUtil;
import org.sonar.server.qualityprofile.ActiveRule;

/**
 * Scrolls over table ACTIVE_RULES and reads documents to populate the active rules index
 */
public class ActiveRuleResultSetIterator extends ResultSetIterator<ActiveRuleDoc> {

  private static final String[] FIELDS = {
    "a.failure_level",
    "a.inheritance",
    "r.plugin_name",
    "r.plugin_rule_key",
    "qp.organization_uuid",
    "qp.kee",
    "a.created_at",
    "a.updated_at"
  };

  private static final String SQL_ALL = "SELECT " + StringUtils.join(FIELDS, ",") + " FROM active_rules a " +
    "INNER JOIN rules_profiles qp ON qp.id=a.profile_id " +
    "INNER JOIN rules r ON r.id = a.rule_id";

  private static final String SQL_AFTER_DATE = SQL_ALL + " WHERE a.updated_at>?";

  private ActiveRuleResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  static ActiveRuleResultSetIterator create(DbClient dbClient, DbSession session, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, sql);
      if (afterDate > 0L) {
        stmt.setLong(1, afterDate);
      }
      return new ActiveRuleResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all active rules", e);
    }
  }

  @Override
  protected ActiveRuleDoc read(ResultSet rs) throws SQLException {
    int severity = rs.getInt(1);
    String inheritance = rs.getString(2);
    RuleKey ruleKey = RuleKey.of(rs.getString(3), rs.getString(4));
    String organizationUuid = rs.getString(5);
    String profileKey = rs.getString(6);
    ActiveRuleKey activeRuleKey = ActiveRuleKey.of(profileKey, ruleKey);

    ActiveRuleDoc doc = new ActiveRuleDoc(activeRuleKey);
    doc.setOrganizationUuid(organizationUuid);
    // all the fields must be present, even if value is null
    doc.setSeverity(SeverityUtil.getSeverityFromOrdinal(severity));

    doc.setInheritance(inheritance == null ? ActiveRule.Inheritance.NONE.name() : inheritance);

    doc.setCreatedAt(rs.getLong(7));
    doc.setUpdatedAt(rs.getLong(8));
    return doc;
  }

}

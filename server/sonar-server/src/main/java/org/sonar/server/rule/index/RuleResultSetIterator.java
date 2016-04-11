/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.rule.index;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.SeverityUtil;
import org.sonar.markdown.Markdown;

/**
 * Scrolls over table RULES and reads documents to populate the rules index
 */
public class RuleResultSetIterator extends ResultSetIterator<RuleDoc> {

  private static final String[] FIELDS = {
    // column 1
    "r.plugin_rule_key",
    "r.plugin_name",
    "r.name",
    "r.description",
    "r.description_format",
    "r.priority",
    "r.status",
    "r.is_template",
    "r.tags",
    "r.system_tags",

    // column 11
    "t.plugin_rule_key",
    "t.plugin_name",
    "r.plugin_config_key",
    "r.language",
    "r.rule_type",
    "r.created_at",
    "r.updated_at",
  };

  private static final String SQL_ALL = "SELECT " + StringUtils.join(FIELDS, ",") + " FROM rules r " +
    "LEFT OUTER JOIN rules t ON t.id=r.template_id";

  private static final String SQL_AFTER_DATE = SQL_ALL + " WHERE r.updated_at>?";

  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private RuleResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  static RuleResultSetIterator create(DbClient dbClient, DbSession session, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, sql);
      if (afterDate > 0L) {
        stmt.setLong(1, afterDate);
      }
      return new RuleResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all rules", e);
    }
  }

  @Override
  protected RuleDoc read(ResultSet rs) throws SQLException {
    RuleDoc doc = new RuleDoc(Maps.<String, Object>newHashMapWithExpectedSize(16));

    String ruleKey = rs.getString(1);
    String repositoryKey = rs.getString(2);
    RuleKey key = RuleKey.of(repositoryKey, ruleKey);

    // all the fields must be present, even if value is null
    doc.setKey(key.toString());
    doc.setRuleKey(ruleKey);
    doc.setRepository(repositoryKey);
    doc.setName(rs.getString(3));

    String description = rs.getString(4);
    String descriptionFormat = rs.getString(5);
    if (descriptionFormat != null) {
      if (RuleDto.Format.HTML.equals(RuleDto.Format.valueOf(descriptionFormat))) {
        doc.setHtmlDescription(description);
      } else {
        doc.setHtmlDescription(description == null ? null : Markdown.convertToHtml(description));
      }
    }

    doc.setSeverity(SeverityUtil.getSeverityFromOrdinal(rs.getInt(6)));
    doc.setStatus(rs.getString(7));
    doc.setIsTemplate(rs.getBoolean(8));
    doc.setAllTags(Sets.union(stringTagsToSet(rs.getString(9)), stringTagsToSet(rs.getString(10))));

    String templateRuleKey = rs.getString(11);
    String templateRepoKey = rs.getString(12);
    if (templateRepoKey != null && templateRuleKey != null) {
      doc.setTemplateKey(RuleKey.of(templateRepoKey, templateRuleKey).toString());
    } else {
      doc.setTemplateKey(null);
    }

    doc.setInternalKey(rs.getString(13));
    doc.setLanguage(rs.getString(14));
    doc.setType(RuleType.valueOf(rs.getInt(15)));
    doc.setCreatedAt(rs.getLong(16));
    doc.setUpdatedAt(rs.getLong(17));

    return doc;
  }

  private static Set<String> stringTagsToSet(@Nullable String tags) {
    return ImmutableSet.copyOf(TAGS_SPLITTER.split(tags == null ? "" : tags));
  }
}

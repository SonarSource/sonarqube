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
package org.sonar.server.rule.index;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.SeverityUtil;
import org.sonar.markdown.Markdown;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.joining;

/**
 * Scrolls over table RULES and reads documents to populate the rules index
 */
public class RuleIteratorForSingleChunk implements RuleIterator {

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
    "rm.tags",
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
    "LEFT OUTER JOIN rules t ON t.id=r.template_id " +
    "LEFT OUTER JOIN rules_metadata rm ON rm.rule_id = r.id and rm.organization_uuid=?";

  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final DbSession session;

  private final OrganizationDto organization;
  private final List<RuleKey> ruleKeys;

  private final PreparedStatement stmt;
  private final ResultSetIterator<RuleDoc> iterator;

  RuleIteratorForSingleChunk(DbClient dbClient, OrganizationDto organization, @Nullable List<RuleKey> ruleKeys) {
    checkArgument(ruleKeys == null || ruleKeys.size() <= DatabaseUtils.PARTITION_SIZE_FOR_ORACLE,
      "Cannot search for more than " + DatabaseUtils.PARTITION_SIZE_FOR_ORACLE + " rule keys at once. Please provide the keys in smaller chunks.");
    this.organization = organization;
    this.ruleKeys = ruleKeys;
    this.session = dbClient.openSession(false);

    try {
      String sql = createSql();
      stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, sql);
      iterator = createIterator();
    } catch (Exception e) {
      session.close();
      throw new IllegalStateException("Fail to prepare SQL request to select all rules", e);
    }
  }

  private RuleIteratorInternal createIterator() {
    try {
      setParameters(stmt);
      return new RuleIteratorInternal(stmt);
    } catch (SQLException e) {
      DatabaseUtils.closeQuietly(stmt);
      throw new IllegalStateException("Fail to prepare SQL request to select all rules", e);
    }
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public RuleDoc next() {
    return iterator.next();
  }

  private String createSql() {
    StringBuilder sql = new StringBuilder(SQL_ALL);
    if (ruleKeys != null && !ruleKeys.isEmpty()) {
      sql.append(" WHERE ");
      sql.append(ruleKeys.stream()
        .map(x -> "(r.plugin_name=? AND r.plugin_rule_key=?)")
        .collect(joining(" OR "))
      );
    }
    return sql.toString();
  }

  private void setParameters(PreparedStatement stmt) throws SQLException {
    AtomicInteger index = new AtomicInteger(1);
    stmt.setString(index.getAndIncrement(), organization.getUuid());
    if (ruleKeys != null && !ruleKeys.isEmpty()) {
      for (RuleKey ruleKey : ruleKeys) {
        stmt.setString(index.getAndIncrement(), ruleKey.repository());
        stmt.setString(index.getAndIncrement(), ruleKey.rule());
      }
    }
  }

  @Override
  public void close() {
    try {
      iterator.close();
    } finally {
      DatabaseUtils.closeQuietly(stmt);
      session.close();
    }
  }

  private static final class RuleIteratorInternal extends ResultSetIterator<RuleDoc> {

    public RuleIteratorInternal(PreparedStatement stmt) throws SQLException {
      super(stmt);
    }

    @Override
    protected RuleDoc read(ResultSet rs) throws SQLException {
      RuleDoc doc = new RuleDoc();

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
      if (descriptionFormat != null && description != null) {
        String htmlDescription;
        if (RuleDto.Format.HTML == RuleDto.Format.valueOf(descriptionFormat)) {
          htmlDescription = description;
        } else {
          htmlDescription = Markdown.convertToHtml(description);
        }
        doc.setHtmlDescription(htmlDescription);
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
}

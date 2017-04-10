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
    "r.system_tags",
    "t.plugin_rule_key",

    // column 11
    "t.plugin_name",
    "r.plugin_config_key",
    "r.language",
    "r.rule_type",
    "r.created_at",
    "r.updated_at",
  };

  private static final String SQL_ALL = "SELECT " + StringUtils.join(FIELDS, ",") + " FROM rules r " +
    "LEFT OUTER JOIN rules t ON t.id=r.template_id";
  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final DbSession session;

  private final List<RuleKey> ruleKeys;

  private final PreparedStatement stmt;
  private final ResultSetIterator<RuleDocWithSystemScope> iterator;

  RuleIteratorForSingleChunk(DbClient dbClient, @Nullable List<RuleKey> ruleKeys) {
    checkArgument(ruleKeys == null || ruleKeys.size() <= DatabaseUtils.PARTITION_SIZE_FOR_ORACLE,
      "Cannot search for more than " + DatabaseUtils.PARTITION_SIZE_FOR_ORACLE + " rule keys at once. Please provide the keys in smaller chunks.");
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
  public RuleDocWithSystemScope next() {
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

  private static final class RuleIteratorInternal extends ResultSetIterator<RuleDocWithSystemScope> {

    public RuleIteratorInternal(PreparedStatement stmt) throws SQLException {
      super(stmt);
    }

    @Override
    protected RuleDocWithSystemScope read(ResultSet rs) throws SQLException {
      RuleDoc doc = new RuleDoc();
      RuleExtensionDoc extensionDoc = new RuleExtensionDoc().setScope(RuleExtensionScope.system());

      String ruleKey = rs.getString(1);
      String repositoryKey = rs.getString(2);
      RuleKey key = RuleKey.of(repositoryKey, ruleKey);
      extensionDoc.setRuleKey(key);

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
      extensionDoc.setTags(stringTagsToSet(rs.getString(9)));

      String templateRuleKey = rs.getString(10);
      String templateRepoKey = rs.getString(11);
      if (templateRepoKey != null && templateRuleKey != null) {
        doc.setTemplateKey(RuleKey.of(templateRepoKey, templateRuleKey).toString());
      } else {
        doc.setTemplateKey(null);
      }

      doc.setInternalKey(rs.getString(12));
      doc.setLanguage(rs.getString(13));
      doc.setType(RuleType.valueOf(rs.getInt(14)));
      doc.setCreatedAt(rs.getLong(15));
      doc.setUpdatedAt(rs.getLong(16));

      return new RuleDocWithSystemScope(doc, extensionDoc);
    }

    private static Set<String> stringTagsToSet(@Nullable String tags) {
      return ImmutableSet.copyOf(TAGS_SPLITTER.split(tags == null ? "" : tags));
    }
  }
}

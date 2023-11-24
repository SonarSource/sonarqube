/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;

public class PopulateRuleTagsTable extends DataChange {

  private static final String SELECT_QUERY = """
   SELECT uuid, system_tags AS tag, 1 as is_system_tag
    FROM rules
    WHERE system_tags IS NOT NULL
   UNION ALL
   SELECT uuid, tags AS tag, 0 as is_system_tag
    FROM rules
    WHERE tags IS NOT NULL
     """;

  private static final String INSERT_QUERY = """
    INSERT INTO rule_tags (rule_uuid, is_system_tag, value)
    VALUES (?, ?, ?)
     """;

  public PopulateRuleTagsTable(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (isTableAlreadyPopulated(context)) {
      return;
    }

    List<PopulateRuleTagsTable.Tag> allTags = findAllTags(context);
    if (allTags.isEmpty()) {
      return;
    }

    Upsert insertTagsQuery = context.prepareUpsert(INSERT_QUERY);
    for (PopulateRuleTagsTable.Tag tag : allTags) {
      insertEveryTag(insertTagsQuery, tag.ruleUuid(), tag.values(), tag.isSystemTag());
    }
    insertTagsQuery.execute().commit();
  }

  private static void insertEveryTag(Upsert insertRuleTags, String ruleUuid, Set<String> values, boolean isSystemTag) throws SQLException {
    for (String tag : values) {
      insertRuleTags
        .setString(1, ruleUuid)
        .setBoolean(2, isSystemTag)
        .setString(3, tag)
        .addBatch();
    }
  }

  private static List<PopulateRuleTagsTable.Tag> findAllTags(Context context) throws SQLException {
    return context.prepareSelect(SELECT_QUERY)
      .list(r -> new PopulateRuleTagsTable.Tag(r.getString(1), parseTagString(r.getString(2)), r.getBoolean(3)));
  }

  private static boolean isTableAlreadyPopulated(Context context) throws SQLException {
    try (Select select = context.prepareSelect("select count(*) from rule_tags")) {
      return Optional.ofNullable(select.get(t -> t.getInt(1) > 0))
        .orElseThrow();
    }
  }

  private static Set<String> parseTagString(String tagString) {
    return Arrays.stream(tagString.split(","))
      .filter(s -> !s.isEmpty())
      .collect(Collectors.toSet());
  }

  private record Tag(String ruleUuid, Set<String> values, boolean isSystemTag) {
  }

}

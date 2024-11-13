/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;

public class PopulateRuleTagsTable extends DataChange {

  private static final String SELECT_QUERY = """
    SELECT uuid, NULL, system_tags AS tag, 1 as is_system_tag
     FROM rules
     WHERE system_tags IS NOT NULL
    UNION ALL
    SELECT r.uuid, rm.organization_uuid, rm.tags AS tag, 0 as is_system_tag
     FROM rules r
       INNER JOIN rules_metadata rm ON rm.rule_uuid = r.uuid
     WHERE rm.tags IS NOT NULL
      """;

  private static final String INSERT_QUERY = """
    INSERT INTO rule_tags (rule_uuid, organization_uuid, is_system_tag, value)
    VALUES (?, ?, ?, ?)
     """;

  public PopulateRuleTagsTable(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (isTableAlreadyPopulated(context)) {
      return;
    }

    List<Tags> allTags = findAllTags(context);
    if (allTags.isEmpty()) {
      return;
    }
    /* TODO */
    // allTags = removeDuplicatesForAllRule(allTags);

    Upsert insertTagsQuery = context.prepareUpsert(INSERT_QUERY);
    for (Tags tags : allTags) {
      insertEveryTag(insertTagsQuery, tags.ruleUuid(), tags.organizationUuid(), tags.values(), tags.isSystemTag());
    }
    insertTagsQuery.execute().commit();
  }

  /**
   * System tags and custom tags can contain the same values. In this case, we keep only the system tag.
   */
  private static List<Tags> removeDuplicatesForAllRule(List<Tags> allTags) {
    Map<String, List<Tags>> tagsByRuleUuid = allTags.stream().collect(Collectors.groupingBy(Tags::ruleUuid));
    List<Tags> listWithoutDuplicates = new ArrayList<>();

    for (Map.Entry<String, List<Tags>> entry : tagsByRuleUuid.entrySet()) {
      listWithoutDuplicates.addAll(removeDuplicateForRule(entry.getValue()));
    }
    return listWithoutDuplicates;
  }

  private static List<Tags> removeDuplicateForRule(List<Tags> ruleTags) {
    Optional<Tags> systemTags = ruleTags.stream().filter(Tags::isSystemTag).findFirst();
    Optional<Tags> manualTags = ruleTags.stream().filter(t -> !t.isSystemTag()).findFirst();

    if (systemTags.isEmpty()) {
      return List.of(manualTags.orElseThrow());
    } else if (manualTags.isEmpty()) {
      return List.of(systemTags.orElseThrow());
    } else {
      Set<String> systemTagValues = systemTags.get().values();
      Set<String> manualTagValues = manualTags.get().values();
      Set<String> commonValues = new HashSet<>(systemTagValues);
      commonValues.retainAll(manualTagValues);

      if (commonValues.isEmpty()) {
        return List.of(manualTags.orElseThrow(), systemTags.orElseThrow());
      } else {
        manualTagValues.removeAll(commonValues);
        return List.of(systemTags.orElseThrow(), new Tags(manualTags.get().ruleUuid(), manualTags.get().organizationUuid(), manualTagValues, false));
      }
    }
  }

  private static void insertEveryTag(Upsert insertRuleTags, String ruleUuid, String organizationUuid, Set<String> values, boolean isSystemTag) throws SQLException {
    for (String tag : values) {
      insertRuleTags
        .setString(1, ruleUuid)
        .setString(2, organizationUuid)
        .setBoolean(3, isSystemTag)
        .setString(4, tag)
        .addBatch();
    }
  }

  private static List<Tags> findAllTags(Context context) throws SQLException {
    return context.prepareSelect(SELECT_QUERY)
      .list(r -> new Tags(r.getString(1), r.getString(2), parseTagString(r.getString(3)), r.getBoolean(4)));
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

  private record Tags(String ruleUuid, String organizationUuid, Set<String> values, boolean isSystemTag) {
  }

}

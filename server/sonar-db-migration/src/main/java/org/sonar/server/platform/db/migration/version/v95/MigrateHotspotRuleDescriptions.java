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
package org.sonar.server.platform.db.migration.version.v95;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;
import static org.sonar.server.platform.db.migration.version.v95.DbVersion95.DEFAULT_DESCRIPTION_KEY;

public class MigrateHotspotRuleDescriptions extends DataChange {

  private static final String SELECT_DEFAULT_HOTSPOTS_DESCRIPTIONS = "select r.uuid, rds.uuid, rds.content from rules r \n"
    + "left join " + RULE_DESCRIPTION_SECTIONS_TABLE + " rds on r.uuid = rds.rule_uuid \n"
    + "where r.rule_type = 4 and r.template_uuid is null and rds.kee = '" + DEFAULT_DESCRIPTION_KEY + "' and rds.rule_uuid not in"
    + " (select rds2.rule_uuid from " + RULE_DESCRIPTION_SECTIONS_TABLE + " rds2 where rds2.kee != '" + DEFAULT_DESCRIPTION_KEY + "')";

  private static final String INSERT_INTO_RULE_DESC_SECTIONS = "insert into " + RULE_DESCRIPTION_SECTIONS_TABLE + " (uuid, rule_uuid, kee, content) values "
    + "(?,?,?,?)";
  private static final String DELETE_DEFAULT_RULE_DESC_SECTIONS = "delete from " + RULE_DESCRIPTION_SECTIONS_TABLE + " where uuid = ? ";

  private final UuidFactory uuidFactory;

  public MigrateHotspotRuleDescriptions(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    List<RuleDescriptionSection> selectRuleDescriptionSection = findExistingRuleDescriptions(context);
    if (selectRuleDescriptionSection.isEmpty()) {
      return;
    }
    insertRuleDescSections(context, selectRuleDescriptionSection);
  }

  private static List<RuleDescriptionSection> findExistingRuleDescriptions(Context context) throws SQLException {
    return context.prepareSelect(SELECT_DEFAULT_HOTSPOTS_DESCRIPTIONS)
      .list(r -> new RuleDescriptionSection(r.getString(1), r.getString(2), r.getString(3)));
  }

  private void insertRuleDescSections(Context context, List<RuleDescriptionSection> defaultRuleDescriptionSections) throws SQLException {
    Upsert insertRuleDescSectionsQuery = context.prepareUpsert(INSERT_INTO_RULE_DESC_SECTIONS);
    Upsert deleteQuery = context.prepareUpsert(DELETE_DEFAULT_RULE_DESC_SECTIONS);
    for (RuleDescriptionSection ruleDescriptionSection : defaultRuleDescriptionSections) {
      Map<String, String> sections = generateNewNamedSections(ruleDescriptionSection.getContent());
      if (sections.isEmpty()) {
        continue;
      }
      insertNewNamedSections(insertRuleDescSectionsQuery, ruleDescriptionSection.getRuleUuid(), sections);
      deleteOldDefaultSection(deleteQuery, ruleDescriptionSection.getSectionUuid());
    }
    insertRuleDescSectionsQuery.execute();
    deleteQuery.execute().commit();
  }

  private static Map<String, String> generateNewNamedSections(String descriptionInHtml) {
    String[] split = extractSection("", descriptionInHtml);
    String remainingText = split[0];
    String ruleDescriptionSection = split[1];

    split = extractSection("<h2>Exceptions</h2>", remainingText);
    remainingText = split[0];
    String exceptions = split[1];

    split = extractSection("<h2>Ask Yourself Whether</h2>", remainingText);
    remainingText = split[0];
    String askSection = split[1];

    split = extractSection("<h2>Sensitive Code Example</h2>", remainingText);
    remainingText = split[0];
    String sensitiveSection = split[1];

    split = extractSection("<h2>Noncompliant Code Example</h2>", remainingText);
    remainingText = split[0];
    String noncompliantSection = split[1];

    split = extractSection("<h2>Recommended Secure Coding Practices</h2>", remainingText);
    remainingText = split[0];
    String recommendedSection = split[1];

    split = extractSection("<h2>Compliant Solution</h2>", remainingText);
    remainingText = split[0];
    String compliantSection = split[1];

    split = extractSection("<h2>See</h2>", remainingText);
    remainingText = split[0];
    String seeSection = split[1];

    Map<String, String> keysToContent = new HashMap<>();
    Optional.ofNullable(createSection(ruleDescriptionSection, exceptions, remainingText)).ifPresent(d -> keysToContent.put(ROOT_CAUSE_SECTION_KEY, d));
    Optional.ofNullable(createSection(askSection, sensitiveSection, noncompliantSection)).ifPresent(d -> keysToContent.put(ASSESS_THE_PROBLEM_SECTION_KEY, d));
    Optional.ofNullable(createSection(recommendedSection, compliantSection, seeSection)).ifPresent(d -> keysToContent.put(HOW_TO_FIX_SECTION_KEY, d));
    return keysToContent;
  }

  private void insertNewNamedSections(Upsert insertRuleDescSections, String ruleUuid, Map<String, String> sections) throws SQLException {
    for (Map.Entry<String, String> sectionKeyToContent : sections.entrySet()) {
      insertRuleDescSections
        .setString(1, uuidFactory.create())
        .setString(2, ruleUuid)
        .setString(3, sectionKeyToContent.getKey())
        .setString(4, sectionKeyToContent.getValue())
        .addBatch();
    }
  }

  private static void deleteOldDefaultSection(Upsert delete, String sectionUuid) throws SQLException {
    delete
      .setString(1, sectionUuid)
      .addBatch();
  }

  private static String[] extractSection(String beginning, String description) {
    String endSection = "<h2>";
    int beginningIndex = description.indexOf(beginning);
    if (beginningIndex != -1) {
      int endIndex = description.indexOf(endSection, beginningIndex + beginning.length());
      if (endIndex == -1) {
        endIndex = description.length();
      }
      return new String[] {
        description.substring(0, beginningIndex) + description.substring(endIndex),
        description.substring(beginningIndex, endIndex)
      };
    } else {
      return new String[] {description, ""};
    }

  }

  @CheckForNull
  private static String createSection(String... contentPieces) {
    return trimToNull(String.join("", contentPieces));
  }

  @CheckForNull
  private static String trimToNull(String input) {
    return input.isEmpty() ? null : input;
  }

  private static class RuleDescriptionSection {
    private final String ruleUuid;
    private final String sectionUuid;
    private final String content;

    private RuleDescriptionSection(String ruleUuid, String sectionUuid, String content) {
      this.ruleUuid = ruleUuid;
      this.sectionUuid = sectionUuid;
      this.content = content;
    }

    public String getRuleUuid() {
      return ruleUuid;
    }

    public String getSectionUuid() {
      return sectionUuid;
    }

    public String getContent() {
      return content;
    }
  }
}

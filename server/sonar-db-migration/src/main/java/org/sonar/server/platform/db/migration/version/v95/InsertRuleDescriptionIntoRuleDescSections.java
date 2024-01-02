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
import java.util.List;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;

public class InsertRuleDescriptionIntoRuleDescSections extends DataChange {

  private static final String SELECT_EXISTING_RULE_DESCRIPTIONS = "select uuid, description from rules where description is not null "
    + "and uuid not in (select rule_uuid from " + RULE_DESCRIPTION_SECTIONS_TABLE + ")";
  private static final String INSERT_INTO_RULE_DESC_SECTIONS = "insert into " + RULE_DESCRIPTION_SECTIONS_TABLE + " (uuid, rule_uuid, kee, content) values "
    + "(?,?,?,?)";

  private final UuidFactory uuidFactory;

  public InsertRuleDescriptionIntoRuleDescSections(Database db, UuidFactory uuidFactory) {
    super(db);
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      if (!DatabaseUtils.tableColumnExists(connection, "rules", "description")) {
        return;
      }
    }
    List<RuleDb> selectRuleDb = findExistingRuleDescriptions(context);
    if (selectRuleDb.isEmpty()) {
      return;
    }
    insertRuleDescSections(context, selectRuleDb);
  }

  private static List<RuleDb> findExistingRuleDescriptions(Context context) throws SQLException {
    return context.prepareSelect(SELECT_EXISTING_RULE_DESCRIPTIONS)
      .list(r -> new RuleDb(r.getString(1), r.getString(2)));
  }

  private void insertRuleDescSections(Context context, List<RuleDb> selectRuleDb) throws SQLException {
    Upsert insertRuleDescSections = context.prepareUpsert(INSERT_INTO_RULE_DESC_SECTIONS);
    for (RuleDb ruleDb : selectRuleDb) {
      insertRuleDescSections
        .setString(1, uuidFactory.create())
        .setString(2, ruleDb.getUuid())
        .setString(3, DbVersion95.DEFAULT_DESCRIPTION_KEY)
        .setString(4, ruleDb.getDescription())
        .addBatch();
    }
    insertRuleDescSections.execute().commit();
  }

  private static class RuleDb {
    private final String uuid;
    private final String description;

    private RuleDb(String uuid, String description) {
      this.uuid = uuid;
      this.description = description;
    }

    public String getUuid() {
      return uuid;
    }

    public String getDescription() {
      return description;
    }
  }
}

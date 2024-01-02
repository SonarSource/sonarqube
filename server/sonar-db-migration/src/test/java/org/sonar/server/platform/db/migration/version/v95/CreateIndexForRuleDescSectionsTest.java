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
import java.util.Map;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.sonar.server.platform.db.migration.version.v95.CreateIndexForRuleDescSections.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;

public class CreateIndexForRuleDescSectionsTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CreateIndexForRuleDescSectionsTest.class, "schema.sql");

  private final CreateIndexForRuleDescSections createIndex = new CreateIndexForRuleDescSections(db.database());

  @Test
  public void should_create_index() throws SQLException {
    db.assertIndexDoesNotExist(RULE_DESCRIPTION_SECTIONS_TABLE, INDEX_NAME);
    createIndex.execute();
    db.assertUniqueIndex(RULE_DESCRIPTION_SECTIONS_TABLE, INDEX_NAME, "rule_uuid", "kee");
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertIndexDoesNotExist(RULE_DESCRIPTION_SECTIONS_TABLE, INDEX_NAME);

    createIndex.execute();
    //re-entrant
    createIndex.execute();

    db.assertUniqueIndex(RULE_DESCRIPTION_SECTIONS_TABLE, INDEX_NAME, "rule_uuid", "kee");
  }

  @Test
  public void index_should_prevent_two_descriptions_with_same_key_on_same_rule() throws SQLException {
    createIndex.execute();

    insertRuleDescSection("default", "rule1");
    insertRuleDescSection("default", "rule2");
    insertRuleDescSection("non_default", "rule2");
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> this.insertRuleDescSection("default", "rule1"));
  }

  private void insertRuleDescSection(String key, String ruleUuid) {
    Map<String, Object> ruleParams = new HashMap<>();
    ruleParams.put("uuid", RandomStringUtils.randomAlphanumeric(40));
    ruleParams.put("rule_uuid", ruleUuid);
    ruleParams.put("kee", key);
    ruleParams.put("content", "content blablablabla");

    db.executeInsert("rule_desc_sections", ruleParams);
  }

}

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
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.sonar.server.platform.db.migration.version.v95.CreateRuleDescSectionsTable.RULE_DESCRIPTION_SECTIONS_TABLE;
import static org.sonar.server.platform.db.migration.version.v95.DbVersion95.DEFAULT_DESCRIPTION_KEY;

public class InsertRuleDescriptionIntoRuleDescSectionsTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(InsertRuleDescriptionIntoRuleDescSectionsTest.class, "schema.sql");

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private final DataChange insertRuleDescriptions = new InsertRuleDescriptionIntoRuleDescSections(db.database(), uuidFactory);

  @Test
  public void insertRuleDescriptions_doesNotFailIfRulesTableIsEmpty() {
    assertThatCode(insertRuleDescriptions::execute)
      .doesNotThrowAnyException();
  }

  @Test
  public void insertRuleDescriptions_whenDifferentRules_createsRelevantSectionDescription() throws SQLException {
    String description1 = RandomStringUtils.randomAlphanumeric(5000);
    String uuid1 = "uuid1";
    insertRule(uuid1, description1);

    String description2 = RandomStringUtils.randomAlphanumeric(5000);
    String uuid2 = "uuid2";
    insertRule(uuid2, description2);

    insertRuleDescriptions.execute();

    assertThat(db.countRowsOfTable(RULE_DESCRIPTION_SECTIONS_TABLE)).isEqualTo(2);
    assertRuleDescriptionCreated(uuid1, description1);
    assertRuleDescriptionCreated(uuid2, description2);
  }

  @Test
  public void insertRuleDescriptions_whenReentrant_doesNotFail() throws SQLException {
    String description1 = RandomStringUtils.randomAlphanumeric(5000);
    String uuid = "uuid1";
    insertRule(uuid, description1);

    insertRuleDescriptions.execute();
    insertRuleDescriptions.execute();
    insertRuleDescriptions.execute();

    assertThat(db.countRowsOfTable(RULE_DESCRIPTION_SECTIONS_TABLE)).isEqualTo(1);
    assertRuleDescriptionCreated(uuid, description1);
  }

  @Test
  public void insertRuleDescriptions_whenNoDescription_doesNotCreateRuleDescriptionSection() throws SQLException {
    String uuid = "uuid1";
    insertRule(uuid, null);

    insertRuleDescriptions.execute();

    assertThat(db.countRowsOfTable(RULE_DESCRIPTION_SECTIONS_TABLE)).isZero();
  }

  private void assertRuleDescriptionCreated(String uuid, String description1) {
    Map<String, Object> result1 = findRuleSectionDescription(uuid);
    assertThat(result1)
      .containsEntry("RULE_UUID", uuid)
      .containsEntry("KEE", DEFAULT_DESCRIPTION_KEY)
      .containsEntry("CONTENT", description1)
      .extractingByKey("UUID").isNotNull();
  }

  private Map<String, Object> findRuleSectionDescription(String uuid) {
    return db.selectFirst("select uuid, kee, rule_uuid, content from "
      + RULE_DESCRIPTION_SECTIONS_TABLE + " where rule_uuid = '" + uuid + "'");
  }

  private void insertRule(String uuid, String description) {
    Map<String, Object> ruleParams = new HashMap<>();
    ruleParams.put("uuid", uuid);
    ruleParams.put("plugin_rule_key", uuid);
    ruleParams.put("plugin_name", "plugin_name");
    ruleParams.put("scope", "ALL");
    ruleParams.put("is_template", false);
    ruleParams.put("is_external", true);
    ruleParams.put("is_ad_hoc", false);
    ruleParams.put("description", description);

    db.executeInsert("rules", ruleParams);
  }

}

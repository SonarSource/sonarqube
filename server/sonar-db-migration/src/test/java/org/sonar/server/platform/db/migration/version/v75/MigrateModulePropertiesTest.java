/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v75;

import com.google.common.collect.ImmutableSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;

public class MigrateModulePropertiesTest {
  private final static long NOW = 50_000_000_000L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateModulePropertiesTest.class, "schema.sql");
  private System2 system2 = new TestSystem2().setNow(NOW);
  private UuidFactory uuidFactory = new SequenceUuidFactory();
  private MigrateModuleProperties underTest = new MigrateModuleProperties(db.database(), system2);

  @Before
  public void setup() {
    String projectUuid = uuidFactory.create();
    String moduleUuid = uuidFactory.create();
    String subModule1Uuid = uuidFactory.create();
    String subModule2Uuid = uuidFactory.create();
    insertComponent(1, projectUuid, null, projectUuid, Qualifiers.PROJECT, "Multi-module project");
    insertComponent(2, moduleUuid, projectUuid, projectUuid, Qualifiers.MODULE, "Module");
    insertComponent(3, subModule1Uuid, moduleUuid, projectUuid, Qualifiers.MODULE, "Submodule 1");
    insertComponent(4, subModule2Uuid, moduleUuid, projectUuid, Qualifiers.MODULE, "Submodule 2");
    insertProperty(1, 1, "sonar.coverage.exclusions", "Proj1.java");
    insertProperty(2, 1, "sonar.cpd.exclusions", "Proj2.java");
    insertProperty(3, 2, "sonar.coverage.exclusions", "ModuleA.java");
    insertProperty(4, 2, "sonar.cpd.exclusions", "ModuleB.java");
    insertProperty(5, 3, "sonar.coverage.exclusions", "Module1A.java");
    insertProperty(6, 3, "sonar.cpd.exclusions", "Moddule1B.java");
    insertProperty(7, 4, "sonar.coverage.exclusions", "Module2A.java");
    insertProperty(8, 4, "sonar.cpd.exclusions", "Module2B.java");

    String project2Uuid = uuidFactory.create();
    insertComponent(5, project2Uuid, null, project2Uuid, Qualifiers.PROJECT, "Single module project");
    insertProperty(9, 5, "sonar.coverage.exclusions", "SingleModuleA.java");
    insertProperty(10, 5, "sonar.cp.exclusions", "SingleModuleB.java");

    String project3Uuid = uuidFactory.create();
    String singleModuleUuid = uuidFactory.create();
    insertComponent(6, project3Uuid, null, project3Uuid, Qualifiers.PROJECT, "Another multi-module project");
    insertComponent(7, singleModuleUuid, project3Uuid, project3Uuid, Qualifiers.MODULE, "Module X");
    insertProperty(11, 6, "sonar.coverage.exclusions", "InRoot.java");
    insertProperty(12, 7, "sonar.coverage.exclusions", "InModule.java");
  }

  @Test
  public void migration() throws SQLException {
    underTest.execute();

    checkMigration();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    checkMigration();
  }

  private void checkMigration() {
    final ImmutableSet<Long> expectedRemainingPropertyResourceIds = ImmutableSet.of(1L, 5L, 6L);
    final String expectedProject1Property = "# previous settings for sub-project Multi-module project::Module\n" +
      "sonar.coverage.exclusions=ModuleA.java\n" +
      "sonar.cpd.exclusions=ModuleB.java\n" +
      "\n" +
      "# previous settings for sub-project Multi-module project::Submodule 1\n" +
      "sonar.coverage.exclusions=Module1A.java\n" +
      "sonar.cpd.exclusions=Moddule1B.java\n" +
      "\n" +
      "# previous settings for sub-project Multi-module project::Submodule 2\n" +
      "sonar.coverage.exclusions=Module2A.java\n" +
      "sonar.cpd.exclusions=Module2B.java\n";
    final String expectedProject2Property = "# previous settings for sub-project Another multi-module project::Module X\n" +
      "sonar.coverage.exclusions=InModule.java\n";

    List<Map<String, Object>> newProperties = db.select("select ID, TEXT_VALUE, CLOB_VALUE " +
      "from properties " +
      "where PROP_KEY='sonar.subprojects.settings.removed' " +
      "order by RESOURCE_ID");
    List<Map<String, Object>> remainingProperties = db.select("select ID from properties");
    List<Map<String, Object>> remainingPropertyResourceIds = db.select("select distinct RESOURCE_ID from properties");

    assertThat(newProperties).hasSize(2);
    Map<String, Object> project1Property = newProperties.get(0);
    assertThat(project1Property.get("TEXT_VALUE")).isNull();
    assertThat(project1Property.get("CLOB_VALUE")).isEqualTo(expectedProject1Property);
    Map<String, Object> project2Property = newProperties.get(1);
    assertThat(project2Property.get("TEXT_VALUE")).isNull();
    assertThat(project2Property.get("CLOB_VALUE")).isEqualTo(expectedProject2Property);
    assertThat(remainingProperties).hasSize(7);
    assertThat(remainingPropertyResourceIds).hasSize(3);
    assertThat(remainingPropertyResourceIds).allMatch(entry -> expectedRemainingPropertyResourceIds.contains(entry.get("RESOURCE_ID")));
  }

  private void insertComponent(long id, String uuid, @Nullable String rootUuid, String projectUuid, String qualifier, String name) {
    db.executeInsert(
      "projects",
      "ID", valueOf(id),
      "UUID", uuid,
      "ROOT_UUID", rootUuid,
      "PROJECT_UUID", projectUuid,
      "SCOPE", Scopes.PROJECT,
      "QUALIFIER", qualifier,
      "NAME", name);
  }

  private void insertProperty(long id, long componentId, String key, String value) {
    db.executeInsert(
      "properties",
      "ID", valueOf(id),
      "RESOURCE_ID", componentId,
      "PROP_KEY", key,
      "TEXT_VALUE", value,
      "IS_EMPTY", false);
  }
}

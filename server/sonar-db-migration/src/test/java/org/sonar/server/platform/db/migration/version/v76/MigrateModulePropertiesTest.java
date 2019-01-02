/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v76;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
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
import static org.sonar.server.platform.db.migration.version.v76.MigrateModuleProperties.NEW_PROPERTY_NAME;

public class MigrateModulePropertiesTest {
  private static final long NOW = 50_000_000_000L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateModulePropertiesTest.class, "schema.sql");
  private System2 system2 = new TestSystem2().setNow(NOW);
  private UuidFactory uuidFactory = new SequenceUuidFactory();
  private MigrateModuleProperties underTest = new MigrateModuleProperties(db.database(), system2);

  @Test
  public void multi_module_project_migration() throws SQLException {
    setupMultiModuleProject();

    underTest.execute();

    verifyMultiModuleProjectMigration();
    assertThat(getRemainingProperties()).hasSize(3);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    setupMultiModuleProject();

    underTest.execute();
    underTest.execute();

    verifyMultiModuleProjectMigration();
    assertThat(getRemainingProperties()).hasSize(3);
  }

  @Test
  public void single_module_project_migration() throws SQLException {
    String project2Uuid = uuidFactory.create();
    insertComponent(5, project2Uuid, null, project2Uuid, Qualifiers.PROJECT, "Single module project");
    insertProperty(9, 5, "sonar.coverage.exclusions", "SingleModuleA.java", null);
    insertProperty(10, 5, "sonar.cp.exclusions", "SingleModuleB.java", null);

    underTest.execute();

    List<Map<String, Object>> properties = db.select(String.format("select ID, TEXT_VALUE, CLOB_VALUE " +
      "from properties " +
      "where PROP_KEY='%s' and RESOURCE_ID = 5", NEW_PROPERTY_NAME));
    assertThat(properties).hasSize(0);
    List<Map<String, Object>> remainingProperties = db.select("select ID from properties");
    assertThat(remainingProperties).hasSize(2);
  }

  @Test
  public void properties_do_not_leak_between_projects() throws SQLException {
    setupMultiModuleProject();
    setupSecondMultiModuleProject();

    underTest.execute();

    verifyMultiModuleProjectMigration();
    verifySecondMultiModuleProjectMigration();
    assertThat(getRemainingProperties()).hasSize(5);
  }

  private List<Map<String, Object>> getRemainingProperties() {
    return db.select("select ID from properties");
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

  private void insertProperty(long id, long componentId, String key, String value, Long userId) {
    db.executeInsert(
      "properties",
      "ID", valueOf(id),
      "RESOURCE_ID", componentId,
      "PROP_KEY", key,
      "TEXT_VALUE", value,
      "IS_EMPTY", false,
      "USER_ID", userId);
  }

  private void setupMultiModuleProject() {
    String projectUuid = uuidFactory.create();
    String moduleUuid = uuidFactory.create();
    String subModule1Uuid = uuidFactory.create();
    String subModule2Uuid = uuidFactory.create();
    String subModule3Uuid = uuidFactory.create();
    String subModule4Uuid = uuidFactory.create();
    String subModule5Uuid = uuidFactory.create();
    insertComponent(1, projectUuid, null, projectUuid, Qualifiers.PROJECT, "Multi-module project");
    insertComponent(2, moduleUuid, projectUuid, projectUuid, Qualifiers.MODULE, "Module");
    insertComponent(3, subModule1Uuid, moduleUuid, projectUuid, Qualifiers.MODULE, "Submodule 1");
    insertComponent(4, subModule2Uuid, moduleUuid, projectUuid, Qualifiers.MODULE, "Submodule 2");
    insertComponent(5, subModule3Uuid, moduleUuid, projectUuid, Qualifiers.MODULE, "Submodule with reset property");
    insertComponent(6, subModule4Uuid, moduleUuid, projectUuid, Qualifiers.MODULE, "Submodule without properties");
    insertComponent(7, subModule5Uuid, moduleUuid, projectUuid, Qualifiers.MODULE, "Submodule with user property");
    insertProperty(1, 1, "sonar.coverage.exclusions", "Proj1.java", null);
    insertProperty(2, 1, "sonar.cpd.exclusions", "Proj2.java", null);
    insertProperty(3, 2, "sonar.coverage.exclusions", "ModuleA.java", null);
    insertProperty(4, 2, "sonar.cpd.exclusions", "ModuleB.java", null);
    insertProperty(5, 3, "sonar.coverage.exclusions", "Module1A.java", null);
    insertProperty(6, 3, "sonar.cpd.exclusions", "Moddule1B.java", null);
    insertProperty(7, 4, "sonar.coverage.exclusions", "Module2A.java", null);
    insertProperty(8, 4, "sonar.cpd.exclusions", "Module2B.java", null);
    insertProperty(9, 5, "sonar.coverage.exclusions", null, null);
    insertProperty(10, 7, "favourite", null, 5L);
  }

  private void verifyMultiModuleProjectMigration() {
    final String expectedResult = "# Settings from 'Multi-module project::Module'\n" +
      "sonar.coverage.exclusions=ModuleA.java\n" +
      "sonar.cpd.exclusions=ModuleB.java\n" +
      "\n" +
      "# Settings from 'Multi-module project::Submodule 1'\n" +
      "sonar.coverage.exclusions=Module1A.java\n" +
      "sonar.cpd.exclusions=Moddule1B.java\n" +
      "\n" +
      "# Settings from 'Multi-module project::Submodule 2'\n" +
      "sonar.coverage.exclusions=Module2A.java\n" +
      "sonar.cpd.exclusions=Module2B.java\n" +
      "\n" +
      "# Settings from 'Multi-module project::Submodule with reset property'\n" +
      "sonar.coverage.exclusions=\n";

    List<Map<String, Object>> properties = db
      .select(String.format("select ID, TEXT_VALUE, CLOB_VALUE from properties where PROP_KEY='%s' and RESOURCE_ID = 1",
        NEW_PROPERTY_NAME));

    assertThat(properties).hasSize(1);
    Map<String, Object> project1Property = properties.get(0);
    assertThat(project1Property.get("TEXT_VALUE")).isNull();
    assertThat(project1Property.get("CLOB_VALUE")).isEqualTo(expectedResult);
  }

  private void setupSecondMultiModuleProject() {
    String project3Uuid = uuidFactory.create();
    String singleModuleUuid = uuidFactory.create();
    insertComponent(8, project3Uuid, null, project3Uuid, Qualifiers.PROJECT, "Another multi-module project");
    insertComponent(9, singleModuleUuid, project3Uuid, project3Uuid, Qualifiers.MODULE, "Module X");
    insertProperty(11, 8, "sonar.coverage.exclusions", "InRoot.java", null);
    insertProperty(12, 9, "sonar.coverage.exclusions", "InModule.java", null);
  }

  private void verifySecondMultiModuleProjectMigration() {
    final String expectedResult = "# Settings from 'Another multi-module project::Module X'\n" +
      "sonar.coverage.exclusions=InModule.java\n";

    List<Map<String, Object>> properties = db.select(String.format("select ID, TEXT_VALUE, CLOB_VALUE " +
      "from properties " +
      "where PROP_KEY='%s' and RESOURCE_ID = 8", NEW_PROPERTY_NAME));

    assertThat(properties).hasSize(1);
    Map<String, Object> project2Property = properties.get(0);
    assertThat(project2Property.get("TEXT_VALUE")).isNull();
    assertThat(project2Property.get("CLOB_VALUE")).isEqualTo(expectedResult);
  }
}

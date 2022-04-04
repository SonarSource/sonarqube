/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.platform.db.migration.version.v86.MigrateApplicationDefinitionsFromXmlToDb.TEXT_VALUE_MAX_LENGTH;

public class MigrateApplicationDefinitionsFromXmlToDbTest {
  private static final String QUALIFIER_PROJECT = "TRK";
  private static final String QUALIFIER_APP = "APP";
  private static final long NOW = 100_000_000_000L;

  private static final String PROJECT_1_UUID = "proj1-uuid";
  private static final String PROJECT_1_MASTER_BRANCH_UUID = "proj1-master-uuid";
  private static final String PROJECT_1_BRANCH_1_UUID = "proj1-branch1-uuid";
  private static final String PROJECT_1_BRANCH_2_UUID = "proj1-branch2-uuid";
  private static final String PROJECT_2_UUID = "proj2-uuid";
  private static final String PROJECT_2_MASTER_BRANCH_UUID = "proj2-master-uuid";
  private static final String PROJECT_2_BRANCH_1_UUID = "proj2-branch1-uuid";
  private static final String APP_1_UUID = "app1-uuid";
  private static final String APP_1_MASTER_BRANCH_UUID = "app1-master-uuid";
  private static final String APP_1_BRANCH_1_UUID = "app1-branch1-uuid";
  private static final String APP_1_BRANCH_2_UUID = "app1-branch2-uuid";
  private static final String APP_2_UUID = "app2-uuid";
  private static final String APP_2_MASTER_BRANCH_UUID = "app2-master-uuid";
  private static final String APP_2_BRANCH_1_UUID = "app2-branch1-uuid";
  private static final String EMPTY_XML = "<views></views>";

  private static final String EMPTY_APP_XML = "<views>\n" +
    "    <vw key=\"app1-key\" def=\"false\">\n" +
    "        <name><![CDATA[app1]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[APP]]></qualifier>\n" +
    "    </vw>\n" +
    "</views>";

  private static final String APP_WITH_NO_BRANCHES_XML = "<views>\n" +
    "    <vw key=\"app1-key\" def=\"false\">\n" +
    "        <name><![CDATA[app1-key]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[APP]]></qualifier>\n" +
    "        <p>proj1-key</p>\n" +
    "        <p>proj2-key</p>\n" +
    "    </vw>\n" +
    "</views>";

  private static final String APP_WITH_DUPLICATED_PROJECTS_XML = "<views>\n" +
    "    <vw key=\"app1-key\" def=\"false\">\n" +
    "        <name><![CDATA[app1-key]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[APP]]></qualifier>\n" +
    "        <p>proj1-key</p>\n" +
    "        <p>proj1-key</p>\n" +
    "        <branch key=\"app1-branch1\">\n" +
    "            <p branch=\"proj1-branch-1\">proj1-key</p>\n" +
    "        </branch>\n" +
    "        <branch key=\"app1-branch2\">\n" +
    "            <p branch=\"proj1-branch-2\">proj1-key</p>\n" +
    "        </branch>\n" +
    "    </vw>\n" +
    "</views>";

  private static final String COMPLEX_XML_BEFORE = "<views>\n" +
    "    <vw key=\"app1-key\" def=\"false\">\n" +
    "        <name><![CDATA[app1-key]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[APP]]></qualifier>\n" +
    "        <p>proj1-key</p>\n" +
    "        <p>proj2-key</p>\n" +
    "        <branch key=\"app1-branch1\">\n" +
    "            <p branch=\"proj1-branch-1\">proj1-key</p>\n" +
    "            <p branch=\"m1\">proj2-key</p>\n" +
    "        </branch>\n" +
    "        <branch key=\"app1-branch2\">\n" +
    "            <p branch=\"proj1-branch-2\">proj1-key</p>\n" +
    "            <p branch=\"m1\">proj2-key</p>\n" +
    "        </branch>\n" +
    "    </vw>\n" +
    "    <vw key=\"app2-key\" def=\"false\">\n" +
    "        <name><![CDATA[app2-key]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[APP]]></qualifier>\n" +
    "        <p>proj1-key</p>\n" +
    "        <p>proj2-key</p>\n" +
    "        <branch key=\"m1\">\n" +
    "            <p branch=\"proj1-branch-1\">proj1-key</p>\n" +
    "            <p branch=\"m1\">proj2-key</p>\n" +
    "        </branch>\n" +
    "    </vw>\n" +
    "    <vw key=\"port1\" def=\"true\">\n" +
    "        <name><![CDATA[port1]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port2\" def=\"false\">\n" +
    "        <name><![CDATA[port2]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <vw-ref><![CDATA[app1-key]]></vw-ref>\n" +
    "        <vw-ref><![CDATA[port1]]></vw-ref>\n" +
    "    </vw>\n" +
    "    <vw key=\"port3\" def=\"false\">\n" +
    "        <name><![CDATA[port3]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <p>proj1-key</p>\n" +
    "    </vw>\n" +
    "    <vw key=\"port4\" def=\"false\">\n" +
    "        <name><![CDATA[port4]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"portx\" def=\"false\" root=\"port2\" parent=\"port2\">\n" +
    "        <name><![CDATA[portx]]></name>\n" +
    "    </vw>\n" +
    "    <vw key=\"port5\" def=\"false\">\n" +
    "        <name><![CDATA[port5]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <tagsAssociation>\n" +
    "            <tag>tag1</tag>\n" +
    "        </tagsAssociation>\n" +
    "    </vw>\n" +
    "    <vw key=\"port6\" def=\"false\">\n" +
    "        <name><![CDATA[port6]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <regexp><![CDATA[.*oj.*]]></regexp>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port7\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "</views>";

  private static final String COMPLEX_XML_AFTER = "<views>\n" +
    "    <vw key=\"port1\" def=\"true\">\n" +
    "        <name><![CDATA[port1]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port2\" def=\"false\">\n" +
    "        <name><![CDATA[port2]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <vw-ref><![CDATA[app1-key]]></vw-ref>\n" +
    "        <vw-ref><![CDATA[port1]]></vw-ref>\n" +
    "    </vw>\n" +
    "    <vw key=\"port3\" def=\"false\">\n" +
    "        <name><![CDATA[port3]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <p>proj1-key</p>\n" +
    "    </vw>\n" +
    "    <vw key=\"port4\" def=\"false\">\n" +
    "        <name><![CDATA[port4]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"portx\" def=\"false\" root=\"port2\" parent=\"port2\">\n" +
    "        <name><![CDATA[portx]]></name>\n" +
    "    </vw>\n" +
    "    <vw key=\"port5\" def=\"false\">\n" +
    "        <name><![CDATA[port5]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <tagsAssociation>\n" +
    "            <tag>tag1</tag>\n" +
    "        </tagsAssociation>\n" +
    "    </vw>\n" +
    "    <vw key=\"port6\" def=\"false\">\n" +
    "        <name><![CDATA[port6]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <regexp><![CDATA[.*oj.*]]></regexp>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port7\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "</views>";

  private static final String LARGE_XML_BEFORE_AND_AFTER = "<views>\n" +
    "    <vw key=\"port1\" def=\"true\">\n" +
    "        <name><![CDATA[port1]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port2\" def=\"false\">\n" +
    "        <name><![CDATA[port2]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <vw-ref><![CDATA[app1-key]]></vw-ref>\n" +
    "        <vw-ref><![CDATA[port1]]></vw-ref>\n" +
    "    </vw>\n" +
    "    <vw key=\"port3\" def=\"false\">\n" +
    "        <name><![CDATA[port3]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <p>proj1-key</p>\n" +
    "    </vw>\n" +
    "    <vw key=\"port4\" def=\"false\">\n" +
    "        <name><![CDATA[port4]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"portx\" def=\"false\" root=\"port2\" parent=\"port2\">\n" +
    "        <name><![CDATA[portx]]></name>\n" +
    "    </vw>\n" +
    "    <vw key=\"port5\" def=\"false\">\n" +
    "        <name><![CDATA[port5]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <tagsAssociation>\n" +
    "            <tag>tag1</tag>\n" +
    "        </tagsAssociation>\n" +
    "    </vw>\n" +
    "    <vw key=\"port6\" def=\"false\">\n" +
    "        <name><![CDATA[port6]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <regexp><![CDATA[.*oj.*]]></regexp>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port7\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port8\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port9\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port10\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port11\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port12\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port13\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port14\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port15\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port16\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port17\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port18\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port19\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port20\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "    <vw key=\"port21\" def=\"false\">\n" +
    "        <name><![CDATA[port7]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <tag_key><![CDATA[business_value]]></tag_key>\n" +
    "        <tag_value><![CDATA[12]]></tag_value>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "</views>";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateApplicationDefinitionsFromXmlToDbTest.class, "schema.sql");

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final System2 system2 = new TestSystem2().setNow(NOW);
  private final MigrationStep underTest = new MigrateApplicationDefinitionsFromXmlToDb(db.database(), uuidFactory, system2);

  @Test
  public void does_nothing_when_no_views_def_property() throws SQLException {
    setupProjectsAndApps();

    underTest.execute();

    assertThat(db.countSql("select count(*) from app_projects")).isZero();
    assertThat(db.countSql("select count(*) from app_branch_project_branch")).isZero();
    assertThat(db.countSql("select count(*) from internal_properties where kee='views.def'")).isZero();
  }

  @Test
  public void does_nothing_when_views_def_property_empty_string() throws SQLException {
    setupProjectsAndApps();
    insertViewsDefInternalProperty("");

    underTest.execute();

    assertThat(db.countSql("select count(*) from app_projects")).isZero();
    assertThat(db.countSql("select count(*) from app_branch_project_branch")).isZero();
  }

  @Test
  public void does_nothing_when_views_def_property_empty_views_content() throws SQLException {
    setupProjectsAndApps();
    insertViewsDefInternalProperty(EMPTY_XML);

    underTest.execute();

    assertThat(db.countSql("select count(*) from app_projects")).isZero();
    assertThat(db.countSql("select count(*) from app_branch_project_branch")).isZero();
  }

  @Test
  public void throws_ISE_when_views_def_property_does_not_pass_validation() {
    setupProjectsAndApps();
    insertViewsDefInternalProperty("abcdefghi");

    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to migrate views definitions property.");

    assertThat(db.countSql("select count(*) from app_projects")).isZero();
    assertThat(db.countSql("select count(*) from app_branch_project_branch")).isZero();
  }

  @Test
  public void migrates_applications_to_new_tables() throws SQLException {
    setupProjectsAndApps();
    insertViewsDefInternalProperty(COMPLEX_XML_BEFORE);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, PROJECT_2_UUID, APP_1_UUID, APP_2_UUID);
    assertThat(db.select("select application_uuid, project_uuid from app_projects"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID),
        tuple(APP_2_UUID, PROJECT_2_UUID));
    assertThat(db.select("select application_uuid, project_uuid, application_branch_uuid, project_branch_uuid from app_branch_project_branch"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"), r -> r.get("APPLICATION_BRANCH_UUID"), r -> r.get("PROJECT_BRANCH_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID, APP_1_BRANCH_1_UUID, PROJECT_2_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_2_UUID, PROJECT_1_BRANCH_2_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID, APP_1_BRANCH_2_UUID, PROJECT_2_BRANCH_1_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID, APP_2_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_2_UUID, PROJECT_2_UUID, APP_2_BRANCH_1_UUID, PROJECT_2_BRANCH_1_UUID));
  }

  @Test
  public void migrates_applications_handling_project_duplications_to_new_tables() throws SQLException {
    setupProjectsAndApps();
    insertViewsDefInternalProperty(APP_WITH_DUPLICATED_PROJECTS_XML);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, PROJECT_2_UUID, APP_1_UUID, APP_2_UUID);
    assertThat(db.select("select application_uuid, project_uuid from app_projects"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"))
      .containsExactly(tuple(APP_1_UUID, PROJECT_1_UUID));
    assertThat(db.select("select application_uuid, project_uuid, application_branch_uuid, project_branch_uuid from app_branch_project_branch"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"), r -> r.get("APPLICATION_BRANCH_UUID"), r -> r.get("PROJECT_BRANCH_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_2_UUID, PROJECT_1_BRANCH_2_UUID));
  }

  @Test
  public void migration_is_resilient() throws SQLException {
    setupProjectsAndApps();
    insertViewsDefInternalProperty(COMPLEX_XML_BEFORE);

    // first attempt
    underTest.execute();

    // xml stays the same (stopped during migration)
    updateViewsDefInternalProperty(COMPLEX_XML_BEFORE);

    // second attempt should not fail
    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, PROJECT_2_UUID, APP_1_UUID, APP_2_UUID);
    assertThat(db.select("select application_uuid, project_uuid from app_projects"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID),
        tuple(APP_2_UUID, PROJECT_2_UUID));
    assertThat(db.select("select application_uuid, project_uuid, application_branch_uuid, project_branch_uuid from app_branch_project_branch"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"), r -> r.get("APPLICATION_BRANCH_UUID"), r -> r.get("PROJECT_BRANCH_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID, APP_1_BRANCH_1_UUID, PROJECT_2_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_2_UUID, PROJECT_1_BRANCH_2_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID, APP_1_BRANCH_2_UUID, PROJECT_2_BRANCH_1_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID, APP_2_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_2_UUID, PROJECT_2_UUID, APP_2_BRANCH_1_UUID, PROJECT_2_BRANCH_1_UUID));
  }

  @Test
  public void migrates_applications_without_application_branches_to_new_tables() throws SQLException {
    setupFullProject1();
    setupProject2();
    setupApp1WithNoBranches();
    insertViewsDefInternalProperty(APP_WITH_NO_BRANCHES_XML);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, PROJECT_2_UUID, APP_1_UUID);
    assertThat(db.select("select application_uuid, project_uuid from app_projects"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID));
    assertThat(db.countSql("select count(*) from app_branch_project_branch")).isZero();
  }

  @Test
  public void skips_apps_that_exist_in_the_definition_but_does_not_exist_in_db() throws SQLException {
    setupFullProject1();
    insertViewsDefInternalProperty(EMPTY_APP_XML);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID);
    assertThat(db.countSql("select count(*) from app_projects")).isZero();
    assertThat(db.countSql("select count(*) from app_branch_project_branch")).isZero();
  }

  @Test
  public void migrates_app_with_0_projects_in_views_definition() throws SQLException {
    setupFullProject1();
    setupProject2();
    setupApp1WithNoBranches();
    insertViewsDefInternalProperty(EMPTY_APP_XML);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, PROJECT_2_UUID, APP_1_UUID);
    assertThat(db.countSql("select count(*) from app_projects")).isZero();
    assertThat(db.countSql("select count(*) from app_branch_project_branch")).isZero();
  }

  @Test
  public void skips_apps_that_are_present_in_views_definition_but_not_in_db() throws SQLException {
    setupFullProject1();
    setupProject2();
    setupApp1WithTwoBranches();
    insertViewsDefInternalProperty(COMPLEX_XML_BEFORE);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, PROJECT_2_UUID, APP_1_UUID);
    assertThat(db.select("select application_uuid, project_uuid from app_projects"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID));
    assertThat(db.select("select application_uuid, project_uuid, application_branch_uuid, project_branch_uuid from app_branch_project_branch"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"), r -> r.get("APPLICATION_BRANCH_UUID"), r -> r.get("PROJECT_BRANCH_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID, APP_1_BRANCH_1_UUID, PROJECT_2_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_2_UUID, PROJECT_1_BRANCH_2_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID, APP_1_BRANCH_2_UUID, PROJECT_2_BRANCH_1_UUID));
  }

  @Test
  public void skips_app_branches_that_are_present_in_views_definition_but_not_in_db() throws SQLException {
    setupFullProject1();
    setupProject2();
    setupApp1WithNoBranches();
    setupApp2();
    insertViewsDefInternalProperty(COMPLEX_XML_BEFORE);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, PROJECT_2_UUID, APP_1_UUID, APP_2_UUID);
    assertThat(db.select("select application_uuid, project_uuid from app_projects"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID),
        tuple(APP_2_UUID, PROJECT_2_UUID));
    assertThat(db.select("select application_uuid, project_uuid, application_branch_uuid, project_branch_uuid from app_branch_project_branch"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"), r -> r.get("APPLICATION_BRANCH_UUID"), r -> r.get("PROJECT_BRANCH_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_2_UUID, PROJECT_1_UUID, APP_2_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_2_UUID, PROJECT_2_UUID, APP_2_BRANCH_1_UUID, PROJECT_2_BRANCH_1_UUID));
  }

  @Test
  public void skips_projects_that_are_present_in_apps_views_definitions_but_not_in_db() throws SQLException {
    setupPartialProject1();
    setupApp1WithTwoBranches();
    setupApp2();
    insertViewsDefInternalProperty(COMPLEX_XML_BEFORE);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, APP_1_UUID, APP_2_UUID);
    assertThat(db.select("select application_uuid, project_uuid from app_projects"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID));
    assertThat(db.select("select application_uuid, project_uuid, application_branch_uuid, project_branch_uuid from app_branch_project_branch"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"), r -> r.get("APPLICATION_BRANCH_UUID"), r -> r.get("PROJECT_BRANCH_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID, APP_2_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID));
  }

  @Test
  public void skips_projects_branches_that_are_present_in_apps_views_definitions_but_not_in_db() throws SQLException {
    setupPartialProject1();
    setupProject2();
    setupApp1WithTwoBranches();
    setupApp2();
    insertViewsDefInternalProperty(COMPLEX_XML_BEFORE);

    underTest.execute();

    assertThat(db.select("select uuid from projects"))
      .extracting(r -> r.get("UUID"))
      .containsExactlyInAnyOrder(PROJECT_1_UUID, PROJECT_2_UUID, APP_1_UUID, APP_2_UUID);
    assertThat(db.select("select application_uuid, project_uuid from app_projects"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID),
        tuple(APP_2_UUID, PROJECT_2_UUID));
    assertThat(db.select("select application_uuid, project_uuid, application_branch_uuid, project_branch_uuid from app_branch_project_branch"))
      .extracting(r -> r.get("APPLICATION_UUID"), r -> r.get("PROJECT_UUID"), r -> r.get("APPLICATION_BRANCH_UUID"), r -> r.get("PROJECT_BRANCH_UUID"))
      .containsExactlyInAnyOrder(
        tuple(APP_1_UUID, PROJECT_1_UUID, APP_1_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID, APP_1_BRANCH_1_UUID, PROJECT_2_BRANCH_1_UUID),
        tuple(APP_1_UUID, PROJECT_2_UUID, APP_1_BRANCH_2_UUID, PROJECT_2_BRANCH_1_UUID),
        tuple(APP_2_UUID, PROJECT_1_UUID, APP_2_BRANCH_1_UUID, PROJECT_1_BRANCH_1_UUID),
        tuple(APP_2_UUID, PROJECT_2_UUID, APP_2_BRANCH_1_UUID, PROJECT_2_BRANCH_1_UUID));
  }

  @Test
  public void removes_application_definitions_from_xml() throws SQLException {
    setupProjectsAndApps();
    insertViewsDefInternalProperty(COMPLEX_XML_BEFORE);

    underTest.execute();

    assertThat(db.countSql("select count(*) from internal_properties where kee='views.def'")).isOne();
    assertViewsXmlDefinitionSimilar(COMPLEX_XML_AFTER, false);
  }

  @Test
  public void removes_application_definitions_from_large_xmls() throws SQLException {
    setupProjectsAndApps();
    insertViewsDefInternalProperty(LARGE_XML_BEFORE_AND_AFTER);

    underTest.execute();

    assertThat(db.countSql("select count(*) from internal_properties where kee='views.def'")).isOne();
    assertViewsXmlDefinitionSimilar(LARGE_XML_BEFORE_AND_AFTER, true);
  }

  @Test
  public void does_not_change_the_xml_if_there_are_no_application_definitions() throws SQLException {
    setupProjectsAndApps();
    insertViewsDefInternalProperty(EMPTY_XML);

    underTest.execute();

    assertThat(db.countSql("select count(*) from internal_properties where kee='views.def'")).isOne();
    assertViewsXmlDefinitionSimilar(EMPTY_XML, false);
  }

  private void setupProjectsAndApps() {
    setupFullProject1();
    setupProject2();
    setupApp1WithTwoBranches();
    setupApp2();
  }

  private void setupFullProject1() {
    setupPartialProject1();
    insertBranch(PROJECT_1_BRANCH_2_UUID, PROJECT_1_UUID, "proj1-branch-2");
  }

  private void setupPartialProject1() {
    insertProject(PROJECT_1_UUID, "proj1-key", QUALIFIER_PROJECT);
    insertBranch(PROJECT_1_MASTER_BRANCH_UUID, PROJECT_1_UUID, "master");
    insertBranch(PROJECT_1_BRANCH_1_UUID, PROJECT_1_UUID, "proj1-branch-1");
  }

  private void setupProject2() {
    insertProject(PROJECT_2_UUID, "proj2-key", QUALIFIER_PROJECT);
    insertBranch(PROJECT_2_MASTER_BRANCH_UUID, PROJECT_2_UUID, "master");
    insertBranch(PROJECT_2_BRANCH_1_UUID, PROJECT_2_UUID, "m1");
  }

  private void setupApp1WithNoBranches() {
    insertProject(APP_1_UUID, "app1-key", QUALIFIER_APP);
    insertBranch(APP_1_MASTER_BRANCH_UUID, APP_1_UUID, "master");
  }

  private void setupApp1WithOneBranch() {
    setupApp1WithNoBranches();
    insertBranch(APP_1_BRANCH_1_UUID, APP_1_UUID, "app1-branch1");
  }

  private void setupApp1WithTwoBranches() {
    setupApp1WithOneBranch();
    insertBranch(APP_1_BRANCH_2_UUID, APP_1_UUID, "app1-branch2");
  }

  private void setupApp2() {
    insertProject(APP_2_UUID, "app2-key", QUALIFIER_APP);
    insertBranch(APP_2_MASTER_BRANCH_UUID, APP_2_UUID, "master");
    insertBranch(APP_2_BRANCH_1_UUID, APP_2_UUID, "m1");
  }

  private void insertViewsDefInternalProperty(@Nullable String xml) {
    String valueColumn = "text_value";
    if (xml != null && xml.length() > TEXT_VALUE_MAX_LENGTH) {
      valueColumn = "clob_value";
    }

    db.executeInsert("internal_properties",
      "kee", "views.def",
      "is_empty", "false",
      valueColumn, xml,
      "created_at", system2.now());
  }

  private void updateViewsDefInternalProperty(@Nullable String xml) {
    db.executeUpdateSql("update internal_properties set text_value = ? where kee = 'views.def'",
      xml);
  }

  private void insertProject(String uuid, String key, String qualifier) {
    db.executeInsert("PROJECTS",
      "UUID", uuid,
      "KEE", key,
      "QUALIFIER", qualifier,
      "ORGANIZATION_UUID", uuid + "-key",
      "TAGS", "tag1",
      "PRIVATE", Boolean.toString(false),
      "UPDATED_AT", System2.INSTANCE.now());
  }

  private void insertBranch(String uuid, String projectUuid, String key) {
    db.executeInsert(
      "PROJECT_BRANCHES",
      "UUID", uuid,
      "PROJECT_UUID", projectUuid,
      "KEE", key,
      "BRANCH_TYPE", "BRANCH",
      "MERGE_BRANCH_UUID", null,
      "CREATED_AT", System2.INSTANCE.now(),
      "UPDATED_AT", System2.INSTANCE.now(),
      "NEED_ISSUE_SYNC", Boolean.toString(false));
  }

  private void assertViewsXmlDefinitionSimilar(final String expectedValue, final boolean expectClob) {
    Map<String, Object> result = db.selectFirst("select text_value, clob_value from internal_properties where kee='views.def'");
    String textValue = (String) result.get("TEXT_VALUE");
    String clobValue = (String) result.get("CLOB_VALUE");

    String existingValue;
    if (expectClob) {
      existingValue = clobValue;
      assertThat(textValue).isNull();
    } else {
      existingValue = textValue;
      assertThat(clobValue).isNull();
    }

    Diff diff = DiffBuilder
      .compare(Input.fromString(expectedValue))
      .withTest(Input.fromString(existingValue))
      .ignoreWhitespace()
      .ignoreComments()
      .checkForSimilar()
      .build();
    assertThat(diff.getDifferences())
      .as(expectedValue)
      .isEmpty();
  }
}

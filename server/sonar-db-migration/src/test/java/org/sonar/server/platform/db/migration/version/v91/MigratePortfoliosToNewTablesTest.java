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
package org.sonar.server.platform.db.migration.version.v91;

import java.sql.SQLException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.platform.db.migration.version.v91.MigratePortfoliosToNewTables.PORTFOLIO_CONSISTENCY_ERROR;
import static org.sonar.server.platform.db.migration.version.v91.MigratePortfoliosToNewTables.PORTFOLIO_PARENT_NOT_FOUND;
import static org.sonar.server.platform.db.migration.version.v91.MigratePortfoliosToNewTables.PORTFOLIO_ROOT_NOT_FOUND;

public class MigratePortfoliosToNewTablesTest {
  static final int TEXT_VALUE_MAX_LENGTH = 4000;
  private static final long NOW = 10_000_000L;

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(MigratePortfoliosToNewTablesTest.class, "schema.sql");

  @Rule
  public LogTester logTester = new LogTester();

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final System2 system2 = new TestSystem2().setNow(NOW);

  private final DataChange underTest = new MigratePortfoliosToNewTables(db.database(), uuidFactory, system2);

  private final String SIMPLE_XML_ONE_PORTFOLIO = "<views>" +
    "<vw key=\"port1\" def=\"false\">\n" +
    "        <name><![CDATA[name]]></name>\n" +
    "        <desc><![CDATA[description]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "<vw key=\"port2\" def=\"false\">\n" +
    "        <name><![CDATA[name2]]></name>\n" +
    "        <desc><![CDATA[description2]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "</views>";

  private final String SIMPLE_XML_ONLY_PORTFOLIOS = "<views>" +
    "<vw key=\"port1\" def=\"true\">\n" +
    "        <name><![CDATA[port1]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "<vw key=\"port3\" def=\"false\">\n" +
    "        <name><![CDATA[port3]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "<vw key=\"port2\" def=\"false\">\n" +
    "        <name><![CDATA[port2]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <vw-ref><![CDATA[port1]]></vw-ref>\n" +
    "        <vw-ref><![CDATA[port3]]></vw-ref>\n" +
    "    </vw>\n" +
    "</views>";

  private final String SIMPLE_XML_PORTFOLIOS_AND_APP = "<views>" +
    "<vw key=\"port1\" def=\"true\">\n" +
    "        <name><![CDATA[port1]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "<vw key=\"port3\" def=\"false\">\n" +
    "        <name><![CDATA[port3]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "    </vw>\n" +
    "<vw key=\"port2\" def=\"false\">\n" +
    "        <name><![CDATA[port2]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <vw-ref><![CDATA[port1]]></vw-ref>\n" +
    "        <vw-ref><![CDATA[port3]]></vw-ref>\n" +
    "    </vw>\n" +
    "<vw key=\"port4\" def=\"false\">\n" +
    "        <name><![CDATA[port2]]></name>\n" +
    "        <desc><![CDATA[]]></desc>\n" +
    "        <qualifier><![CDATA[VW]]></qualifier>\n" +
    "        <vw-ref><![CDATA[port2]]></vw-ref>\n" +
    "        <vw-ref><![CDATA[app1-key]]></vw-ref>\n" +
    "        <vw-ref><![CDATA[app2-key]]></vw-ref>\n" +
    "        <vw-ref><![CDATA[app3-key-not-existing]]></vw-ref>\n" +
    "    </vw>\n" +
    "</views>";

  private final String SIMPLE_XML_PORTFOLIOS_HIERARCHY = "<views>" +
    "  <vw key=\"port1\" def=\"false\">\n"
    + "    <name><![CDATA[port1]]></name>\n"
    + "    <desc><![CDATA[port1]]></desc>\n"
    + "    <qualifier><![CDATA[VW]]></qualifier>\n"
    + "  </vw>\n"
    + "  <vw key=\"port2\" def=\"false\" root=\"port1\" parent=\"port1\">\n"
    + "    <name><![CDATA[port2]]></name>\n"
    + "    <desc><![CDATA[port2]]></desc>\n"
    + "  </vw>\n"
    + "  <vw key=\"port3\" def=\"false\" root=\"port1\" parent=\"port1\">\n"
    + "    <name><![CDATA[port3]]></name>\n"
    + "    <desc><![CDATA[port3]]></desc>\n"
    + "  </vw>\n"
    + "  <vw key=\"port4\" def=\"false\" root=\"port1\" parent=\"port1\">\n"
    + "    <name><![CDATA[port4]]></name>\n"
    + "    <desc><![CDATA[port4]]></desc>\n"
    + "  </vw>\n"
    + "  <vw key=\"port5\" def=\"false\" root=\"port1\" parent=\"port2\">\n"
    + "    <name><![CDATA[port5]]></name>\n"
    + "  </vw>\n"
    + "  <vw key=\"port6\" def=\"false\" root=\"port1\" parent=\"port3\">\n"
    + "    <name><![CDATA[port6]]></name>\n"
    + "    <desc><![CDATA[port6]]></desc>\n"
    + "  </vw>\n"
    + "  <vw key=\"port7\" def=\"false\" root=\"port1\" parent=\"port3\">\n"
    + "    <name><![CDATA[port7]]></name>\n"
    + "    <desc><![CDATA[port7]]></desc>\n"
    + "  </vw>" +
    "</views>";

  private final String SIMPLE_XML_PORTFOLIOS_DIFFERENT_SELECTIONS = "<views>"
    + "  <vw key=\"port-regexp\" def=\"false\">\n"
    + "    <name><![CDATA[port-regexp]]></name>\n"
    + "    <desc><![CDATA[port-regexp]]></desc>\n"
    + "    <regexp><![CDATA[.*port.*]]></regexp>\n"
    + "    <qualifier><![CDATA[VW]]></qualifier>\n"
    + "  </vw>"
    + "  <vw key=\"port-tags\" def=\"false\">\n"
    + "    <name><![CDATA[port-tags]]></name>\n"
    + "    <desc><![CDATA[port-tags]]></desc>\n"
    + "    <qualifier><![CDATA[VW]]></qualifier>\n"
    + "    <tagsAssociation>\n"
    + "      <tag>tag1</tag>\n"
    + "      <tag>tag2</tag>\n"
    + "      <tag>tag3</tag>\n"
    + "    </tagsAssociation>\n"
    + "  </vw>"
    + "  <vw key=\"port-projects\" def=\"false\">\n"
    + "    <name><![CDATA[port-projects]]></name>\n"
    + "    <desc><![CDATA[port-projects]]></desc>\n"
    + "    <qualifier><![CDATA[VW]]></qualifier>\n"
    + "    <p>p1-key</p>\n"
    + "    <p>p2-key</p>\n"
    + "    <p>p3-key</p>\n"
    + "    <p>p4-key-not-existing</p>\n"
    + "  </vw>"
    + "</views>";

  private final String XML_PORTFOLIOS_LEGACY_SELECTIONS = "<views>"
    + "  <vw key=\"port-language\" def=\"false\">\n"
    + "    <name><![CDATA[port-language]]></name>\n"
    + "    <desc><![CDATA[port-language]]></desc>\n"
    + "    <language><![CDATA[javascript]]></language>\n"
    + "    <qualifier><![CDATA[VW]]></qualifier>\n"
    + "  </vw>"
    + "  <vw key=\"port-tag-value\" def=\"false\">\n"
    + "    <name><![CDATA[port-tag-value]]></name>\n"
    + "    <desc><![CDATA[port-tag-value]]></desc>\n"
    + "    <qualifier><![CDATA[VW]]></qualifier>\n"
    + "    <tag_key>tag-key</tag_key>\n"
    + "    <tag_value>tag-value</tag_value>\n"
    + "  </vw>"
    + "</views>";

  private final String SIMPLE_XML_PORTFOLIOS_WRONG_HIERARCHY = "<views>"
    + "  <vw key=\"port1\" def=\"false\">\n"
    + "    <name><![CDATA[port1]]></name>\n"
    + "    <desc><![CDATA[port1]]></desc>\n"
    + "    <qualifier><![CDATA[VW]]></qualifier>\n"
    + "  </vw>\n"
    + "  <vw key=\"port2\" def=\"false\" root=\"NON_EXISTING_ROOT\" parent=\"port1\">\n"
    + "    <name><![CDATA[port2]]></name>\n"
    + "    <desc><![CDATA[port2]]></desc>\n"
    + "  </vw>\n"
    + "  <vw key=\"port3\" def=\"false\" root=\"port1\" parent=\"NON_EXISTING_PARENT\">\n"
    + "    <name><![CDATA[port3]]></name>\n"
    + "    <desc><![CDATA[port3]]></desc>\n"
    + "  </vw>\n"
    + "  <vw key=\"port4\" def=\"false\" root=\"port3\" parent=\"port3\">\n"
    + "    <name><![CDATA[port4]]></name>\n"
    + "    <desc><![CDATA[port4]]></desc>\n"
    + "  </vw>\n"
    + "</views>";

  @Test
  public void does_not_fail_when_nothing_to_migrate() throws SQLException {
    assertThatCode(underTest::execute)
      .doesNotThrowAnyException();
  }

  @Test
  public void migrate_single_portfolio_with_default_visibility() throws SQLException {
    insertViewsDefInternalProperty(SIMPLE_XML_ONE_PORTFOLIO);
    insertDefaultVisibilityProperty(true);
    insertComponent("uuid", "port2", false);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThat(db.select("select kee, private, name, description, "
      + "selection_mode, selection_expression, "
      + "updated_at, created_at from portfolios"))
        .extracting(
          row -> row.get("KEE"),
          row -> row.get("PRIVATE"),
          row -> row.get("NAME"),
          row -> row.get("DESCRIPTION"),
          row -> row.get("SELECTION_MODE"),
          row -> row.get("SELECTION_EXPRESSION"),
          row -> row.get("UPDATED_AT"),
          row -> row.get("CREATED_AT"))
        .containsExactlyInAnyOrder(
          tuple("port1", true, "name", "description", "NONE", null, NOW, NOW),
          tuple("port2", false, "name2", "description2", "NONE", null, NOW, NOW));
  }

  @Test
  public void migrate_portfolios_should_assign_component_uuid() throws SQLException {
    insertViewsDefInternalProperty(SIMPLE_XML_ONE_PORTFOLIO);
    insertComponent("uuid1", "port1", true);
    insertComponent("uuid2", "port2", false);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThat(db.select("select uuid, kee, private, name, description, "
      + "selection_mode, selection_expression, "
      + "updated_at, created_at from portfolios"))
        .extracting(
          row -> row.get("UUID"),
          row -> row.get("KEE"),
          row -> row.get("PRIVATE"),
          row -> row.get("NAME"),
          row -> row.get("DESCRIPTION"),
          row -> row.get("SELECTION_MODE"),
          row -> row.get("SELECTION_EXPRESSION"),
          row -> row.get("UPDATED_AT"),
          row -> row.get("CREATED_AT"))
        .containsExactlyInAnyOrder(
          tuple("uuid1", "port1", true, "name", "description", "NONE", null, NOW, NOW),
          tuple("uuid2", "port2", false, "name2", "description2", "NONE", null, NOW, NOW));
  }

  @Test
  public void migrate_simple_xml_only_portfolios_references() throws SQLException {
    insertViewsDefInternalProperty(SIMPLE_XML_ONLY_PORTFOLIOS);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThat(db.select("select kee from portfolios"))
      .extracting(row -> row.get("KEE"))
      .containsExactlyInAnyOrder("port1", "port2", "port3");

    var portfolioKeyByUuid = db.select("select uuid, kee from portfolios").stream()
      .collect(Collectors.toMap(row -> row.get("KEE"), row -> row.get("UUID").toString()));

    String portfolio1Uuid = portfolioKeyByUuid.get("port1");
    String portfolio2Uuid = portfolioKeyByUuid.get("port2");
    String portfolio3Uuid = portfolioKeyByUuid.get("port3");

    assertThat(db.select("select portfolio_uuid, reference_uuid from portfolio_references"))
      .extracting(row -> row.get("PORTFOLIO_UUID"), row -> row.get("REFERENCE_UUID"))
      .containsExactlyInAnyOrder(tuple(portfolio2Uuid, portfolio1Uuid), tuple(portfolio2Uuid, portfolio3Uuid));
  }

  @Test
  public void migrate_simple_xml_portfolios_and_apps_references() throws SQLException {
    insertViewsDefInternalProperty(SIMPLE_XML_PORTFOLIOS_AND_APP);

    insertProject("app1-uuid", "app1-key", Qualifiers.APP);
    insertProject("app2-uuid", "app2-key", Qualifiers.APP);
    insertProject("proj1", "proj1-key", Qualifiers.PROJECT);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThat(db.select("select kee from portfolios"))
      .extracting(row -> row.get("KEE"))
      .containsExactlyInAnyOrder("port1", "port2", "port3", "port4");

    var portfolioKeyByUuid = db.select("select uuid, kee from portfolios").stream()
      .collect(Collectors.toMap(row -> row.get("KEE"), row -> row.get("UUID").toString()));

    String portfolio1Uuid = portfolioKeyByUuid.get("port1");
    String portfolio2Uuid = portfolioKeyByUuid.get("port2");
    String portfolio3Uuid = portfolioKeyByUuid.get("port3");
    String portfolio4Uuid = portfolioKeyByUuid.get("port4");

    assertThat(db.select("select portfolio_uuid, reference_uuid from portfolio_references"))
      .extracting(row -> row.get("PORTFOLIO_UUID"), row -> row.get("REFERENCE_UUID"))
      .containsExactlyInAnyOrder(
        tuple(portfolio2Uuid, portfolio1Uuid),
        tuple(portfolio2Uuid, portfolio3Uuid),
        tuple(portfolio4Uuid, portfolio2Uuid),
        tuple(portfolio4Uuid, "app1-uuid"),
        tuple(portfolio4Uuid, "app2-uuid"))
      .doesNotContain(tuple(portfolio4Uuid, "app3-key-not-existing"));
  }

  @Test
  public void migrate_xml_portfolios_hierarchy() throws SQLException {
    insertViewsDefInternalProperty(SIMPLE_XML_PORTFOLIOS_HIERARCHY);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThat(db.select("select kee from portfolios"))
      .extracting(row -> row.get("KEE"))
      .containsExactlyInAnyOrder("port1", "port2", "port3", "port4", "port5", "port6", "port7");

    var portfolioKeyByUuid = db.select("select uuid, kee from portfolios").stream()
      .collect(Collectors.toMap(row -> row.get("KEE"), row -> row.get("UUID").toString()));

    String portfolio1Uuid = portfolioKeyByUuid.get("port1");
    String portfolio2Uuid = portfolioKeyByUuid.get("port2");
    String portfolio3Uuid = portfolioKeyByUuid.get("port3");
    String portfolio4Uuid = portfolioKeyByUuid.get("port4");
    String portfolio5Uuid = portfolioKeyByUuid.get("port5");
    String portfolio6Uuid = portfolioKeyByUuid.get("port6");
    String portfolio7Uuid = portfolioKeyByUuid.get("port7");

    assertThat(db.select("select uuid, parent_uuid, root_uuid from portfolios"))
      .extracting(row -> row.get("UUID"), row -> row.get("PARENT_UUID"), row -> row.get("ROOT_UUID"))
      .containsExactlyInAnyOrder(
        tuple(portfolio1Uuid, null, portfolio1Uuid),
        tuple(portfolio2Uuid, portfolio1Uuid, portfolio1Uuid),
        tuple(portfolio3Uuid, portfolio1Uuid, portfolio1Uuid),
        tuple(portfolio4Uuid, portfolio1Uuid, portfolio1Uuid),
        tuple(portfolio5Uuid, portfolio2Uuid, portfolio1Uuid),
        tuple(portfolio6Uuid, portfolio3Uuid, portfolio1Uuid),
        tuple(portfolio7Uuid, portfolio3Uuid, portfolio1Uuid));
  }

  @Test
  public void migrate_xml_portfolios_different_selections() throws SQLException {
    insertViewsDefInternalProperty(SIMPLE_XML_PORTFOLIOS_DIFFERENT_SELECTIONS);

    insertProject("p1-uuid", "p1-key", Qualifiers.PROJECT);
    insertProject("p2-uuid", "p2-key", Qualifiers.PROJECT);
    insertProject("p3-uuid", "p3-key", Qualifiers.PROJECT);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThat(db.select("select kee, selection_mode, selection_expression from portfolios"))
      .extracting(row -> row.get("KEE"), row -> row.get("SELECTION_MODE"), row -> row.get("SELECTION_EXPRESSION"))
      .containsExactlyInAnyOrder(
        tuple("port-regexp", "REGEXP", ".*port.*"),
        tuple("port-projects", "MANUAL", null),
        tuple("port-tags", "TAGS", "tag1,tag2,tag3"));

    // verify projects
    assertThat(db.select("select p.kee, pp.project_uuid from "
      + "portfolio_projects pp join portfolios p on pp.portfolio_uuid = p.uuid where p.kee = 'port-projects'"))
        .extracting(row -> row.get("KEE"), row -> row.get("PROJECT_UUID"))
        .containsExactlyInAnyOrder(
          tuple("port-projects", "p1-uuid"),
          tuple("port-projects", "p2-uuid"),
          tuple("port-projects", "p3-uuid"))
        .doesNotContain(tuple("port-projects", "p4-uuid-not-existing"));
  }

  @Test
  public void migrate_xml_portfolios_legacy_selections() throws SQLException {
    insertViewsDefInternalProperty(XML_PORTFOLIOS_LEGACY_SELECTIONS);
    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThat(db.select("select kee, selection_mode, selection_expression from portfolios"))
      .extracting(row -> row.get("KEE"), row -> row.get("SELECTION_MODE"), row -> row.get("SELECTION_EXPRESSION"))
      .containsExactlyInAnyOrder(
        tuple("port-language", "NONE", null),
        tuple("port-tag-value", "NONE", null));
  }

  @Test
  public void migrate_xml_should_have_explicite_error_log_when_portfolio_hierarchy_nonexistent() {
    //GIVEN
    insertViewsDefInternalProperty(SIMPLE_XML_PORTFOLIOS_WRONG_HIERARCHY);
    //WHEN, THEN
    assertThatThrownBy(underTest::execute).isInstanceOf(IllegalStateException.class);
    assertThat(logTester.logs(LoggerLevel.ERROR)).containsExactly(
      PORTFOLIO_CONSISTENCY_ERROR,
      String.format(PORTFOLIO_ROOT_NOT_FOUND, "NON_EXISTING_ROOT", "port2", "port2"),
      String.format(PORTFOLIO_PARENT_NOT_FOUND, "NON_EXISTING_PARENT", "port3", "port3"));
  }

  @Test
  public void migrate_xml_should_not_have_explicite_error_log() throws SQLException {
    //GIVEN
    insertViewsDefInternalProperty(SIMPLE_XML_PORTFOLIOS_HIERARCHY);
    //WHEN
    underTest.execute();
    //THEN
    assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty();
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

  private void insertComponent(String uuid, String kee, boolean isPrivate) {
    db.executeInsert("components",
      "uuid", uuid,
      "kee", kee,
      "enabled", false,
      "private", isPrivate,
      "root_uuid", uuid,
      "uuid_path", uuid,
      "project_uuid", uuid);
  }

  private void insertDefaultVisibilityProperty(boolean isPrivate) {
    db.executeInsert("properties",
      "uuid", uuidFactory.create(),
      "prop_key", "projects.default.visibility",
      "IS_EMPTY", false,
      "text_value", isPrivate ? "private" : "public",
      "created_at", system2.now());
  }

}

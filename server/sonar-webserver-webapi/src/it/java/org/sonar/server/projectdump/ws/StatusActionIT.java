/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.projectdump.ws;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.core.util.Slug;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.util.Comparator.reverseOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.test.JsonAssert.assertJson;

public class StatusActionIT {

  private static final String ID_PARAM = "id";
  private static final String KEY_PARAM = "key";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final ComponentTypesRule resourceTypes = new ComponentTypesRule().setRootQualifiers(PROJECT);

  private static final String projectDumpsDirectoryPathname = "data/governance/project_dumps/";
  private static final String importDirectoryPathname = Paths.get(projectDumpsDirectoryPathname, "import").toString();
  private static final String exportDirectoryPathname = Paths.get(projectDumpsDirectoryPathname, "export").toString();

  private ProjectDto project;

  private WsActionTester underTest;

  private final Configuration config = mock(Configuration.class);

  @Before
  public void setUp() throws Exception {
    project = db.components().insertPrivateProject().getProjectDto();

    logInAsProjectAdministrator("user");

    when(config.get("sonar.path.data")).thenReturn(Optional.of("data"));
    underTest = new WsActionTester(new StatusAction(dbClient, userSession, new ComponentFinder(dbClient, resourceTypes), config));

    cleanUpFilesystem();
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    cleanUpFilesystem();
  }

  @Test
  public void definition() {
    WebService.Action definition = underTest.getDef();

    assertThat(definition.key()).isEqualTo("status");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.description()).isNotEmpty();
    assertThat(definition.since()).isEqualTo("1.0");
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.responseExampleFormat()).isEqualTo("json");
    assertThat(definition.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("id", false),
        tuple("key", false));
  }

  @Test
  public void fails_with_BRE_if_no_param_is_provided() {
    assertThatThrownBy(() -> underTest.newRequest().execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project id or project key must be provided, not both.");
  }

  @Test
  public void fails_with_BRE_if_both_params_are_provided() {
    String projectUuid = project.getUuid();
    String projectKey = project.getKey();
    assertThatThrownBy(() -> {
      underTest.newRequest()
        .setParam(ID_PARAM, projectUuid).setParam(KEY_PARAM, projectKey)
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project id or project key must be provided, not both.");
  }

  @Test
  public void fails_with_NFE_if_component_with_uuid_does_not_exist() {
    String UNKOWN_UUID = "UNKOWN_UUID";

    assertThatThrownBy(() -> {
      underTest.newRequest()
        .setParam(ID_PARAM, UNKOWN_UUID)
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project '" + UNKOWN_UUID + "' not found");
  }

  @Test
  public void fails_with_NFE_if_component_with_key_does_not_exist() {
    String UNKNOWN_KEY = "UNKNOWN_KEY";

    assertThatThrownBy(() -> {
      underTest.newRequest()
        .setParam(KEY_PARAM, UNKNOWN_KEY)
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project '" + UNKNOWN_KEY + "' not found");
  }

  @Test
  public void project_without_snapshot_can_be_imported_but_not_exported() {
    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertJson(response)
      .isSimilarTo("{\"canBeExported\":false,\"canBeImported\":true}");
  }

  @Test
  public void project_with_snapshots_but_none_is_last_can_neither_be_imported_nor_exported() {
    insertSnapshot(project, false);
    insertSnapshot(project, false);

    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertJson(response)
      .isSimilarTo("{\"canBeExported\":false,\"canBeImported\":false}");
  }

  @Test
  public void project_with_is_last_snapshot_can_be_exported_but_not_imported() {
    insertSnapshot(project, false);
    insertSnapshot(project, true);
    insertSnapshot(project, false);

    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertJson(response)
      .isSimilarTo("{\"canBeExported\":true,\"canBeImported\":false}");
  }

  @Test
  public void exportedDump_field_contains_absolute_path_if_file_exists_and_is_regular_file() throws IOException {
    final String exportDumpFilePath = ensureDumpFileExists(project.getKey(), false);

    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertJson(response)
      .isSimilarTo("{\"exportedDump\":\"" + exportDumpFilePath + "\"}");
  }

  @Test
  public void exportedDump_field_contains_absolute_path_if_file_exists_and_is_link() throws IOException {
    final String exportDumpFilePath = ensureDumpFileExists(project.getKey(), false);

    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertJson(response)
      .isSimilarTo("{\"exportedDump\":\"" + exportDumpFilePath + "\"}");
  }

  @Test
  public void exportedDump_field_not_sent_if_file_is_directory() throws IOException {
    Files.createDirectories(Paths.get(exportDirectoryPathname));

    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertThat(response)
      .doesNotContain("exportedDump");
  }

  @Test
  public void dumpToImport_field_contains_absolute_path_if_file_exists_and_is_regular_file() throws IOException {
    final String importDumpFilePath = ensureDumpFileExists(project.getKey(), true);

    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertJson(response)
      .isSimilarTo("{\"dumpToImport\":\"" + importDumpFilePath + "\"}");
  }

  @Test
  public void dumpToImport_field_contains_absolute_path_if_file_exists_and_is_link() throws IOException {
    final String importDumpFilePath = ensureDumpFileExists(project.getKey(), true);

    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertJson(response)
      .isSimilarTo("{\"dumpToImport\":\"" + importDumpFilePath + "\"}");
  }

  @Test
  public void dumpToImport_field_not_sent_if_file_is_directory() throws IOException {
    Files.createDirectories(Paths.get(importDirectoryPathname));

    String response = underTest.newRequest()
      .setParam(KEY_PARAM, project.getKey())
      .execute()
      .getInput();

    assertThat(response)
      .doesNotContain("dumpToImport");
  }

  @Test
  public void fail_when_using_branch_id() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);

    assertThatThrownBy(() -> {
      underTest.newRequest()
        .setParam(ID_PARAM, branch.uuid())
        .execute();
    })
      .isInstanceOf(NotFoundException.class);
  }


  private void insertSnapshot(ProjectDto projectDto, boolean last) {
    db.components().insertSnapshot(projectDto, snapshotDto -> snapshotDto.setLast(last));
    dbSession.commit();
  }

  private void logInAsProjectAdministrator(String login) {
    userSession.logIn(login).addProjectPermission(ProjectPermission.ADMIN, project);
  }

  private String ensureDumpFileExists(String projectKey, boolean isImport) throws IOException {
    final String targetDirectoryPathname = isImport ? importDirectoryPathname : exportDirectoryPathname;
    final String dumpFilename = Slug.slugify(projectKey) + ".zip";
    final String dumpFilePathname = Paths.get(targetDirectoryPathname, dumpFilename).toString();

    final Path dumpFilePath = Paths.get(dumpFilePathname);

    File fileToImport = new File(dumpFilePathname);

    fileToImport.getParentFile().mkdirs();

    Files.createFile(dumpFilePath);

    return dumpFilePathname;
  }

  private static void cleanUpFilesystem() throws IOException {
    final Path projectDumpsDirectoryPath = Paths.get(projectDumpsDirectoryPathname);

    if (Files.exists(projectDumpsDirectoryPath)) {
      Files.walk(projectDumpsDirectoryPath)
        .sorted(reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    }
  }
}

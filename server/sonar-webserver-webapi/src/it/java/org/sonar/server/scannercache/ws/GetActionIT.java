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
package org.sonar.server.scannercache.ws;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.scannercache.ScannerAnalysisCacheDao;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.scannercache.ScannerCache;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.SCAN;

public class GetActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final ScannerAnalysisCacheDao dao = new ScannerAnalysisCacheDao();
  private final ProjectDao projectDao = new ProjectDao(System2.INSTANCE, new NoOpAuditPersister());
  private final BranchDao branchDao = new BranchDao(System2.INSTANCE);
  private final ScannerCache cache = new ScannerCache(dbTester.getDbClient(), dao, projectDao, branchDao);
  private final ComponentFinder finder = new ComponentFinder(dbTester.getDbClient(), null);
  private final GetAction ws = new GetAction(dbTester.getDbClient(), userSession, finder, cache);
  private final WsActionTester wsTester = new WsActionTester(ws);

  @Test
  public void get_data_for_project() throws IOException {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    BranchDto branch = dbTester.components().insertProjectBranch(projectData.getProjectDto());
    ProjectData projectData2 = dbTester.components().insertPrivateProject();

    dao.insert(dbTester.getSession(), projectData.getMainBranchDto().getUuid(), stringToCompressedInputStream("test data1"));
    dao.insert(dbTester.getSession(), branch.getUuid(), stringToCompressedInputStream("test data2"));
    dao.insert(dbTester.getSession(), projectData2.getMainBranchComponent().uuid(), stringToCompressedInputStream("test data3"));

    userSession.logIn().addProjectPermission(SCAN, projectData.getProjectDto());
    TestResponse response = wsTester.newRequest()
      .setParam("project", projectData.projectKey())
      .setHeader("Accept-Encoding", "gzip")
      .execute();

    assertThat(compressedInputStreamToString(response.getInputStream())).isEqualTo("test data1");
    assertThat(response.getHeader("Content-Encoding")).isEqualTo("gzip");
  }

  @Test
  public void get_uncompressed_data_for_project() throws IOException {
    ProjectData projectData = dbTester.components().insertPrivateProject();
    ProjectDto project1 = projectData.getProjectDto();

    dao.insert(dbTester.getSession(), projectData.getMainBranchDto().getUuid(), stringToCompressedInputStream("test data1"));

    userSession.logIn().addProjectPermission(SCAN, project1);

    TestResponse response = wsTester.newRequest()
      .setParam("project", project1.getKey())
      .execute();

    assertThat(response.getHeader("Content-Encoding")).isNull();
    assertThat(response.getInput()).isEqualTo("test data1");
  }

  @Test
  public void get_data_for_branch() throws IOException {
    ProjectDto project1 = dbTester.components().insertPrivateProject().getProjectDto();
    BranchDto branch = dbTester.components().insertProjectBranch(project1);

    dao.insert(dbTester.getSession(), project1.getUuid(), stringToCompressedInputStream("test data1"));
    dao.insert(dbTester.getSession(), branch.getUuid(), stringToCompressedInputStream("test data2"));

    userSession.logIn().addProjectPermission(SCAN, project1)
      .registerBranches(branch);
    TestResponse response = wsTester.newRequest()
      .setParam("project", project1.getKey())
      .setParam("branch", branch.getKey())
      .setHeader("Accept-Encoding", "gzip")
      .execute();

    assertThat(compressedInputStreamToString(response.getInputStream())).isEqualTo("test data2");
  }

  @Test
  public void return_not_found_if_project_not_found() {
    TestRequest request = wsTester
      .newRequest()
      .setParam("project", "project1");
    assertThatThrownBy(request::execute).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void return_not_found_if_branch_mixed_with_pr() {
    ProjectDto project1 = dbTester.components().insertPrivateProject().getProjectDto();
    BranchDto branch = dbTester.components().insertProjectBranch(project1);

    userSession.logIn().addProjectPermission(SCAN, project1);
    TestRequest request = wsTester.newRequest()
      .setParam("project", project1.getKey())
      .setParam("pullRequest", branch.getKey());

    assertThatThrownBy(request::execute).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void return_not_found_if_cache_not_found() {
    ProjectDto project1 = dbTester.components().insertPrivateProject().getProjectDto();

    userSession.logIn().addProjectPermission(SCAN, project1);
    TestRequest request = wsTester
      .newRequest()
      .setParam("project", "project1");
    assertThatThrownBy(request::execute).isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_no_permissions() {
    ProjectDto project = dbTester.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(UserRole.CODEVIEWER, project);
    TestRequest request = wsTester
      .newRequest()
      .setParam("project", project.getKey());

    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  private static String compressedInputStreamToString(InputStream inputStream) throws IOException {
    return IOUtils.toString(new GZIPInputStream(inputStream), UTF_8);
  }

  private static InputStream stringToCompressedInputStream(String str) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
      IOUtils.write(str.getBytes(UTF_8), gzipOutputStream);
    }
    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
  }
}

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
package org.sonar.server.scannercache.ws;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.scannercache.ScannerAnalysisCacheDao;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.scannercache.ScannerCache;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.ProjectPermission.SCAN;

public class ClearActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final ScannerAnalysisCacheDao dao = new ScannerAnalysisCacheDao();
  private final ProjectDao projectDao = new ProjectDao(System2.INSTANCE, new NoOpAuditPersister());
  private final BranchDao branchDao = new BranchDao(System2.INSTANCE);
  private final ScannerCache cache = new ScannerCache(dbTester.getDbClient(), dao, projectDao, branchDao);
  private final ClearAction ws = new ClearAction(userSession, cache);
  private final WsActionTester wsTester = new WsActionTester(ws);

  @Test
  public void should_clear_all_entries() throws IOException {
    ProjectDto project1 = dbTester.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = dbTester.components().insertPrivateProject().getProjectDto();

    dao.insert(dbTester.getSession(), project1.getUuid(), stringToInputStream("test data"));
    dao.insert(dbTester.getSession(), project2.getUuid(), stringToInputStream("test data"));

    assertThat(dataStreamToString(dao.selectData(dbTester.getSession(), project1.getUuid()))).isEqualTo("test data");
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestResponse response = wsTester.newRequest().execute();

    response.assertNoContent();
    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isZero();
  }

  @Test
  public void should_clear_project_cache() {
    ProjectDto project1 = dbTester.components().insertPrivateProject(cDto -> cDto.setKey("p1")).getProjectDto();
    BranchDto branch11 = dbTester.components().insertProjectBranch(project1, cdto -> cdto.setKey("b11"));
    BranchDto branch12 = dbTester.components().insertProjectBranch(project1, cdto -> cdto.setKey("b12"));
    ProjectDto project2 = dbTester.components().insertPrivateProject(cDto -> cDto.setKey("p2")).getProjectDto();
    BranchDto branch21 = dbTester.components().insertProjectBranch(project2, cdto -> cdto.setKey("b21"));

    dao.insert(dbTester.getSession(), branch11.getUuid(), stringToInputStream("test data"));
    dao.insert(dbTester.getSession(), branch12.getUuid(), stringToInputStream("test data"));
    dao.insert(dbTester.getSession(), branch21.getUuid(), stringToInputStream("test data"));

    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestResponse response = wsTester.newRequest()
      .setParam(ClearAction.PARAM_PROJECT_KEY, "p1")
      .execute();

    response.assertNoContent();
    assertThat(cache.get(branch11.getUuid())).isNull();
    assertThat(cache.get(branch12.getUuid())).isNull();
    assertThat(cache.get(branch21.getUuid())).isNotNull();
  }

  @Test
  public void should_clear_branch_cache() {
    ProjectDto project1 = dbTester.components().insertPrivateProject(cDto -> cDto.setKey("p1")).getProjectDto();
    BranchDto branch11 = dbTester.components().insertProjectBranch(project1, cdto -> cdto.setKey("b11"));
    BranchDto branch12 = dbTester.components().insertProjectBranch(project1, cdto -> cdto.setKey("b12"));
    ProjectDto project2 = dbTester.components().insertPrivateProject(cDto -> cDto.setKey("p2")).getProjectDto();
    BranchDto branch21 = dbTester.components().insertProjectBranch(project2, cdto -> cdto.setKey("b21"));

    dao.insert(dbTester.getSession(), branch11.getUuid(), stringToInputStream("test data"));
    dao.insert(dbTester.getSession(), branch12.getUuid(), stringToInputStream("test data"));
    dao.insert(dbTester.getSession(), branch21.getUuid(), stringToInputStream("test data"));

    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestResponse response = wsTester.newRequest()
      .setParam(ClearAction.PARAM_PROJECT_KEY, "p1")
      .setParam(ClearAction.PARAM_BRANCH_KEY, "b11")
      .execute();

    response.assertNoContent();
    assertThat(cache.get(branch11.getUuid())).isNull();
    assertThat(cache.get(branch12.getUuid())).isNotNull();
    assertThat(cache.get(branch21.getUuid())).isNotNull();
  }

  @Test
  public void should_fail_on_missing_project_key() {
    userSession.logIn().addPermission(GlobalPermission.ADMINISTER);
    TestRequest failingRequest = wsTester.newRequest().setParam(ClearAction.PARAM_BRANCH_KEY, "b11");

    assertThatThrownBy(failingRequest::execute, "missing project key param with branch present should throw exception")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_not_global_admin() throws IOException {
    ProjectDto project = dbTester.components().insertPrivateProject().getProjectDto();
    dao.insert(dbTester.getSession(), "branch1", stringToInputStream("test data"));
    DbInputStream branchDataIs = dao.selectData(dbTester.getSession(), "branch1");
    assertThat(branchDataIs).isNotNull();
    assertThat(dataStreamToString(branchDataIs)).isEqualTo("test data");
    userSession.logIn().addProjectPermission(SCAN, project);
    TestRequest request = wsTester.newRequest();

    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  private static String dataStreamToString(DbInputStream dbInputStream) throws IOException {
    try (DbInputStream ds = dbInputStream) {
      return IOUtils.toString(ds, StandardCharsets.UTF_8);
    }
  }

  private static InputStream stringToInputStream(String str) {
    return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
  }
}

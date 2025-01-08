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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.core.util.Slug.slugify;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.db.DatabaseUtils.closeQuietly;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.server.component.ComponentFinder.ParamNames.ID_AND_KEY;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class StatusAction implements ProjectDumpAction {
  private static final String PARAM_PROJECT_KEY = "key";
  private static final String PARAM_PROJECT_ID = "id";
  private static final String DUMP_FILE_EXTENSION = ".zip";

  private static final String GOVERNANCE_DIR_NAME = "governance";
  private static final String PROJECT_DUMPS_DIR_NAME = "project_dumps";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  private final String dataPath;
  private final File importDir;
  private final File exportDir;

  public StatusAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, Configuration config) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.dataPath = config.get(PATH_DATA.getKey()).get();
    this.importDir = this.getProjectDumpDir("import");
    this.exportDir = this.getProjectDumpDir("export");
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("status")
      .setDescription("Provide the import and export status of a project. Permission 'Administer' is required. " +
        "The project id or project key must be provided.")
      .setSince("1.0")
      .setInternal(true)
      .setPost(false)
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-status.json"));
    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue(UUID_EXAMPLE_01);
    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue("my_project");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String uuid = request.param(PARAM_PROJECT_ID);
    String key = request.param(PARAM_PROJECT_KEY);
    checkRequest(uuid == null ^ key == null, "Project id or project key must be provided, not both.");

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = getProject(dbSession, uuid, key);
      BranchDto mainBranch = componentFinder.getMainBranch(dbSession, project);
      userSession.checkEntityPermission(UserRole.ADMIN, project);

      WsResponse wsResponse = new WsResponse();
      checkDumps(project, wsResponse);

      SnapshotsStatus snapshots = checkSnapshots(dbSession, mainBranch);
      if (snapshots.hasLast) {
        wsResponse.setCanBeExported();
      } else if (!snapshots.hasAny) {
        wsResponse.setCanBeImported();
      }
      write(response, wsResponse);
    }
  }

  private SnapshotsStatus checkSnapshots(DbSession dbSession, BranchDto mainBranch) throws SQLException {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      String sql = "select" +
        " count(*), islast" +
        " from snapshots" +
        " where" +
        " root_component_uuid = ?" +
        " group by" +
        " islast";
      stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, sql);
      stmt.setString(1, mainBranch.getUuid());
      rs = stmt.executeQuery();
      SnapshotsStatus res = new SnapshotsStatus();
      while (rs.next()) {
        long count = rs.getLong(1);
        boolean isLast = rs.getBoolean(2);
        if (isLast) {
          res.setHasLast(count > 0);
        }
        if (count > 0) {
          res.setHasAny(true);
        }
      }
      return res;
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
  }

  private File getProjectDumpDir(String type) {
    final File governanceDir = new File(this.dataPath, GOVERNANCE_DIR_NAME);
    final File projectDumpDir = new File(governanceDir, PROJECT_DUMPS_DIR_NAME);

    return new File(projectDumpDir, type);
  }

  private void checkDumps(ProjectDto project, WsResponse wsResponse) {
    String fileName = slugify(project.getKey()) + DUMP_FILE_EXTENSION;

    final File importFile = new File(this.importDir, fileName);
    final File exportFile = new File(this.exportDir, fileName);

    if (importFile.exists() && importFile.isFile()) {
      wsResponse.setDumpToImport(importFile.toPath().toString());
    }

    if (exportFile.exists() && exportFile.isFile()) {
      wsResponse.setExportedDump(exportFile.toPath().toString());
    }
  }

  private static void write(Response response, WsResponse wsResponse) {
    JsonWriter jsonWriter = response.newJsonWriter();
    jsonWriter
      .beginObject()
      .prop("canBeExported", wsResponse.canBeExported)
      .prop("canBeImported", wsResponse.canBeImported)
      .prop("exportedDump", wsResponse.exportedDump)
      .prop("dumpToImport", wsResponse.dumpToImport)
      .endObject();
    jsonWriter.close();
  }

  private static class WsResponse {
    private String exportedDump = null;
    private String dumpToImport = null;
    private boolean canBeExported = false;
    private boolean canBeImported = false;

    public void setExportedDump(String exportedDump) {
      checkArgument(isNotBlank(exportedDump), "exportedDump can not be null nor empty");
      this.exportedDump = exportedDump;
    }

    public void setDumpToImport(String dumpToImport) {
      checkArgument(isNotBlank(dumpToImport), "dumpToImport can not be null nor empty");
      this.dumpToImport = dumpToImport;
    }

    public void setCanBeExported() {
      this.canBeExported = true;
    }

    public void setCanBeImported() {
      this.canBeImported = true;
    }
  }

  private static class SnapshotsStatus {
    private boolean hasLast = false;
    private boolean hasAny = false;

    public void setHasLast(boolean hasLast) {
      this.hasLast = hasLast;
    }

    public void setHasAny(boolean hasAny) {
      this.hasAny = hasAny;
    }
  }

  private ProjectDto getProject(DbSession dbSession, @Nullable String uuid, @Nullable String key) {
    return componentFinder.getProjectByUuidOrKey(dbSession, uuid, key, ID_AND_KEY);
  }
}

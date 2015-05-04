/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.test.ws;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.test.index.CoveredFileDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;

public class TestsCoveredFilesAction implements TestAction {

  public static final String TEST_UUID = "testUuid";

  private final DbClient dbClient;
  private final TestIndex index;

  public TestsCoveredFilesAction(DbClient dbClient, TestIndex index) {
    this.dbClient = dbClient;
    this.index = index;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("covered_files")
      .setDescription("Get the list of source files covered by a test. Require Browse permission on test file's project")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "tests-example-covered-files.json"))
      .setHandler(this)
      .addPagingParams(100);

    action
      .createParam(TEST_UUID)
      .setRequired(true)
      .setDescription("Test uuid")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");
  }

  @Override
  public void handle(Request request, Response response) {
    String testUuid = request.mandatoryParam(TEST_UUID);
    UserSession.get().checkComponentUuidPermission(UserRole.CODEVIEWER, index.searchByTestUuid(testUuid).fileUuid());

    List<CoveredFileDoc> coveredFiles = index.coveredFiles(testUuid);
    Map<String, ComponentDto> componentsByUuid = buildComponentsByUuid(coveredFiles);
    JsonWriter json = response.newJsonWriter().beginObject();
    if (!coveredFiles.isEmpty()) {
      writeTests(coveredFiles, componentsByUuid, json);
    }
    json.endObject().close();
  }

  private void writeTests(List<CoveredFileDoc> coveredFiles, Map<String, ComponentDto> componentsByUuid, JsonWriter json) {
    json.name("files").beginArray();
    for (CoveredFileDoc coveredFile : coveredFiles) {
      json.beginObject();
      json.prop("key", componentsByUuid.get(coveredFile.fileUuid()).key());
      json.prop("longName", componentsByUuid.get(coveredFile.fileUuid()).longName());
      json.prop("coveredLines", coveredFile.coveredLines().size());
      json.endObject();
    }
    json.endArray();
  }

  private Map<String, ComponentDto> buildComponentsByUuid(List<CoveredFileDoc> coveredFiles) {
    List<String> sourceFileUuids = Lists.transform(coveredFiles, new Function<CoveredFileDoc, String>() {
      @Override
      public String apply(CoveredFileDoc coveredFile) {
        return coveredFile.fileUuid();
      }
    });
    DbSession dbSession = dbClient.openSession(false);
    List<ComponentDto> components;
    try {
      components = dbClient.componentDao().getByUuids(dbSession, sourceFileUuids);
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
    return Maps.uniqueIndex(components, new Function<ComponentDto, String>() {
      @Override
      public String apply(ComponentDto component) {
        return component.uuid();
      }
    });
  }

}

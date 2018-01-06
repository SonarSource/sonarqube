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
package org.sonar.server.test.ws;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.test.index.CoveredFileDoc;
import org.sonar.server.test.index.TestDoc;
import org.sonar.server.test.index.TestIndex;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Tests;

import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CoveredFilesAction implements TestsWsAction {

  public static final String TEST_ID = "testId";

  private final DbClient dbClient;
  private final TestIndex index;
  private final UserSession userSession;

  public CoveredFilesAction(DbClient dbClient, TestIndex index, UserSession userSession) {
    this.dbClient = dbClient;
    this.index = index;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("covered_files")
      .setDescription("Get the list of source files covered by a test. Require Browse permission on test file's project")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "tests-example-covered-files.json"))
      .setDeprecatedSince("5.6")
      .setHandler(this)
      .setChangelog(new Change("6.6", "\"branch\" field is now returned"))
      .addPagingParams(100);

    action
      .createParam(TEST_ID)
      .setRequired(true)
      .setDescription("Test ID")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String testId = request.mandatoryParam(TEST_ID);
    TestDoc testDoc = checkFoundWithOptional(index.getNullableByTestUuid(testId), "Test with id '%s' is not found", testId);
    userSession.checkComponentUuidPermission(UserRole.CODEVIEWER, testDoc.fileUuid());

    List<CoveredFileDoc> coveredFiles = index.coveredFiles(testId);
    Map<String, ComponentDto> componentsByUuid = buildComponentsByUuid(coveredFiles);

    Tests.CoveredFilesResponse.Builder responseBuilder = Tests.CoveredFilesResponse.newBuilder();
    if (!coveredFiles.isEmpty()) {
      for (CoveredFileDoc doc : coveredFiles) {
        Tests.CoveredFilesResponse.CoveredFile.Builder fileBuilder = Tests.CoveredFilesResponse.CoveredFile.newBuilder();
        fileBuilder.setId(doc.fileUuid());
        fileBuilder.setCoveredLines(doc.coveredLines().size());
        ComponentDto component = componentsByUuid.get(doc.fileUuid());
        if (component != null) {
          fileBuilder.setKey(component.getKey());
          fileBuilder.setLongName(component.longName());
          setNullable(component.getBranch(), fileBuilder::setBranch);
        }

        responseBuilder.addFiles(fileBuilder);
      }
    }
    writeProtobuf(responseBuilder.build(), request, response);
  }

  private Map<String, ComponentDto> buildComponentsByUuid(List<CoveredFileDoc> coveredFiles) {
    List<String> sourceFileUuids = Lists.transform(coveredFiles, new CoveredFileToFileUuidFunction());
    List<ComponentDto> components;
    try (DbSession dbSession = dbClient.openSession(false)) {
      components = dbClient.componentDao().selectByUuids(dbSession, sourceFileUuids);
    }
    return Maps.uniqueIndex(components, ComponentDto::uuid);
  }

  private static class CoveredFileToFileUuidFunction implements Function<CoveredFileDoc, String> {
    @Override
    public String apply(@Nonnull CoveredFileDoc coveredFile) {
      return coveredFile.fileUuid();
    }
  }

}

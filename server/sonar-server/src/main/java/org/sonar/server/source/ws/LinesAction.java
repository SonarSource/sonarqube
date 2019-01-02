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
package org.sonar.server.source.ws;

import com.google.common.base.MoreObjects;
import com.google.common.io.Resources;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.source.HtmlSourceDecorator;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.component.ComponentFinder.ParamNames.UUID_AND_KEY;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_FILE_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class LinesAction implements SourcesWsAction {

  private static final String PARAM_UUID = "uuid";
  private static final String PARAM_KEY = "key";
  private static final String PARAM_FROM = "from";
  private static final String PARAM_TO = "to";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_PULL_REQUEST = "pullRequest";

  private final ComponentFinder componentFinder;
  private final SourceService sourceService;
  private final HtmlSourceDecorator htmlSourceDecorator;
  private final DbClient dbClient;
  private final UserSession userSession;

  public LinesAction(ComponentFinder componentFinder, DbClient dbClient, SourceService sourceService,
    HtmlSourceDecorator htmlSourceDecorator, UserSession userSession) {
    this.componentFinder = componentFinder;
    this.sourceService = sourceService;
    this.htmlSourceDecorator = htmlSourceDecorator;
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("lines")
      .setDescription("Show source code with line oriented info. Require See Source Code permission on file's project<br/>" +
        "Each element of the result array is an object which contains:" +
        "<ol>" +
        "<li>Line number</li>" +
        "<li>Content of the line</li>" +
        "<li>Author of the line (from SCM information)</li>" +
        "<li>Revision of the line (from SCM information)</li>" +
        "<li>Last commit date of the line (from SCM information)</li>" +
        "<li>Line hits from coverage</li>" +
        "<li>Number of conditions to cover in tests</li>" +
        "<li>Number of conditions covered by tests</li>" +
        "<li>Whether the line is new</li>" +
        "</ol>")
      .setSince("5.0")
      .setInternal(true)
      .setResponseExample(Resources.getResource(getClass(), "lines-example.json"))
      .setChangelog(
        new Change("6.2", "fields \"utLineHits\", \"utConditions\" and \"utCoveredConditions\" " +
          "has been renamed \"lineHits\", \"conditions\" and \"coveredConditions\""),
        new Change("6.2", "fields \"itLineHits\", \"itConditions\" and \"itCoveredConditions\" " +
          "are no more returned"),
        new Change("6.6", "field \"branch\" added"),
        new Change("7.4", "field \"isNew\" added"))
      .setHandler(this);

    action
      .createParam(PARAM_UUID)
      .setDescription("File uuid. Mandatory if param 'key' is not set")
      .setExampleValue("f333aab4-7e3a-4d70-87e1-f4c491f05e5c");

    action
      .createParam(PARAM_KEY)
      .setDescription("File key. Mandatory if param 'uuid' is not set. Available since 5.2")
      .setExampleValue(KEY_FILE_EXAMPLE_001);

    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setInternal(true)
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action
      .createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id")
      .setInternal(true)
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);

    action
      .createParam(PARAM_FROM)
      .setDescription("First line to return. Starts from 1")
      .setExampleValue("10")
      .setDefaultValue("1");

    action
      .createParam(PARAM_TO)
      .setDescription("Optional last line to return (inclusive). It must be greater than " +
        "or equal to parameter 'from'. If unset, then all the lines greater than or equal to 'from' " +
        "are returned.")
      .setExampleValue("20");
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto file = loadComponent(dbSession, request);
      Supplier<Optional<Long>> periodDateSupplier = () -> dbClient.snapshotDao()
        .selectLastAnalysisByComponentUuid(dbSession, file.projectUuid())
        .map(SnapshotDto::getPeriodDate);

      userSession.checkComponentPermission(UserRole.CODEVIEWER, file);
      int from = request.mandatoryParamAsInt(PARAM_FROM);
      int to = MoreObjects.firstNonNull(request.paramAsInt(PARAM_TO), Integer.MAX_VALUE);

      Iterable<DbFileSources.Line> lines = checkFoundWithOptional(sourceService.getLines(dbSession, file.uuid(), from, to), "No source found for file '%s'", file.getDbKey());
      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        writeSource(lines, json, isMemberOfOrganization(dbSession, file), periodDateSupplier);
        json.endObject();
      }
    }
  }

  private boolean isMemberOfOrganization(DbSession dbSession, ComponentDto file) {
    OrganizationDto organizationDto = dbClient.organizationDao().selectByUuid(dbSession, file.getOrganizationUuid())
      .orElseThrow(() -> new IllegalStateException(String.format("Organization with uuid '%s' not found", file.getOrganizationUuid())));
    return !userSession.hasMembership(organizationDto);
  }

  private ComponentDto loadComponent(DbSession dbSession, Request wsRequest) {
    String componentKey = wsRequest.param(PARAM_KEY);
    String componentId = wsRequest.param(PARAM_UUID);
    String branch = wsRequest.param(PARAM_BRANCH);
    String pullRequest = wsRequest.param(PARAM_PULL_REQUEST);
    checkArgument(componentId == null || (branch == null && pullRequest == null), "Parameter '%s' cannot be used at the same time as '%s' or '%s'",
      PARAM_UUID, PARAM_BRANCH, PARAM_PULL_REQUEST);
    if (branch == null && pullRequest == null) {
      return componentFinder.getByUuidOrKey(dbSession, componentId, componentKey, UUID_AND_KEY);
    }

    checkRequest(componentKey != null, "The '%s' parameter is missing", PARAM_KEY);
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, pullRequest);
  }

  private void writeSource(Iterable<DbFileSources.Line> lines, JsonWriter json, boolean filterScmAuthors, Supplier<Optional<Long>> periodDateSupplier) {
    Optional<Long> periodDate = null;

    json.name("sources").beginArray();
    for (DbFileSources.Line line : lines) {
      json.beginObject()
        .prop("line", line.getLine())
        .prop("code", htmlSourceDecorator.getDecoratedSourceAsHtml(line.getSource(), line.getHighlighting(), line.getSymbols()))
        .prop("scmRevision", line.getScmRevision());
      if (!filterScmAuthors) {
        json.prop("scmAuthor", line.getScmAuthor());
      }
      if (line.hasScmDate()) {
        json.prop("scmDate", DateUtils.formatDateTime(new Date(line.getScmDate())));
      }
      Optional<Integer> lineHits = getLineHits(line);
      if (lineHits.isPresent()) {
        json.prop("utLineHits", lineHits.get());
        json.prop("lineHits", lineHits.get());
      }
      Optional<Integer> conditions = getConditions(line);
      if (conditions.isPresent()) {
        json.prop("utConditions", conditions.get());
        json.prop("conditions", conditions.get());
      }
      Optional<Integer> coveredConditions = getCoveredConditions(line);
      if (coveredConditions.isPresent()) {
        json.prop("utCoveredConditions", coveredConditions.get());
        json.prop("coveredConditions", coveredConditions.get());
      }
      json.prop("duplicated", line.getDuplicationCount() > 0);
      if (line.hasIsNewLine()) {
        json.prop("isNew", line.getIsNewLine());
      } else {
        if (periodDate == null) {
          periodDate = periodDateSupplier.get();
        }
        json.prop("isNew", periodDate.isPresent() && line.getScmDate() > periodDate.get());
      }
      json.endObject();
    }
    json.endArray();
  }

  private static Optional<Integer> getLineHits(DbFileSources.Line line) {
    if (line.hasLineHits()) {
      return Optional.of(line.getLineHits());
    } else if (line.hasDeprecatedOverallLineHits()) {
      return Optional.of(line.getDeprecatedOverallLineHits());
    } else if (line.hasDeprecatedUtLineHits()) {
      return Optional.of(line.getDeprecatedUtLineHits());
    } else if (line.hasDeprecatedItLineHits()) {
      return Optional.of(line.getDeprecatedItLineHits());
    }
    return Optional.empty();
  }

  private static Optional<Integer> getConditions(DbFileSources.Line line) {
    if (line.hasConditions()) {
      return Optional.of(line.getConditions());
    } else if (line.hasDeprecatedOverallConditions()) {
      return Optional.of(line.getDeprecatedOverallConditions());
    } else if (line.hasDeprecatedUtConditions()) {
      return Optional.of(line.getDeprecatedUtConditions());
    } else if (line.hasDeprecatedItConditions()) {
      return Optional.of(line.getDeprecatedItConditions());
    }
    return Optional.empty();
  }

  private static Optional<Integer> getCoveredConditions(DbFileSources.Line line) {
    if (line.hasCoveredConditions()) {
      return Optional.of(line.getCoveredConditions());
    } else if (line.hasDeprecatedOverallCoveredConditions()) {
      return Optional.of(line.getDeprecatedOverallCoveredConditions());
    } else if (line.hasDeprecatedUtCoveredConditions()) {
      return Optional.of(line.getDeprecatedUtCoveredConditions());
    } else if (line.hasDeprecatedItCoveredConditions()) {
      return Optional.of(line.getDeprecatedItCoveredConditions());
    }
    return Optional.empty();
  }

}

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
package org.sonar.server.source.ws;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.component.ws.ComponentViewerJsonWriter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;

import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;

public class IssueSnippetsAction implements SourcesWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final SourceService sourceService;
  private final ComponentViewerJsonWriter componentViewerJsonWriter;
  private final LinesJsonWriter linesJsonWriter;

  public IssueSnippetsAction(DbClient dbClient, UserSession userSession, SourceService sourceService, LinesJsonWriter linesJsonWriter,
    ComponentViewerJsonWriter componentViewerJsonWriter) {
    this.sourceService = sourceService;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.linesJsonWriter = linesJsonWriter;
    this.componentViewerJsonWriter = componentViewerJsonWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("issue_snippets")
      .setDescription("Get code snippets involved in an issue or hotspot. Requires 'See Source Code permission' permission on the project<br/>")
      .setSince("7.8")
      .setInternal(true)
      .setResponseExample(getResource(getClass(), "example-show.json"))
      .setHandler(this);

    action
      .createParam("issueKey")
      .setRequired(true)
      .setDescription("Issue or hotspot key")
      .setExampleValue("AU-Tpxb--iU5OvuD2FLy");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String issueKey = request.mandatoryParam("issueKey");
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto issueDto = dbClient.issueDao().selectByKey(dbSession, issueKey)
        .orElseThrow(() -> new NotFoundException(format("Issue with key '%s' does not exist", issueKey)));
      ComponentDto project = dbClient.componentDao().selectByUuid(dbSession, issueDto.getProjectUuid())
        .orElseThrow(() -> new NotFoundException(format("Project with uuid '%s' does not exist", issueDto.getProjectUuid())));
      userSession.checkComponentPermission(UserRole.CODEVIEWER, project);

      DbIssues.Locations locations = issueDto.parseLocations();
      String componentUuid = issueDto.getComponentUuid();
      if (locations == null || componentUuid == null) {
        response.noContent();
      } else {
        Map<String, TreeSet<Integer>> linesPerComponent = getLinesPerComponent(componentUuid, locations);
        Map<String, ComponentDto> componentsByUuid = dbClient.componentDao().selectByUuids(dbSession, linesPerComponent.keySet())
          .stream().collect(Collectors.toMap(ComponentDto::uuid, c -> c));

        Set<String> branchUuids = componentsByUuid.values().stream()
          .map(ComponentDto::branchUuid)
          .collect(Collectors.toSet());

        Map<String, BranchDto> branches = dbClient.branchDao()
          .selectByUuids(dbSession, branchUuids)
          .stream()
          .collect(Collectors.toMap(BranchDto::getUuid, b -> b));

        try (JsonWriter jsonWriter = response.newJsonWriter()) {
          jsonWriter.beginObject();

          for (Map.Entry<String, TreeSet<Integer>> e : linesPerComponent.entrySet()) {
            ComponentDto componentDto = componentsByUuid.get(e.getKey());
            if (componentDto != null) {
              writeSnippet(dbSession, jsonWriter, componentDto, e.getValue(), branches.get(componentDto.branchUuid()));
            }
          }

          jsonWriter.endObject();
        }
      }
    }
  }

  private void writeSnippet(DbSession dbSession, JsonWriter writer, ComponentDto fileDto, Set<Integer> lines, BranchDto branchDto) {
    Optional<Iterable<DbFileSources.Line>> lineSourcesOpt = sourceService.getLines(dbSession, fileDto.uuid(), lines);
    if (lineSourcesOpt.isEmpty()) {
      return;
    }

    Supplier<Optional<Long>> periodDateSupplier = () -> dbClient.snapshotDao()
      .selectLastAnalysisByComponentUuid(dbSession, fileDto.branchUuid())
      .map(SnapshotDto::getPeriodDate);

    Iterable<DbFileSources.Line> lineSources = lineSourcesOpt.get();

    writer.name(fileDto.getKey()).beginObject();

    writer.name("component").beginObject();
    String branch = branchDto.isMain() ? null : branchDto.getBranchKey();
    String pullRequest = branchDto.getPullRequestKey();
    componentViewerJsonWriter.writeComponentWithoutFav(writer, fileDto, dbSession, branch, pullRequest);
    componentViewerJsonWriter.writeMeasures(writer, fileDto, dbSession);
    writer.endObject();
    linesJsonWriter.writeSource(lineSources, writer, periodDateSupplier);

    writer.endObject();
  }

  private static Map<String, TreeSet<Integer>> getLinesPerComponent(String componentUuid, DbIssues.Locations locations) {
    Map<String, TreeSet<Integer>> linesPerComponent = new HashMap<>();

    if (locations.hasTextRange()) {
      // extra lines for the main location
      addTextRange(linesPerComponent, componentUuid, locations.getTextRange(), 9);
    }
    for (DbIssues.Flow flow : locations.getFlowList()) {
      for (DbIssues.Location l : flow.getLocationList()) {
        if (l.hasComponentId()) {
          addTextRange(linesPerComponent, l.getComponentId(), l.getTextRange(), 5);
        } else {
          addTextRange(linesPerComponent, componentUuid, l.getTextRange(), 5);
        }
      }
    }

    return linesPerComponent;
  }

  private static void addTextRange(Map<String, TreeSet<Integer>> linesPerComponent, String componentUuid,
    DbCommons.TextRange textRange, int numLinesAfterIssue) {
    int start = textRange.getStartLine() - 5;
    int end = textRange.getEndLine() + numLinesAfterIssue;

    TreeSet<Integer> lines = linesPerComponent.computeIfAbsent(componentUuid, c -> new TreeSet<>());
    IntStream.rangeClosed(start, end).forEach(lines::add);

    // If two snippets in the same component are 10 lines apart from each other, include those 10 lines.
    Integer closestToStart = lines.lower(start);
    if (closestToStart != null && closestToStart >= start - 11) {
      IntStream.range(closestToStart + 1, start).forEach(lines::add);
    }

    Integer closestToEnd = lines.higher(end);
    if (closestToEnd != null && closestToEnd <= end + 11) {
      IntStream.range(end + 1, closestToEnd).forEach(lines::add);
    }

  }
}

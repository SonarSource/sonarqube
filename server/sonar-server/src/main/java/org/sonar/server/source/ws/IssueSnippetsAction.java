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

import com.google.common.io.Resources;
import java.util.Collections;
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.component.ws.ComponentViewerJsonWriter;
import org.sonar.server.issue.IssueFinder;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.UserSession;

public class IssueSnippetsAction implements SourcesWsAction {
  private final IssueFinder issueFinder;
  private final LinesJsonWriter linesJsonWriter;
  private final ComponentViewerJsonWriter componentViewerJsonWriter;
  private final SourceService sourceService;
  private final DbClient dbClient;

  public IssueSnippetsAction(SourceService sourceService, DbClient dbClient, IssueFinder issueFinder, LinesJsonWriter linesJsonWriter,
    ComponentViewerJsonWriter componentViewerJsonWriter) {
    this.sourceService = sourceService;
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
    this.linesJsonWriter = linesJsonWriter;
    this.componentViewerJsonWriter = componentViewerJsonWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("issue_snippets")
      .setDescription("Get code snipets involved in an issue. Requires 'Browse' permission on the project<br/>")
      .setSince("7.8")
      .setInternal(true)
      .setResponseExample(Resources.getResource(getClass(), "example-show.json"))
      .setHandler(this);

    action
      .createParam("issueKey")
      .setRequired(true)
      .setDescription("Issue key")
      .setExampleValue("AU-Tpxb--iU5OvuD2FLy");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String issueKey = request.mandatoryParam("issueKey");
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueDto issueDto = issueFinder.getByKey(dbSession, issueKey);

      Map<String, TreeSet<Integer>> linesPerComponent;
      Map<String, ComponentDto> componentsByUuid;

      DbIssues.Locations locations = issueDto.parseLocations();
      if (locations != null && issueDto.getComponentUuid() != null) {
        linesPerComponent = getLinesPerComponent(issueDto.getComponentUuid(), locations);
        componentsByUuid = dbClient.componentDao().selectByUuids(dbSession, linesPerComponent.keySet())
          .stream().collect(Collectors.toMap(ComponentDto::uuid, c -> c));
      } else {
        componentsByUuid = Collections.emptyMap();
        linesPerComponent = Collections.emptyMap();
      }

      try (JsonWriter jsonWriter = response.newJsonWriter()) {
        jsonWriter.beginObject();

        for (Map.Entry<String, TreeSet<Integer>> e : linesPerComponent.entrySet()) {
          ComponentDto componentDto = componentsByUuid.get(e.getKey());
          if (componentDto != null) {
            processComponent(dbSession, jsonWriter, componentDto, e.getKey(), e.getValue());
          }
        }

        jsonWriter.endObject();
      }
    }
  }

  private void processComponent(DbSession dbSession, JsonWriter writer, ComponentDto componentDto, String fileUuid, Set<Integer> lines) {
    Optional<Iterable<DbFileSources.Line>> lineSourcesOpt = sourceService.getLines(dbSession, fileUuid, lines);
    if (!lineSourcesOpt.isPresent()) {
      return;
    }

    Supplier<Optional<Long>> periodDateSupplier = () -> dbClient.snapshotDao()
      .selectLastAnalysisByComponentUuid(dbSession, componentDto.projectUuid())
      .map(SnapshotDto::getPeriodDate);

    Iterable<DbFileSources.Line> lineSources = lineSourcesOpt.get();

    writer.name(componentDto.getKey()).beginObject();

    writer.name("component").beginObject();
    componentViewerJsonWriter.writeComponentWithoutFav(writer, componentDto, dbSession, false);
    componentViewerJsonWriter.writeMeasures(writer, componentDto, dbSession);
    writer.endObject();
    linesJsonWriter.writeSource(lineSources, writer, true, periodDateSupplier);

    writer.endObject();
  }

  private Map<String, TreeSet<Integer>> getLinesPerComponent(String componentUuid, DbIssues.Locations locations) {
    Map<String, TreeSet<Integer>> linesPerComponent = new HashMap<>();

    if (locations.hasTextRange()) {
      // extra lines for the main location
      addTextRange(linesPerComponent, componentUuid, locations.getTextRange(), 9);
    }
    for (DbIssues.Flow flow : locations.getFlowList()) {
      for (DbIssues.Location l : flow.getLocationList()) {
        if (l.hasComponentId()) {
          addTextRange(linesPerComponent, l.getComponentId(), l.getTextRange(), 2);
        } else {
          addTextRange(linesPerComponent, componentUuid, l.getTextRange(), 2);
        }
      }
    }

    return linesPerComponent;
  }

  private static void addTextRange(Map<String, TreeSet<Integer>> linesPerComponent, String componentUuid,
    DbCommons.TextRange textRange, int numLinesAfterIssue) {
    int start = textRange.getStartLine() - 2;
    int end = textRange.getEndLine() + numLinesAfterIssue;

    TreeSet<Integer> lines = linesPerComponent.computeIfAbsent(componentUuid, c -> new TreeSet<>());
    IntStream.rangeClosed(start, end).forEach(lines::add);

    // If two snippets in the same component are 3 lines apart of each other, include those 3 lines.
    Integer closestToStart = lines.lower(start);
    if (closestToStart != null && closestToStart >= start - 4) {
      IntStream.range(closestToStart + 1, start).forEach(lines::add);
    }

    Integer closestToEnd = lines.higher(end);
    if (closestToEnd != null && closestToEnd <= end + 4) {
      IntStream.range(end + 1, closestToEnd).forEach(lines::add);
    }

  }
}

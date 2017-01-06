/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.IssueFinder;
import org.sonarqube.ws.Issues.ChangelogWsResponse;
import org.sonarqube.ws.Issues.ChangelogWsResponse.Changelog;

import static com.google.common.base.Strings.emptyToNull;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.issue.IssueFieldsSetter.FILE;
import static org.sonar.server.issue.IssueFieldsSetter.TECHNICAL_DEBT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_CHANGELOG;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE;

public class ChangelogAction implements IssuesWsAction {

  private static final String EFFORT_CHANGELOG_KEY = "effort";

  private final DbClient dbClient;
  private final IssueFinder issueFinder;

  public ChangelogAction(DbClient dbClient, IssueFinder issueFinder) {
    this.dbClient = dbClient;
    this.issueFinder = issueFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_CHANGELOG)
      .setDescription("Display changelog of an issue<br/>." +
        "Require the 'Browse' permission on the project of the specified issue<br/>." +
        "Since 6.3, changes on effort are returning raw value in minutes, it doesn't return anymore the duration.")
      .setSince("4.1")
      .setHandler(this)
      .setResponseExample(Resources.getResource(IssuesWs.class, "changelog-example.json"));
    action.createParam(PARAM_ISSUE)
      .setDescription("Issue key")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ChangelogWsResponse wsResponse = Stream.of(request)
        .map(searchChangelog(dbSession))
        .map(buildResponse())
        .collect(Collectors.toOneElement());
      writeProtobuf(wsResponse, request, response);
    }
  }

  private Function<Request, ChangeLogResults> searchChangelog(DbSession dbSession) {
    return request -> new ChangeLogResults(dbSession, request.mandatoryParam(PARAM_ISSUE));
  }

  private static Function<ChangeLogResults, ChangelogWsResponse> buildResponse() {
    return result -> Stream.of(ChangelogWsResponse.newBuilder())
      .peek(addChanges(result))
      .map(ChangelogWsResponse.Builder::build)
      .collect(Collectors.toOneElement());
  }

  private static Consumer<ChangelogWsResponse.Builder> addChanges(ChangeLogResults results) {
    return response -> results.changes.stream()
      .map(toWsChangelog(results))
      .forEach(response::addChangelog);
  }

  private static Function<FieldDiffs, Changelog> toWsChangelog(ChangeLogResults results) {
    return change -> {
      String userLogin = change.userLogin();
      Changelog.Builder changelogBuilder = Changelog.newBuilder();
      changelogBuilder.setCreationDate(formatDateTime(change.creationDate()));
      UserDto user = userLogin == null ? null : results.users.get(userLogin);
      if (user != null) {
        changelogBuilder.setUser(user.getLogin());
        changelogBuilder.setUserName(user.getName());
        setNullable(emptyToNull(user.getEmail()), changelogBuilder::setEmail);
      }
      change.diffs().entrySet().stream()
        .map(toWsDiff(results))
        .forEach(changelogBuilder::addDiffs);
      return changelogBuilder.build();
    };
  }

  private static Function<Map.Entry<String, FieldDiffs.Diff>, Changelog.Diff> toWsDiff(ChangeLogResults results) {
    return diff -> {
      FieldDiffs.Diff value = diff.getValue();
      Changelog.Diff.Builder diffBuilder = Changelog.Diff.newBuilder();
      String key = diff.getKey();
      if (key.equals(FILE)) {
        diffBuilder.setKey(key);
        setNullable(results.getFileLongName(emptyToNull(value.newValue().toString())), diffBuilder::setNewValue);
        setNullable(results.getFileLongName(emptyToNull(value.oldValue().toString())), diffBuilder::setOldValue);
      } else {
        diffBuilder.setKey(key.equals(TECHNICAL_DEBT) ? EFFORT_CHANGELOG_KEY : key);
        setNullable(emptyToNull(value.newValue().toString()), diffBuilder::setNewValue);
        setNullable(emptyToNull(value.oldValue().toString()), diffBuilder::setOldValue);
      }
      return diffBuilder.build();
    };
  }

  private class ChangeLogResults {
    private final List<FieldDiffs> changes;
    private final Map<String, UserDto> users;
    private final Map<String, ComponentDto> files;

    ChangeLogResults(DbSession dbSession, String issueKey) {
      IssueDto dbIssue = issueFinder.getByKey(dbSession, issueKey);
      this.changes = dbClient.issueChangeDao().selectChangelogByIssue(dbSession, dbIssue.getKey());
      List<String> logins = changes.stream().filter(change -> change.userLogin() != null).map(FieldDiffs::userLogin).collect(Collectors.toList());
      this.users = dbClient.userDao().selectByLogins(dbSession, logins).stream().collect(Collectors.uniqueIndex(UserDto::getLogin));
      this.files = dbClient.componentDao().selectByUuids(dbSession, getFileUuids(changes)).stream().collect(Collectors.uniqueIndex(ComponentDto::uuid, Function.identity()));
    }

    private Set<String> getFileUuids(List<FieldDiffs> changes) {
      return changes.stream()
        .filter(diffs -> diffs.diffs().containsKey(FILE))
        .flatMap(diffs -> Stream.of(diffs.get(FILE).newValue().toString(), diffs.get(FILE).oldValue().toString()))
        .collect(Collectors.toSet());
    }

    @CheckForNull
    String getFileLongName(@Nullable String fileUuid) {
      if (fileUuid == null) {
        return null;
      }
      ComponentDto file = files.get(fileUuid);
      return file == null ? null : file.longName();
    }
  }
}

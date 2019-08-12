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
package org.sonar.server.notification.ws;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.notification.MyNewIssuesNotificationHandler;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.sonar.server.notification.ws.NotificationsWsParameters.ACTION_REMOVE;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_CHANNEL;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_LOGIN;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_PROJECT;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_TYPE;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class RemoveAction implements NotificationsWsAction {
  private final NotificationCenter notificationCenter;
  private final NotificationUpdater notificationUpdater;
  private final Dispatchers dispatchers;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public RemoveAction(NotificationCenter notificationCenter, NotificationUpdater notificationUpdater, Dispatchers dispatchers, DbClient dbClient, ComponentFinder componentFinder,
    UserSession userSession) {
    this.notificationCenter = notificationCenter;
    this.notificationUpdater = notificationUpdater;
    this.dispatchers = dispatchers;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_REMOVE)
      .setDescription("Remove a notification for the authenticated user.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>Authentication if no login is provided</li>" +
        "  <li>System administration if a login is provided</li>" +
        "</ul>")
      .setSince("6.3")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);

    List<NotificationChannel> channels = notificationCenter.getChannels();
    action.createParam(PARAM_CHANNEL)
      .setDescription("Channel through which the notification is sent. For example, notifications can be sent by email.")
      .setPossibleValues(channels)
      .setDefaultValue(EmailNotificationChannel.class.getSimpleName());

    action.createParam(PARAM_TYPE)
      .setDescription("Notification type. Possible values are for:" +
        "<ul>" +
        "  <li>Global notifications: %s</li>" +
        "  <li>Per project notifications: %s</li>" +
        "</ul>",
        dispatchers.getGlobalDispatchers().stream().sorted().collect(Collectors.joining(", ")),
        dispatchers.getProjectDispatchers().stream().sorted().collect(Collectors.joining(", ")))
      .setRequired(true)
      .setExampleValue(MyNewIssuesNotificationHandler.KEY);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RemoveRequest removeRequest = toWsRequest(request);
    remove(removeRequest);

    response.noContent();
  }

  private void remove(RemoveRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkPermissions(request);
      UserDto user = getUser(dbSession, request);
      Optional<ComponentDto> project = searchProject(dbSession, request);
      notificationUpdater.remove(dbSession, request.getChannel(), request.getType(), user, project.orElse(null));
      dbSession.commit();
    }
  }

  private UserDto getUser(DbSession dbSession, RemoveRequest request) {
    String login = request.getLogin() == null ? userSession.getLogin() : request.getLogin();
    return checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' not found", login);
  }

  private Optional<ComponentDto> searchProject(DbSession dbSession, RemoveRequest request) {
    Optional<ComponentDto> project = request.getProject() == null ? empty() : Optional.of(componentFinder.getByKey(dbSession, request.getProject()));
    project.ifPresent(p -> checkRequest(Qualifiers.PROJECT.equals(p.qualifier()) && Scopes.PROJECT.equals(p.scope()),
      "Component '%s' must be a project", request.getProject()));
    return project;
  }

  private void checkPermissions(RemoveRequest request) {
    if (request.getLogin() == null) {
      userSession.checkLoggedIn();
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }

  private RemoveRequest toWsRequest(Request request) {
    RemoveRequest remove = new RemoveRequest()
      .setType(request.mandatoryParam(PARAM_TYPE))
      .setChannel(request.mandatoryParam(PARAM_CHANNEL));
    ofNullable(request.param(PARAM_PROJECT)).ifPresent(remove::setProject);
    ofNullable(request.param(PARAM_LOGIN)).ifPresent(remove::setLogin);

    if (remove.getProject() == null) {
      checkRequest(dispatchers.getGlobalDispatchers().contains(remove.getType()), "Value of parameter '%s' (%s) must be one of: %s",
        PARAM_TYPE,
        remove.getType(),
        dispatchers.getGlobalDispatchers());
    } else {
      checkRequest(dispatchers.getProjectDispatchers().contains(remove.getType()), "Value of parameter '%s' (%s) must be one of: %s",
        PARAM_TYPE,
        remove.getType(),
        dispatchers.getProjectDispatchers());
    }

    return remove;
  }

  static class RemoveRequest {

    private String channel;
    private String login;
    private String project;
    private String type;

    public RemoveRequest setChannel(String channel) {
      this.channel = channel;
      return this;
    }

    public String getChannel() {
      return channel;
    }

    public RemoveRequest setLogin(String login) {
      this.login = login;
      return this;
    }

    public String getLogin() {
      return login;
    }

    public RemoveRequest setProject(String project) {
      this.project = project;
      return this;
    }

    public String getProject() {
      return project;
    }

    public RemoveRequest setType(String type) {
      this.type = type;
      return this;
    }

    public String getType() {
      return type;
    }
  }
}

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
package org.sonar.server.notification.ws;

import java.util.List;
import java.util.Optional;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.notification.MyNewIssuesNotificationDispatcher;
import org.sonar.server.notification.NotificationCenter;
import org.sonar.server.notification.NotificationUpdater;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;

import static java.util.Optional.empty;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.notification.ws.NotificationsWsParameters.ACTION_ADD;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_CHANNEL;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_LOGIN;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_PROJECT;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_TYPE;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class AddAction implements NotificationsWsAction {
  private final NotificationCenter notificationCenter;
  private final NotificationUpdater notificationUpdater;
  private final Dispatchers dispatchers;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public AddAction(NotificationCenter notificationCenter, NotificationUpdater notificationUpdater, Dispatchers dispatchers, DbClient dbClient, ComponentFinder componentFinder,
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
    WebService.NewAction action = context.createAction(ACTION_ADD)
      .setDescription("Add a notification for the authenticated user.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        " <li>Authentication if no login is provided. If a project is provided, requires the 'Browse' permission on the specified project.</li>" +
        " <li>System administration if a login is provided. If a project is provided, requires the 'Browse' permission on the specified project.</li>" +
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
        String.join(", ", dispatchers.getGlobalDispatchers()),
        String.join(", ", dispatchers.getProjectDispatchers()))
      .setRequired(true)
      .setExampleValue(MyNewIssuesNotificationDispatcher.KEY);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    AddRequest addRequest = toWsRequest(request);
    add(addRequest);

    response.noContent();
  }

  private void add(AddRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkPermissions(request);
      UserDto user = getUser(dbSession, request);
      Optional<ComponentDto> project = searchProject(dbSession, request);
      notificationUpdater.add(dbSession, request.getChannel(), request.getType(), user, project.orElse(null));
      dbSession.commit();
    }
  }

  private UserDto getUser(DbSession dbSession, AddRequest request) {
    String login = request.getLogin() == null ? userSession.getLogin() : request.getLogin();
    return checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' not found", login);
  }

  private Optional<ComponentDto> searchProject(DbSession dbSession, AddRequest request) {
    Optional<ComponentDto> project = request.getProject() == null ? empty() : Optional.of(componentFinder.getByKey(dbSession, request.getProject()));
    project.ifPresent(p -> checkRequest(Qualifiers.PROJECT.equals(p.qualifier()) && Scopes.PROJECT.equals(p.scope()),
      "Component '%s' must be a project", request.getProject()));
    project.ifPresent(p -> userSession.checkComponentPermission(UserRole.USER, p));
    return project;
  }

  private void checkPermissions(AddRequest request) {
    if (request.getLogin() == null) {
      userSession.checkLoggedIn();
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }

  private AddRequest toWsRequest(Request request) {
    AddRequest add = new AddRequest()
      .setType(request.mandatoryParam(PARAM_TYPE))
      .setChannel(request.mandatoryParam(PARAM_CHANNEL));
    setNullable(request.param(PARAM_PROJECT), add::setProject);
    setNullable(request.param(PARAM_LOGIN), add::setLogin);

    if (add.getProject() == null) {
      checkRequest(dispatchers.getGlobalDispatchers().contains(add.getType()), "Value of parameter '%s' (%s) must be one of: %s",
        PARAM_TYPE,
        add.getType(),
        dispatchers.getGlobalDispatchers());
    } else {
      checkRequest(dispatchers.getProjectDispatchers().contains(add.getType()), "Value of parameter '%s' (%s) must be one of: %s",
        PARAM_TYPE,
        add.getType(),
        dispatchers.getProjectDispatchers());
    }

    return add;
  }

  private static class AddRequest {

    private String channel;
    private String login;
    private String project;
    private String type;

    public AddRequest setChannel(String channel) {
      this.channel = channel;
      return this;
    }

    public String getChannel() {
      return channel;
    }

    public AddRequest setLogin(String login) {
      this.login = login;
      return this;
    }

    public String getLogin() {
      return login;
    }

    public AddRequest setProject(String project) {
      this.project = project;
      return this;
    }

    public String getProject() {
      return project;
    }

    public AddRequest setType(String type) {
      this.type = type;
      return this;
    }

    public String getType() {
      return type;
    }
  }
}

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

import com.google.common.base.Splitter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.notification.NotificationCenter;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Notifications.ListResponse;
import org.sonarqube.ws.Notifications.Notification;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.core.util.stream.MoreCollectors.toOneElement;
import static org.sonar.server.notification.ws.NotificationsWsParameters.ACTION_LIST;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_LOGIN;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements NotificationsWsAction {

  private static final Splitter PROPERTY_KEY_SPLITTER = Splitter.on(".");

  private final DbClient dbClient;
  private final UserSession userSession;
  private final List<String> channels;
  private final Dispatchers dispatchers;

  public ListAction(NotificationCenter notificationCenter, DbClient dbClient, UserSession userSession, Dispatchers dispatchers) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.channels = notificationCenter.getChannels().stream().map(NotificationChannel::getKey).sorted().collect(MoreCollectors.toList());
    this.dispatchers = dispatchers;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_LIST)
      .setDescription("List notifications of the authenticated user.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>Authentication if no login is provided</li>" +
        "  <li>System administration if a login is provided</li>" +
        "</ul>")
      .setSince("6.3")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ListResponse listResponse = search(request);

    writeProtobuf(listResponse, request, response);
  }

  private ListResponse search(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkPermissions(request);
      UserDto user = getUser(dbSession, request);

      return Stream
        .of(ListResponse.newBuilder())
        .map(r -> r.addAllChannels(channels))
        .map(r -> r.addAllGlobalTypes(dispatchers.getGlobalDispatchers()))
        .map(r -> r.addAllPerProjectTypes(dispatchers.getProjectDispatchers()))
        .map(addNotifications(dbSession, user))
        .map(ListResponse.Builder::build)
        .collect(toOneElement());
    }
  }

  private UserDto getUser(DbSession dbSession, Request request) {
    String login = request.param(PARAM_LOGIN) == null ? userSession.getLogin() : request.param(PARAM_LOGIN);
    return checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' not found", login);
  }

  private UnaryOperator<ListResponse.Builder> addNotifications(DbSession dbSession, UserDto user) {
    return response -> {
      List<PropertyDto> properties = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setUserId(user.getId()).build(), dbSession);
      Map<Long, ComponentDto> componentsById = searchProjects(dbSession, properties);
      Map<String, OrganizationDto> organizationsByUuid = getOrganizations(dbSession, componentsById.values());

      Predicate<PropertyDto> isNotification = prop -> prop.getKey().startsWith("notification.");
      Predicate<PropertyDto> isComponentInDb = prop -> prop.getResourceId() == null || componentsById.containsKey(prop.getResourceId());

      Notification.Builder notification = Notification.newBuilder();

      properties.stream()
        .filter(isNotification)
        .filter(channelAndDispatcherAuthorized())
        .filter(isComponentInDb)
        .map(toWsNotification(notification, organizationsByUuid, componentsById))
        .sorted(comparing(Notification::getProject, nullsFirst(naturalOrder()))
          .thenComparing(comparing(Notification::getChannel))
          .thenComparing(comparing(Notification::getType)))
        .forEach(response::addNotifications);

      return response;
    };
  }

  private Predicate<PropertyDto> channelAndDispatcherAuthorized() {
    return prop -> {
      List<String> key = PROPERTY_KEY_SPLITTER.splitToList(prop.getKey());
      return key.size() == 3
        && channels.contains(key.get(2))
        && isDispatcherAuthorized(prop, key.get(1));
    };
  }

  private boolean isDispatcherAuthorized(PropertyDto prop, String dispatcher) {
    return (prop.getResourceId() != null && dispatchers.getProjectDispatchers().contains(dispatcher)) || dispatchers.getGlobalDispatchers().contains(dispatcher);
  }

  private Map<Long, ComponentDto> searchProjects(DbSession dbSession, List<PropertyDto> properties) {
    Set<Long> componentIds = properties.stream()
      .map(PropertyDto::getResourceId)
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toSet(properties.size()));
    Set<Long> authorizedProjectIds = dbClient.authorizationDao().keepAuthorizedProjectIds(dbSession, componentIds, userSession.getUserId(), UserRole.USER);
    return dbClient.componentDao().selectByIds(dbSession, componentIds)
      .stream()
      .filter(c -> authorizedProjectIds.contains(c.getId()))
      .collect(MoreCollectors.uniqueIndex(ComponentDto::getId));
  }

  private Map<String, OrganizationDto> getOrganizations(DbSession dbSession, Collection<ComponentDto> values) {
    Set<String> organizationUuids = values.stream()
      .map(ComponentDto::getOrganizationUuid)
      .collect(MoreCollectors.toSet());
    return dbClient.organizationDao().selectByUuids(dbSession, organizationUuids)
      .stream()
      .collect(MoreCollectors.uniqueIndex(OrganizationDto::getUuid));
  }

  private static Function<PropertyDto, Notification> toWsNotification(Notification.Builder notification,
    Map<String, OrganizationDto> organizationsByUuid, Map<Long, ComponentDto> projectsById) {
    return property -> {
      notification.clear();
      List<String> propertyKey = Splitter.on(".").splitToList(property.getKey());
      notification.setType(propertyKey.get(1));
      notification.setChannel(propertyKey.get(2));
      setNullable(property.getResourceId(),
        componentId -> populateProjectFields(notification, componentId, organizationsByUuid, projectsById));

      return notification.build();
    };
  }

  private static Notification.Builder populateProjectFields(Notification.Builder notification, Long componentId,
    Map<String, OrganizationDto> organizationsByUuid, Map<Long, ComponentDto> projectsById) {
    ComponentDto project = projectsById.get(componentId);
    String organizationUuid = project.getOrganizationUuid();
    OrganizationDto organizationDto = organizationsByUuid.get(organizationUuid);
    checkArgument(organizationDto != null, "No organization for uuid '%s'", organizationUuid);

    return notification.setOrganization(organizationDto.getKey())
      .setProject(project.getDbKey())
      .setProjectName(project.name());
  }

  private void checkPermissions(Request request) {
    if (request.param(PARAM_LOGIN) == null) {
      userSession.checkLoggedIn();
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }
}

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
package org.sonar.server.notification.ws;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.notification.NotificationChannel;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Notifications.ListResponse;
import org.sonarqube.ws.Notifications.Notification;
import org.sonarqube.ws.Notifications.Notification.Builder;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Optional.ofNullable;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.notification.ws.NotificationsWsParameters.ACTION_LIST;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_LOGIN;
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
    this.channels = notificationCenter.getChannels().stream().map(NotificationChannel::getKey).sorted().toList();
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
      Set<String> globalPermissions = getGlobalPermissions(dbSession, user);
      return Optional
        .of(ListResponse.newBuilder())
        .map(r -> r.addAllChannels(channels))
        .map(r -> r.addAllGlobalTypes(getAllowedGlobalDispatchers(globalPermissions)))
        .map(r -> r.addAllPerProjectTypes(getAllowedProjectDispatchers(globalPermissions)))
        .map(addNotifications(dbSession, user, globalPermissions))
        .map(ListResponse.Builder::build)
        .orElseThrow();
    }
  }

  private Set<String> getGlobalPermissions(DbSession dbSession, UserDto userDto) {
    return dbClient.authorizationDao().selectGlobalPermissions(dbSession, userDto.getUuid());
  }

  private List<String> getAllowedGlobalDispatchers(Set<String> globalPermissions) {
    return dispatchers.getGlobalDispatchers()
      .stream()
      .filter(d -> hasUserPermission(globalPermissions, d))
      .toList();
  }

  private List<String> getAllowedProjectDispatchers(Set<String> globalPermissions) {
    return dispatchers.getProjectDispatchers()
      .stream()
      .filter(d -> hasUserPermission(globalPermissions, d))
      .toList();
  }

  private boolean hasUserPermission(Set<String> globalPermissions, String dispatcher) {
    if (!dispatchers.getPermissionRestrictedDispatchers().containsKey(dispatcher)) {
      return true;
    }
    String globalPermission = dispatchers.getPermissionRestrictedDispatchers().get(dispatcher);
    return globalPermissions.contains(globalPermission);
  }

  private UserDto getUser(DbSession dbSession, Request request) {
    String login = request.param(PARAM_LOGIN) == null ? userSession.getLogin() : request.param(PARAM_LOGIN);
    return checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' not found", login);
  }

  private UnaryOperator<ListResponse.Builder> addNotifications(DbSession dbSession, UserDto user, Set<String> globalPermissions) {
    return response -> {
      List<PropertyDto> properties = dbClient.propertiesDao().selectByQuery(PropertyQuery.builder().setUserUuid(user.getUuid()).build(),
        dbSession);
      Map<String, EntityDto> entitiesByUuid = searchProjects(dbSession, properties);

      Predicate<PropertyDto> isNotification = prop -> prop.getKey().startsWith("notification.");
      Predicate<PropertyDto> isComponentInDb = prop -> prop.getEntityUuid() == null || entitiesByUuid.containsKey(prop.getEntityUuid());

      Notification.Builder notification = Notification.newBuilder();

      Set<PropertyDto> notificationProps = properties.stream()
        .filter(isNotification)
        .filter(channelAndDispatcherAuthorized(globalPermissions))
        .filter(isComponentInDb).collect(Collectors.toSet());

      Set<Notification> activeNotifications = notificationProps.stream()
        .filter(prop -> Boolean.parseBoolean(prop.getValue()))
        .map(p -> toWsNotification(notification, entitiesByUuid, p.getKey(), p.getEntityUuid()))
        .collect(Collectors.toSet());

      Set<Notification> notificationsEnabledByDefault = getEnabledByDefaultNotifications(notificationProps, notification, entitiesByUuid, globalPermissions);

      Stream.concat(activeNotifications.stream(), notificationsEnabledByDefault.stream())
        .sorted(comparing(Notification::getProject, nullsFirst(naturalOrder()))
          .thenComparing(Notification::getChannel)
          .thenComparing(Notification::getType))
        .forEach(response::addNotifications);

      return response;
    };
  }

  private Set<Notification> getEnabledByDefaultNotifications(Set<PropertyDto> notificationProps, Builder notificationBuilder, Map<String, EntityDto> entitiesByUuid,
    Set<String> globalPermissions) {
    Set<String> disabledNotifications = notificationProps.stream()
      .filter(prop -> !Boolean.parseBoolean(prop.getValue()))
      .map(PropertyDto::getKey)
      .collect(Collectors.toSet());

    return dispatchers.getEnabledByDefaultDispatchers()
      .stream()
      .filter(d -> hasUserPermission(globalPermissions, d))
      .flatMap(dispatcher -> channels.stream()
        .filter(channel -> !isNotificationDisabled(disabledNotifications, dispatcher, channel))
        .map(channel -> toWsNotification(notificationBuilder, entitiesByUuid, "notification.%s.%s".formatted(dispatcher, channel), null)))
      .collect(Collectors.toSet());
  }

  private static boolean isNotificationDisabled(Set<String> disabledNotification, String dispatcher, String channel) {
    return disabledNotification.contains("notification.%s.%s".formatted(dispatcher, channel));
  }

  private Predicate<PropertyDto> channelAndDispatcherAuthorized(Set<String> globalPermissions) {
    return prop -> {
      List<String> key = PROPERTY_KEY_SPLITTER.splitToList(prop.getKey());
      return key.size() == 3
        && channels.contains(key.get(2))
        && isDispatcherAuthorized(prop, key.get(1), globalPermissions);
    };
  }

  private boolean isDispatcherAuthorized(PropertyDto prop, String dispatcher, Set<String> globalPermissions) {
    return ((prop.getEntityUuid() != null && dispatchers.getProjectDispatchers().contains(dispatcher))
      || dispatchers.getGlobalDispatchers().contains(dispatcher))
      && hasUserPermission(globalPermissions, dispatcher);
  }

  private Map<String, EntityDto> searchProjects(DbSession dbSession, List<PropertyDto> properties) {
    Set<String> entityUuids = properties.stream()
      .map(PropertyDto::getEntityUuid)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    Set<String> authorizedProjectUuids = dbClient.authorizationDao().keepAuthorizedEntityUuids(dbSession, entityUuids,
      userSession.getUuid(), UserRole.USER);
    return dbClient.entityDao().selectByUuids(dbSession, entityUuids)
      .stream()
      .filter(c -> authorizedProjectUuids.contains(c.getUuid()))
      .collect(Collectors.toMap(EntityDto::getUuid, Function.identity()));
  }

  private static Notification toWsNotification(Builder notification,
    Map<String, EntityDto> projectsByUuid, String key, @Nullable String entityUuid) {
    notification.clear();
    List<String> propertyKey = Splitter.on(".").splitToList(key);
    notification.setType(propertyKey.get(1));
    notification.setChannel(propertyKey.get(2));
    ofNullable(entityUuid).ifPresent(componentUuid -> populateProjectFields(notification, componentUuid, projectsByUuid));
    return notification.build();
  }

  private static void populateProjectFields(Builder notification, String componentUuid, Map<String, EntityDto> projectsByUuid) {
    EntityDto project = projectsByUuid.get(componentUuid);
    notification
      .setProject(project.getKey())
      .setProjectName(project.getName());
  }

  private void checkPermissions(Request request) {
    if (request.param(PARAM_LOGIN) == null) {
      userSession.checkLoggedIn();
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }
}

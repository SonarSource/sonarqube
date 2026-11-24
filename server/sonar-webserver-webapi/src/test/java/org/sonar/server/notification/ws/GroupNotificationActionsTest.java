/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import com.sonarsource.groupnotifications.api.GroupNotificationSubscriptionsService;
import com.sonarsource.groupnotifications.api.model.GroupNotificationSubscription;
import com.sonarsource.groupnotifications.api.model.GroupNotificationType;
import org.sonarqube.ws.Notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class GroupNotificationActionsTest {

  private static final String GROUP_UUID = "group-uuid";
  private static final String GROUP_NAME = "security-team";
  private static final String NOTIFICATION_TYPE = "security-alert-raised";
  private static final String CHANNEL_KEY = "EmailNotificationChannel";
  private static final String BANNER_CHANNEL_KEY = "BannerChannel";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final GroupNotificationSubscriptionsService subscriptionsService = mock(GroupNotificationSubscriptionsService.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final GroupDao groupDao = mock(GroupDao.class);

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.groupDao()).thenReturn(groupDao);
    when(groupDao.selectByUuids(dbSession, List.of(GROUP_UUID)))
      .thenReturn(List.of(new GroupDto().setUuid(GROUP_UUID).setName(GROUP_NAME)));
  }

  @Test
  public void addGroup_whenSystemAdministrator_shouldDelegateSubscription() {
    addGroupAsSystemAdministrator(null);

    verify(subscriptionsService).add(new GroupNotificationSubscription(null, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY));
  }

  @Test
  public void addGroup_whenBannerChannel_shouldDelegateSubscription() {
    addGroupAsSystemAdministrator(BANNER_CHANNEL_KEY);

    verify(subscriptionsService).add(new GroupNotificationSubscription(null, GROUP_UUID, NOTIFICATION_TYPE, BANNER_CHANNEL_KEY));
  }

  @Test
  public void addGroup_whenChannelIsBlank_shouldDefaultToEmail() throws Exception {
    userSession.logIn().setSystemAdministrator();
    mockSupportedTypes();
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    when(request.mandatoryParam(AbstractGroupNotificationAction.PARAM_GROUP_UUID)).thenReturn(GROUP_UUID);
    when(request.mandatoryParam(AbstractGroupNotificationAction.PARAM_TYPE)).thenReturn(NOTIFICATION_TYPE);
    when(request.param(AbstractGroupNotificationAction.PARAM_CHANNEL)).thenReturn(" ");

    new AddGroupAction(userSession, subscriptionsService).handle(request, response);

    verify(subscriptionsService).add(new GroupNotificationSubscription(null, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY));
    verify(response).noContent();
  }

  @Test
  public void addGroup_whenNotSystemAdministrator_shouldRejectRequest() {
    userSession.logIn().setNonSystemAdministrator();
    mockSupportedTypes();
    WsActionTester ws = new WsActionTester(new AddGroupAction(userSession, subscriptionsService));

    var request = ws.newRequest()
      .setParam(AbstractGroupNotificationAction.PARAM_GROUP_UUID, GROUP_UUID)
      .setParam(AbstractGroupNotificationAction.PARAM_TYPE, NOTIFICATION_TYPE);
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);

    verify(subscriptionsService, never()).add(new GroupNotificationSubscription(null, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY));
  }

  @Test
  public void addGroup_whenTypeIsNotAdvertised_shouldRejectRequest() {
    userSession.logIn().setSystemAdministrator();
    when(subscriptionsService.selectAvailableTypes()).thenReturn(List.of());
    WsActionTester ws = new WsActionTester(new AddGroupAction(userSession, subscriptionsService));

    var request = ws.newRequest()
      .setParam(AbstractGroupNotificationAction.PARAM_GROUP_UUID, GROUP_UUID)
      .setParam(AbstractGroupNotificationAction.PARAM_TYPE, "unknown");
    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class);

    verify(subscriptionsService, never()).add(new GroupNotificationSubscription(null, GROUP_UUID, "unknown", CHANNEL_KEY));
  }

  @Test
  public void removeGroup_whenSystemAdministrator_shouldDelegateSubscription() {
    userSession.logIn().setSystemAdministrator();
    mockSupportedTypes();
    WsActionTester ws = new WsActionTester(new RemoveGroupAction(userSession, subscriptionsService));

    ws.newRequest()
      .setParam(AbstractGroupNotificationAction.PARAM_GROUP_UUID, GROUP_UUID)
      .setParam(AbstractGroupNotificationAction.PARAM_TYPE, NOTIFICATION_TYPE)
      .execute();

    verify(subscriptionsService).remove(new GroupNotificationSubscription(null, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY));
  }

  @Test
  public void listGroups_whenSystemAdministrator_shouldReturnSubscriptionsAndGroupNames() {
    userSession.logIn().setSystemAdministrator();
    mockSupportedTypes();
    when(subscriptionsService.selectByOrganization(null))
      .thenReturn(List.of(new GroupNotificationSubscription(null, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY)));
    WsActionTester ws = new WsActionTester(new ListGroupsAction(dbClient, userSession, subscriptionsService));

    Notifications.ListGroupsResponse response = ws.newRequest().executeProtobuf(Notifications.ListGroupsResponse.class);

    assertThat(response.getSubscriptionsList()).hasSize(1);
    assertThat(response.getSubscriptions(0).getGroupUuid()).isEqualTo(GROUP_UUID);
    assertThat(response.getSubscriptions(0).getGroupName()).isEqualTo(GROUP_NAME);
    assertThat(response.getSubscriptions(0).getNotificationType()).isEqualTo(NOTIFICATION_TYPE);
    assertThat(response.getSubscriptions(0).getChannelKey()).isEqualTo(CHANNEL_KEY);
    assertThat(response.getAvailableTypesList()).containsExactlyInAnyOrder(
      Notifications.GroupNotificationType.newBuilder().setNotificationType(NOTIFICATION_TYPE).setChannelKey(CHANNEL_KEY).build(),
      Notifications.GroupNotificationType.newBuilder().setNotificationType(NOTIFICATION_TYPE).setChannelKey(BANNER_CHANNEL_KEY).build());
  }

  @Test
  public void listGroups_whenNoSubscriptions_shouldReturnEmptyList() {
    userSession.logIn().setSystemAdministrator();
    mockSupportedTypes();
    when(subscriptionsService.selectByOrganization(null)).thenReturn(List.of());
    WsActionTester ws = new WsActionTester(new ListGroupsAction(dbClient, userSession, subscriptionsService));

    Notifications.ListGroupsResponse response = ws.newRequest().executeProtobuf(Notifications.ListGroupsResponse.class);

    assertThat(response.getSubscriptionsList()).isEmpty();
    verifyNoInteractions(groupDao);
  }

  @Test
  public void listGroupSubscriptions_whenAuthenticated_shouldReturnCurrentUserGroupSubscriptions() {
    userSession.logIn();
    String userUuid = userSession.getUuid();
    when(subscriptionsService.selectForUser(userUuid, null))
      .thenReturn(List.of(new GroupNotificationSubscription(null, GROUP_UUID, NOTIFICATION_TYPE, CHANNEL_KEY)));
    WsActionTester ws = new WsActionTester(new ListGroupSubscriptionsAction(dbClient, userSession, subscriptionsService));

    Notifications.ListGroupSubscriptionsResponse response = ws.newRequest()
      .executeProtobuf(Notifications.ListGroupSubscriptionsResponse.class);

    assertThat(response.getGroupSubscriptionsList()).hasSize(1);
    assertThat(response.getGroupSubscriptions(0).getGroupName()).isEqualTo(GROUP_NAME);
    assertThat(response.getGroupSubscriptions(0).getNotificationType()).isEqualTo(NOTIFICATION_TYPE);
    assertThat(response.getGroupSubscriptions(0).getChannelKey()).isEqualTo(CHANNEL_KEY);
  }

  @Test
  public void listGroupSubscriptions_whenNoSubscriptions_shouldReturnEmptyList() {
    userSession.logIn();
    String userUuid = userSession.getUuid();
    when(subscriptionsService.selectForUser(userUuid, null)).thenReturn(List.of());
    WsActionTester ws = new WsActionTester(new ListGroupSubscriptionsAction(dbClient, userSession, subscriptionsService));

    Notifications.ListGroupSubscriptionsResponse response = ws.newRequest()
      .executeProtobuf(Notifications.ListGroupSubscriptionsResponse.class);

    assertThat(response.getGroupSubscriptionsList()).isEmpty();
    verify(dbClient, never()).openSession(false);
  }

  @Test
  public void listGroupSubscriptions_whenAnonymous_shouldRejectRequest() {
    userSession.anonymous();
    WsActionTester ws = new WsActionTester(new ListGroupSubscriptionsAction(dbClient, userSession, subscriptionsService));
    var request = ws.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  private void mockSupportedTypes() {
    when(subscriptionsService.selectAvailableTypes())
      .thenReturn(List.of(
        new GroupNotificationType(NOTIFICATION_TYPE, CHANNEL_KEY),
        new GroupNotificationType(NOTIFICATION_TYPE, BANNER_CHANNEL_KEY)));
  }

  private void addGroupAsSystemAdministrator(String channelKey) {
    userSession.logIn().setSystemAdministrator();
    mockSupportedTypes();
    WsActionTester ws = new WsActionTester(new AddGroupAction(userSession, subscriptionsService));
    var request = ws.newRequest()
      .setParam(AbstractGroupNotificationAction.PARAM_GROUP_UUID, GROUP_UUID)
      .setParam(AbstractGroupNotificationAction.PARAM_TYPE, NOTIFICATION_TYPE);
    if (channelKey != null) {
      request.setParam(AbstractGroupNotificationAction.PARAM_CHANNEL, channelKey);
    }
    request.execute();
  }
}

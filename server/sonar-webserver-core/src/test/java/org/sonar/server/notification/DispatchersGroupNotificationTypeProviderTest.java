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
package org.sonar.server.notification;

import java.util.List;
import org.junit.Test;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.ws.Dispatchers;
import com.sonarsource.groupnotifications.api.model.GroupNotificationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DispatchersGroupNotificationTypeProviderTest {

  @Test
  public void selectAvailableTypes_returnsTheExistingSecurityAlertTypeForEmailAndBanner() {
    Dispatchers dispatchers = mock(Dispatchers.class);
    when(dispatchers.getGroupSubscriptionDispatchers()).thenReturn(List.of("security-alert-raised"));

    List<GroupNotificationType> result = new DispatchersGroupNotificationTypeProvider(dispatchers).selectAvailableTypes();

    assertThat(result).containsExactly(
      new GroupNotificationType("security-alert-raised", EmailNotificationChannel.class.getSimpleName()),
      new GroupNotificationType("security-alert-raised", "BannerChannel"));
  }

  @Test
  public void selectAvailableTypes_withoutSecurityAlerts_shouldOnlyReturnEmailTypes() {
    Dispatchers dispatchers = mock(Dispatchers.class);
    when(dispatchers.getGroupSubscriptionDispatchers()).thenReturn(List.of("other-notification"));

    List<GroupNotificationType> result = new DispatchersGroupNotificationTypeProvider(dispatchers).selectAvailableTypes();

    assertThat(result).containsExactly(
      new GroupNotificationType("other-notification", EmailNotificationChannel.class.getSimpleName()));
  }
}

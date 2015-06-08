/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.notification;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.notifications.NotificationChannel;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationCenterTest {

  @Mock
  private NotificationChannel emailChannel;

  @Mock
  private NotificationChannel gtalkChannel;

  private NotificationCenter notificationCenter;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    NotificationDispatcherMetadata metadata1 = NotificationDispatcherMetadata.create("Dispatcher1").setProperty("global", "true").setProperty("on-project", "true");
    NotificationDispatcherMetadata metadata2 = NotificationDispatcherMetadata.create("Dispatcher2").setProperty("global", "true");
    NotificationDispatcherMetadata metadata3 = NotificationDispatcherMetadata.create("Dispatcher3").setProperty("global", "FOO").setProperty("on-project", "BAR");

    notificationCenter = new NotificationCenter(
        new NotificationDispatcherMetadata[] {metadata1, metadata2, metadata3},
        new NotificationChannel[] {emailChannel, gtalkChannel}
        );
  }

  @Test
  public void shouldReturnChannels() {
    assertThat(notificationCenter.getChannels()).containsOnly(emailChannel, gtalkChannel);
  }

  @Test
  public void shouldReturnDispatcherKeysForSpecificPropertyValue() {
    assertThat(notificationCenter.getDispatcherKeysForProperty("global", "true")).containsOnly("Dispatcher1", "Dispatcher2");
  }

  @Test
  public void shouldReturnDispatcherKeysForExistenceOfProperty() {
    assertThat(notificationCenter.getDispatcherKeysForProperty("on-project", null)).containsOnly("Dispatcher1", "Dispatcher3");
  }

  @Test
  public void testDefaultConstructors() {
    notificationCenter = new NotificationCenter(new NotificationChannel[] {emailChannel});
    assertThat(notificationCenter.getChannels()).hasSize(1);

    notificationCenter = new NotificationCenter();
    assertThat(notificationCenter.getChannels()).hasSize(0);

    notificationCenter = new NotificationCenter(new NotificationDispatcherMetadata[] {NotificationDispatcherMetadata.create("Dispatcher1").setProperty("global", "true")});
    assertThat(notificationCenter.getChannels()).hasSize(0);
    assertThat(notificationCenter.getDispatcherKeysForProperty("global", null)).hasSize(1);
  }

}

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
package org.sonar.server.notification;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.Notification;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationTest {

  private Notification notification;

  @Before
  public void init() {
    notification = new Notification("alerts").setDefaultMessage("There are new alerts").setFieldValue("alertCount", "42");
  }

  @Test
  public void shouldReturnType() {
    assertThat(notification.getType()).isEqualTo("alerts");
  }

  @Test
  public void shouldReturnDefaultMessage() {
    assertThat(notification.getDefaultMessage()).isEqualTo("There are new alerts");
  }

  @Test
  public void shouldReturnToStringIfDefaultMessageNotSet() {
    notification = new Notification("alerts").setFieldValue("alertCount", "42");
    System.out.println(notification);
    assertThat(notification.getDefaultMessage()).contains("type='alerts'");
    assertThat(notification.getDefaultMessage()).contains("fields={alertCount=42}");
  }

  @Test
  public void shouldReturnField() {
    assertThat(notification.getFieldValue("alertCount")).isEqualTo("42");
    assertThat(notification.getFieldValue("fake")).isNull();

    // default message is stored as field as well
    assertThat(notification.getFieldValue("default_message")).isEqualTo("There are new alerts");
  }

  @Test
  public void shouldEqual() {
    assertThat(notification.equals("")).isFalse();
    assertThat(notification.equals(null)).isFalse();
    assertThat(notification.equals(notification)).isTrue();

    Notification otherNotif = new Notification("alerts").setDefaultMessage("There are new alerts").setFieldValue("alertCount", "42");
    assertThat(otherNotif).isEqualTo(notification);

    otherNotif = new Notification("alerts").setDefaultMessage("There are new alerts").setFieldValue("alertCount", "15000");
    assertThat(otherNotif).isNotEqualTo(notification);
  }

}

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
package org.sonar.api.notifications;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {
  @Test
  void getType_shouldReturnTypePassedInConstructor() {
    Notification notification = new Notification("type");
    assertThat(notification.getType()).isEqualTo("type");
  }

  @Test
  void getDefaultMessage_whenNotDefined_returnToString() {
    Notification notification = new Notification("type");
    assertThat(notification.getDefaultMessage()).isEqualTo("Notification{type='type', fields={}}");
  }

  @Test
  void getDefaultMessage_whenDefined_returnDefinedMessage() {
    Notification notification = new Notification("type");
    notification.setDefaultMessage("default");
    assertThat(notification.getDefaultMessage()).isEqualTo("default");
  }

  @Test
  void getFieldValue_whenNotDefined_shouldReturnNull() {
    Notification notification = new Notification("type");
    assertThat(notification.getFieldValue("unknown")).isNull();
  }

  @Test
  void getFieldValue_whenDefined_shouldReturnValue() {
    Notification notification = new Notification("type");
    notification.setFieldValue("key", "value");
    assertThat(notification.getFieldValue("key")).isEqualTo("value");
  }

  @Test
  void equals_whenTypeAndFieldsMatch_shouldReturnTrue() {
    Notification notification1 = new Notification("type");
    Notification notification2 = new Notification("type");

    notification1.setFieldValue("key", "value");
    notification2.setFieldValue("key", "value");

    assertThat(notification1)
      .hasSameHashCodeAs(notification2)
      .isEqualTo(notification2);
  }

  @Test
  void equals_whenTypeDontMatch_shouldReturnFalse() {
    Notification notification1 = new Notification("type1");
    Notification notification2 = new Notification("type2");

    assertThat(notification1).isNotEqualTo(notification2);
  }

  @Test
  void equals_whenFieldsDontMatch_shouldReturnFalse() {
    Notification notification1 = new Notification("type");
    Notification notification2 = new Notification("type");

    notification1.setFieldValue("key", "value1");
    notification2.setFieldValue("key", "value2");

    assertThat(notification1).isNotEqualTo(notification2);
  }

  @Test
  void toString_shouldReturnTypeAndFields() {
    Notification notification1 = new Notification("type");

    notification1.setFieldValue("key", "value1");

    assertThat(notification1).hasToString("Notification{type='type', fields={key=value1}}");
  }
}

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

package org.sonarqube.ws;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageFormatterTest {

  @Test
  public void print() {
    WsPermissions.Permission.Builder message = WsPermissions.Permission.newBuilder()
      .setName("permission-name")
      .setKey("permission-key")
      .setDescription("permission-description")
      .setUsersCount(1984)
      .setGroupsCount(42);

    String result = MessageFormatter.print(message);

    assertThat(result).isEqualTo("org.sonarqube.ws.WsPermissions.Permission.Builder" +
      "[key: \"permission-key\" name: \"permission-name\" description: \"permission-description\" usersCount: 1984 groupsCount: 42]");
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.tester;

import org.junit.Test;
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class MockUserSessionTest {
  @Test
  public void set_mock_session() {
    GroupDto group = newGroupDto();
    MockUserSession mock = new MockUserSession("foo").setGroups(group);

    assertThat(mock.getLogin()).isEqualTo("foo");
    assertThat(mock.getUuid()).isEqualTo("foouuid");
    assertThat(mock.getGroups()).extracting(GroupDto::getUuid).containsOnly(group.getUuid());
    assertThat(mock.isLoggedIn()).isTrue();
  }
}

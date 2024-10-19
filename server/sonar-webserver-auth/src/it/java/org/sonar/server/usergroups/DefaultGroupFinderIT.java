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
package org.sonar.server.usergroups;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultGroupFinderIT {

  @Rule
  public DbTester db = DbTester.create();

  private final DefaultGroupFinder underTest = new DefaultGroupFinder(db.getDbClient());

  @Test
  public void find_default_group() {
    GroupDto defaultGroup = db.users().insertDefaultGroup();

    GroupDto result = underTest.findDefaultGroup(db.getSession());

    assertThat(result.getUuid()).isEqualTo(defaultGroup.getUuid());
    assertThat(result.getName()).isEqualTo("sonar-users");
  }

  @Test
  public void fail_with_ISE_when_no_default_group() {
    db.users().insertGroup();
    DbSession session = db.getSession();

    assertThatThrownBy(() -> underTest.findDefaultGroup(session))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default group cannot be found");
  }

  @Test
  public void fail_with_ISE_when_default_group_does_not_exist() {
    GroupDto defaultGroup = db.users().insertDefaultGroup();
    db.getDbClient().groupDao().deleteByUuid(db.getSession(), defaultGroup.getUuid(), defaultGroup.getName());
    DbSession session = db.getSession();

    assertThatThrownBy(() -> underTest.findDefaultGroup(session))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default group cannot be found");
  }
}

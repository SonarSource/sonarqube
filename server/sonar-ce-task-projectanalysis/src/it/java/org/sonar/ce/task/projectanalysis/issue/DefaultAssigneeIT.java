/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TestSettingsRepository;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserIdDto;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultAssigneeIT {

  public static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public DbTester db = DbTester.create();

  private final MapSettings settings = new MapSettings();
  private final ConfigurationRepository settingsRepository = new TestSettingsRepository(settings.asConfig());
  private final DefaultAssignee underTest = new DefaultAssignee(db.getDbClient(), settingsRepository);

  @Test
  public void no_default_assignee() {
    assertThat(underTest.loadDefaultAssigneeUserId()).isNull();
  }

  @Test
  public void set_default_assignee() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    UserDto userDto = db.users().insertUser("erik");

    UserIdDto userId = underTest.loadDefaultAssigneeUserId();
    assertThat(userId).isNotNull();
    assertThat(userId.getUuid()).isEqualTo(userDto.getUuid());
    assertThat(userId.getLogin()).isEqualTo(userDto.getLogin());
  }

  @Test
  public void configured_login_does_not_exist() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");

    assertThat(underTest.loadDefaultAssigneeUserId()).isNull();
  }

  @Test
  public void configured_login_is_disabled() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    db.users().insertUser(user -> user.setLogin("erik").setActive(false));

    assertThat(underTest.loadDefaultAssigneeUserId()).isNull();
  }

  @Test
  public void default_assignee_is_cached() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    UserDto userDto = db.users().insertUser("erik");
    UserIdDto userId = underTest.loadDefaultAssigneeUserId();
    assertThat(userId).isNotNull();
    assertThat(userId.getUuid()).isEqualTo(userDto.getUuid());
    assertThat(userId.getLogin()).isEqualTo(userDto.getLogin());

    // The setting is updated but the assignee hasn't changed
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "other");
    userId = underTest.loadDefaultAssigneeUserId();
    assertThat(userId).isNotNull();
    assertThat(userId.getUuid()).isEqualTo(userDto.getUuid());
    assertThat(userId.getLogin()).isEqualTo(userDto.getLogin());
  }
}

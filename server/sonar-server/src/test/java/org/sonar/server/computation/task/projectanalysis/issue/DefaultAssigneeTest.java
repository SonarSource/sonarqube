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
package org.sonar.server.computation.task.projectanalysis.issue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.SettingsRepository;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultAssigneeTest {

  public static final String PROJECT_KEY = "PROJECT_KEY";

  TreeRootHolderRule rootHolder = mock(TreeRootHolderRule.class, Mockito.RETURNS_DEEP_STUBS);
  UserIndex userIndex = mock(UserIndex.class);
  Settings settings = new Settings();
  SettingsRepository settingsRepository = mock(SettingsRepository.class);

  DefaultAssignee underTest = new DefaultAssignee(rootHolder, userIndex, settingsRepository);

  @Before
  public void before() {
    when(rootHolder.getRoot()).thenReturn(mock(Component.class));
    when(settingsRepository.getSettings(rootHolder.getRoot())).thenReturn(settings);
  }

  @Test
  public void no_default_assignee() {
    assertThat(underTest.getLogin()).isNull();
  }

  @Test
  public void default_assignee() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    when(userIndex.getNullableByLogin("erik")).thenReturn(new UserDoc().setLogin("erik").setActive(true));

    assertThat(underTest.getLogin()).isEqualTo("erik");
  }

  @Test
  public void configured_login_does_not_exist() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    when(userIndex.getNullableByLogin("erik")).thenReturn(null);

    assertThat(underTest.getLogin()).isNull();
  }

  @Test
  public void configured_login_is_disabled() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    when(userIndex.getNullableByLogin("erik")).thenReturn(new UserDoc().setLogin("erik").setActive(false));

    assertThat(underTest.getLogin()).isNull();
  }
}

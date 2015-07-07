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
package org.sonar.server.computation.issue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.ProjectSettingsRepository;
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
  ProjectSettingsRepository settingsRepository = mock(ProjectSettingsRepository.class);

  DefaultAssignee underTest = new DefaultAssignee(rootHolder, userIndex, settingsRepository);

  @Before
  public void before() {
    when(rootHolder.getRoot().getKey()).thenReturn(PROJECT_KEY);
    when(settingsRepository.getProjectSettings(PROJECT_KEY)).thenReturn(settings);
  }

  @Test
  public void no_default_assignee() {
    assertThat(underTest.getLogin()).isNull();
  }

  @Test
  public void default_assignee() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    when(userIndex.getNullableByLogin("erik")).thenReturn(new UserDoc().setLogin("erik"));

    assertThat(underTest.getLogin()).isEqualTo("erik");
  }

  @Test
  public void configured_login_does_not_exist() {
    settings.setProperty(CoreProperties.DEFAULT_ISSUE_ASSIGNEE, "erik");
    when(userIndex.getNullableByLogin("erik")).thenReturn(null);

    assertThat(underTest.getLogin()).isNull();
  }
}

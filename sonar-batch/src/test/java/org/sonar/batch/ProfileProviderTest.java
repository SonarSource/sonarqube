/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProfileProviderTest {
  @Test
  public void shouldProvideProfile() {
    ProfileProvider provider = new ProfileProvider();
    ProfileLoader loader = mock(ProfileLoader.class);
    Project project = new Project("project");
    RulesProfile profile = RulesProfile.create();
    Languages languages = mock(Languages.class);
    when(loader.load(eq(project), any(Settings.class), eq(languages))).thenReturn(profile);

    assertThat(provider.provide(project, loader, new Settings(), languages), is(profile));
    assertThat(provider.provide(project, loader, new Settings(), languages), is(profile));
    verify(loader).load(eq(project), any(Settings.class), eq(languages));
    verifyNoMoreInteractions(loader);
  }
}

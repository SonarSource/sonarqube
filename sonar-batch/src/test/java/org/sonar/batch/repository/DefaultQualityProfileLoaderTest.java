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
package org.sonar.batch.repository;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import org.junit.Before;
import org.junit.Test;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.protocol.input.QProfile;

import java.util.Collection;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultQualityProfileLoaderTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private DefaultQualityProfileLoader qpLoader;
  private DefaultProjectRepositoriesFactory factory;
  private ProjectRepositories projectRepositories;

  @Before
  public void setUp() {
    projectRepositories = new ProjectRepositories();
    projectRepositories.addQProfile(new QProfile("profile", "name", "lang", new Date()));

    factory = mock(DefaultProjectRepositoriesFactory.class);
    when(factory.create()).thenReturn(projectRepositories);
    qpLoader = new DefaultQualityProfileLoader(factory);
  }

  @Test
  public void test() {
    Collection<QProfile> loaded = qpLoader.load("project", null);

    assertThat(loaded).hasSize(1);
    assertThat(loaded.iterator().next().key()).isEqualTo("profile");
    verify(factory).create();
    verifyNoMoreInteractions(factory);
  }

  @Test
  public void testNoProfile() {
    projectRepositories = new ProjectRepositories();
    when(factory.create()).thenReturn(projectRepositories);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("No quality profiles");

    qpLoader.load("project", null);
  }
}

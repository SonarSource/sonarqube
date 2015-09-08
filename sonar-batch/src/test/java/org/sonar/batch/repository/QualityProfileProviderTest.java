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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.sonar.batch.protocol.input.QProfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.junit.Test;
import org.sonar.batch.analysis.AnalysisProperties;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.mockito.Mock;
import org.junit.Before;

public class QualityProfileProviderTest {
  private QualityProfileProvider qualityProfileProvider;

  @Mock
  private QualityProfileLoader loader;
  @Mock
  private ProjectReactor projectReactor;
  @Mock
  private AnalysisMode mode;
  @Mock
  private AnalysisProperties props;

  private Collection<QProfile> response;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    qualityProfileProvider = new QualityProfileProvider();

    ProjectDefinition root = mock(ProjectDefinition.class);
    when(root.getKeyWithBranch()).thenReturn("project");
    when(projectReactor.getRoot()).thenReturn(root);

    response = new ArrayList<QProfile>(1);
    response.add(new QProfile("profile", "name", "lang", new Date()));
  }

  @Test
  public void testProvide() {
    when(loader.load("project", null)).thenReturn(response);
    ModuleQProfiles qps = qualityProfileProvider.provide(projectReactor, loader, props, mode);
    assertResponse(qps);

    verify(loader).load("project", null);
  }

  @Test
  public void testProfileProp() {
    when(loader.load("project", "custom")).thenReturn(response);
    when(props.property(ModuleQProfiles.SONAR_PROFILE_PROP)).thenReturn("custom");

    ModuleQProfiles qps = qualityProfileProvider.provide(projectReactor, loader, props, mode);
    assertResponse(qps);

    verify(loader).load("project", "custom");
  }

  private void assertResponse(ModuleQProfiles qps) {
    assertThat(qps.findAll()).hasSize(1);
    assertThat(qps.findAll()).extracting("key").containsExactly("profile");
  }
}

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
package org.sonar.server.startup;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDao;
import org.sonar.server.property.InternalProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueFlagResetSetupTaskTest {

  @Mock
  private DbClient dbClient;
  
  @Mock
  private InternalProperties internalProperties;
  
  
  @Mock
  private SonarQubeVersion sonarQubeVersion;
  
  @Mock
  private SonarRuntime sonarRuntime;
  
  @Mock
  private DbSession dbSession;
  
  @Mock
  private IssueDao issueDao;

  @InjectMocks
  private IssueFlagResetSetupTask underTest;

  @Test
  public void start_should_set_initial_version_when_current_version_not_stored() {
    Version runtimeVersion = Version.create(10, 5);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DEVELOPER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.empty());

    underTest.start();

    verify(internalProperties).write("sonarqube.currentVersion", "10.5");
    verify(issueDao, never()).resetFlagFromSonarQubeUpdate(any());
  }

  @Test
  public void start_should_reset_flag_when_major_version_upgraded() {
    Version runtimeVersion = Version.create(11, 0);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DEVELOPER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.of("10.5"));
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(issueDao.resetFlagFromSonarQubeUpdate(dbSession)).thenReturn(5);

    underTest.start();

    verify(internalProperties).write("sonarqube.currentVersion", "11.0");
    verify(issueDao).resetFlagFromSonarQubeUpdate(dbSession);
    verify(dbSession).commit();
  }

  @Test
  public void start_should_reset_flag_when_minor_version_upgraded() {
    Version runtimeVersion = Version.create(10, 6);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DEVELOPER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.of("10.5"));
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(issueDao.resetFlagFromSonarQubeUpdate(dbSession)).thenReturn(3);

    underTest.start();

    verify(internalProperties).write("sonarqube.currentVersion", "10.6");
    verify(issueDao).resetFlagFromSonarQubeUpdate(dbSession);
    verify(dbSession).commit();
  }

  @Test
  public void start_should_reset_flag_when_complex_version_numbers() {
    Version runtimeVersion = Version.create(2025, 4);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DEVELOPER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.of("2024.12"));
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(issueDao.resetFlagFromSonarQubeUpdate(dbSession)).thenReturn(10);

    underTest.start();

    verify(internalProperties).write("sonarqube.currentVersion", "2025.4");
    verify(issueDao).resetFlagFromSonarQubeUpdate(dbSession);
    verify(dbSession).commit();
  }

  @Test
  public void start_should_not_reset_flag_when_same_major_minor_version() {
    Version runtimeVersion = Version.create(10, 5);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DEVELOPER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.of("10.5"));

    underTest.start();

    verify(internalProperties, never()).write(eq("sonarqube.currentVersion"), any());
    verify(issueDao, never()).resetFlagFromSonarQubeUpdate(any());
  }

  @Test
  public void start_should_not_reset_flag_when_patch_version_difference() {
    Version runtimeVersion = Version.create(10, 5);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DEVELOPER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.of("10.5.1"));

    underTest.start();

    verify(internalProperties, never()).write(eq("sonarqube.currentVersion"), any());
    verify(issueDao, never()).resetFlagFromSonarQubeUpdate(any());
  }

  @Test
  public void start_should_update_version_when_stored_version_format_invalid() {
    Version runtimeVersion = Version.create(10, 5);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DEVELOPER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.of("invalid-version"));

    underTest.start();

    verify(internalProperties).write("sonarqube.currentVersion", "10.5");
    verify(issueDao, never()).resetFlagFromSonarQubeUpdate(any());
  }

  
  @Test
  public void start_should_skip_when_community_edition() {
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.COMMUNITY);
    
    underTest.start();
    
    verify(internalProperties, never()).read(any());
    verify(sonarQubeVersion, never()).get();
    verify(issueDao, never()).resetFlagFromSonarQubeUpdate(any());
  }
  
  @Test
  public void start_should_run_when_developer_edition() {
    Version runtimeVersion = Version.create(10, 5);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DEVELOPER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.empty());
    
    underTest.start();
    
    verify(internalProperties).read("sonarqube.currentVersion");
    verify(sonarQubeVersion).get();
    verify(internalProperties).write("sonarqube.currentVersion", "10.5");
  }

  @Test
  public void start_should_run_when_enterprise_edition() {
    Version runtimeVersion = Version.create(10, 5);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.ENTERPRISE);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.empty());
    
    underTest.start();
    
    verify(internalProperties).read("sonarqube.currentVersion");
    verify(sonarQubeVersion).get();
    verify(internalProperties).write("sonarqube.currentVersion", "10.5");
  }

  @Test
  public void start_should_run_when_datacenter_edition() {
    Version runtimeVersion = Version.create(10, 5);
    when(sonarRuntime.getEdition()).thenReturn(SonarEdition.DATACENTER);
    when(sonarQubeVersion.get()).thenReturn(runtimeVersion);
    when(internalProperties.read("sonarqube.currentVersion")).thenReturn(Optional.empty());
    
    underTest.start();
    
    verify(internalProperties).read("sonarqube.currentVersion");
    verify(sonarQubeVersion).get();
    verify(internalProperties).write("sonarqube.currentVersion", "10.5");
  }
}
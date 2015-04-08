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
package org.sonar.server.platform.monitoring;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.user.SecurityRealmFactory;

import java.io.File;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarQubeMonitorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Settings settings = new Settings();
  Server server = mock(Server.class);

  @Test
  public void getServerId() throws Exception {
    when(server.getStartedAt()).thenReturn(DateUtils.parseDate("2015-01-01"));
    SonarQubeMonitor monitor = new SonarQubeMonitor(settings, new SecurityRealmFactory(settings), server);

    LinkedHashMap<String, Object> attributes = monitor.attributes();
    assertThat(attributes).containsKeys("Server ID", "Version");
  }

  @Test
  public void official_distribution() throws Exception {
    File rootDir = temp.newFolder();
    FileUtils.write(new File(rootDir, SonarQubeMonitor.BRANDING_FILE_PATH), "1.2");

    when(server.getRootDir()).thenReturn(rootDir);
    SonarQubeMonitor monitor = new SonarQubeMonitor(settings, new SecurityRealmFactory(settings), server);

    LinkedHashMap<String, Object> attributes = monitor.attributes();
    assertThat(attributes).containsEntry("Official Distribution", Boolean.TRUE);
  }

  @Test
  public void not_an_official_distribution() throws Exception {
    File rootDir = temp.newFolder();
    // branding file is missing
    when(server.getRootDir()).thenReturn(rootDir);
    SonarQubeMonitor monitor = new SonarQubeMonitor(settings, new SecurityRealmFactory(settings), server);

    LinkedHashMap<String, Object> attributes = monitor.attributes();
    assertThat(attributes).containsEntry("Official Distribution", Boolean.FALSE);
  }
}

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
package org.sonar.server.platform.platformlevel;

import java.util.Optional;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.platform.db.migration.charset.DatabaseCharsetChecker;
import org.sonar.server.plugins.ServerPluginRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

public class PlatformLevel2Test {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private Properties props = new Properties();

  @Before
  public void setUp() throws Exception {
    // these are mandatory settings declared by bootstrap process
    props.setProperty(PATH_HOME.getKey(), tempFolder.newFolder().getAbsolutePath());
    props.setProperty(PATH_DATA.getKey(), tempFolder.newFolder().getAbsolutePath());
    props.setProperty(PATH_TEMP.getKey(), tempFolder.newFolder().getAbsolutePath());
  }

  @Test
  public void add_all_components_by_default() {
    var parentContainer = mock(SpringComponentContainer.class);
    var container = mock(SpringComponentContainer.class);
    var platform = mock(PlatformLevel.class);
    var webserver = mock(NodeInformation.class);

    when(parentContainer.createChild()).thenReturn(container);
    when(platform.getContainer()).thenReturn(parentContainer);
    when(parentContainer.getOptionalComponentByType(any())).thenReturn(Optional.empty());
    when(container.getOptionalComponentByType(NodeInformation.class)).thenReturn(Optional.of(webserver));
    when(webserver.isStartupLeader()).thenReturn(true);

    PlatformLevel2 underTest = new PlatformLevel2(platform);
    underTest.configure();

    verify(container).add(ServerPluginRepository.class);
    verify(container).add(DatabaseCharsetChecker.class);
    verify(container, atLeastOnce()).add(any());
  }

  @Test
  public void do_not_add_all_components_when_startup_follower() {
    var parentContainer = mock(SpringComponentContainer.class);
    var container = mock(SpringComponentContainer.class);
    var platform = mock(PlatformLevel.class);
    var webserver = mock(NodeInformation.class);

    when(parentContainer.createChild()).thenReturn(container);
    when(platform.getContainer()).thenReturn(parentContainer);
    when(parentContainer.getOptionalComponentByType(any())).thenReturn(Optional.empty());
    when(container.getOptionalComponentByType(NodeInformation.class)).thenReturn(Optional.of(webserver));
    when(webserver.isStartupLeader()).thenReturn(false);

    PlatformLevel2 underTest = new PlatformLevel2(platform);
    underTest.configure();

    verify(container).add(ServerPluginRepository.class);
    verify(container, never()).add(DatabaseCharsetChecker.class);
    verify(container, atLeastOnce()).add(any());
  }


}

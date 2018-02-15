/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.WebServer;
import org.sonar.server.platform.db.migration.charset.DatabaseCharsetChecker;
import org.sonar.server.startup.ClusterConfigurationCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
    PlatformLevel1 level1 = new PlatformLevel1(mock(Platform.class), props);
    level1.configure();

    PlatformLevel2 underTest = new PlatformLevel2(level1);
    underTest.configure();

    // some level1 components
    assertThat(underTest.getOptional(WebServer.class)).isPresent();
    assertThat(underTest.getOptional(System2.class)).isPresent();

    // level2 component that does not depend on cluster state
    assertThat(underTest.getOptional(PluginRepository.class)).isPresent();

    // level2 component that is injected only on "startup leaders"
    assertThat(underTest.getOptional(DatabaseCharsetChecker.class)).isPresent();
  }

  @Test
  public void do_not_add_all_components_when_startup_follower() {
    props.setProperty("sonar.cluster.enabled", "true");
    PlatformLevel1 level1 = new PlatformLevel1(mock(Platform.class), props);
    level1.configure();

    PlatformLevel2 underTest = new PlatformLevel2(level1);
    underTest.configure();

    assertThat(underTest.get(WebServer.class).isStartupLeader()).isFalse();

    // level2 component that does not depend on cluster state
    assertThat(underTest.getOptional(PluginRepository.class)).isPresent();

    // level2 component that is injected only on "startup leaders"
    assertThat(underTest.getOptional(DatabaseCharsetChecker.class)).isNotPresent();
  }

  @Test
  public void add_ClusterConfigurationCheck_when_cluster_mode_activated() {
    props.setProperty("sonar.cluster.enabled", "true");
    PlatformLevel1 level1 = new PlatformLevel1(mock(Platform.class), props);
    level1.configure();

    PlatformLevel2 underTest = new PlatformLevel2(level1);
    underTest.configure();

    assertThat(underTest.getOptional(ClusterConfigurationCheck.class)).isPresent();
  }

  @Test
  public void do_NOT_add_ClusterConfigurationCheck_when_cluster_mode_NOT_activated() {
    props.setProperty("sonar.cluster.enabled", "false");
    PlatformLevel1 level1 = new PlatformLevel1(mock(Platform.class), props);
    level1.configure();

    PlatformLevel2 underTest = new PlatformLevel2(level1);
    underTest.configure();

    assertThat(underTest.getOptional(ClusterConfigurationCheck.class)).isNotPresent();

  }
}

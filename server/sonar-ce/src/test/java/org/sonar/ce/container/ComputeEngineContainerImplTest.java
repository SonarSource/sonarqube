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
package org.sonar.ce.container;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.picocontainer.MutablePicoContainer;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.DbTester;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;

public class ComputeEngineContainerImplTest {
  private static final int COMPONENTS_IN_CONTAINER_AT_CONSTRUCTION = 2;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ComputeEngineContainerImpl underTest = new ComputeEngineContainerImpl();

  @Test
  public void constructor_adds_only_container_and_PropertyDefinitions() {
    ComponentContainer componentContainer = underTest.getComponentContainer();

    assertThat(componentContainer.getComponentsByType(Object.class)).hasSize(2);
    assertThat(componentContainer.getComponentByType(PropertyDefinitions.class)).isNotNull();
    assertThat(componentContainer.getComponentByType(ComponentContainer.class)).isSameAs(componentContainer);
  }

  @Test
  public void configure_adds_raw_properties_from_Props_to_container() {
    Properties properties = new Properties();
    underTest.configure(new Props(properties));

    assertThat(underTest.getComponentContainer().getComponentByType(Properties.class)).isSameAs(properties);
  }

  @Test
  public void verify_number_of_components_in_container() {
    Properties properties = new Properties();
    underTest.configure(new Props(properties));

    assertThat(underTest.getComponentContainer().getPicoContainer().getComponentAdapters())
      .hasSize(COMPONENTS_IN_CONTAINER_AT_CONSTRUCTION
        + 22 // level 1
        + 47 // content of DaoModule
        + 1 // content of EsSearchModule
        + 58 // content of CorePropertyDefinitions
        + 1 // content of CePropertyDefinitions
        + 59 // content of MigrationStepModule
        + 10 // level 2
        + 5 // level 3
        + 76 // level 4
        + 6 // content of CeModule
        + 7 // content of CeQueueModule
        + 4 // content of ReportProcessingModule
        + 4 // content of CeTaskProcessorModule
        + 3 // level startup
      );
  }

  @Test
  public void real_start() throws IOException {
    Properties properties = new Properties();
    File homeDir = tempFolder.newFolder();
    File dataDir = new File(homeDir, "data");
    File tmpDir = new File(homeDir, "tmp");
    properties.setProperty(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
    properties.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    properties.setProperty(ProcessProperties.PATH_TEMP, tmpDir.getAbsolutePath());
    String url = ((BasicDataSource) dbTester.database().getDataSource()).getUrl();
    properties.setProperty(DatabaseProperties.PROP_URL, url);
    properties.setProperty(DatabaseProperties.PROP_USER, "sonar");
    properties.setProperty(DatabaseProperties.PROP_PASSWORD, "sonar");

    underTest
      .configure(new Props(properties))
      .start();

  }

  @Test
  public void start_starts_pico_container() {
    MutablePicoContainer picoContainer = underTest.getComponentContainer().getPicoContainer();

    assertThat(picoContainer.getLifecycleState().isStarted()).isFalse();

    underTest.start();

    assertThat(picoContainer.getLifecycleState().isStarted()).isTrue();
  }

  @Test
  public void stop_stops_and_dispose_pico_container() {
    MutablePicoContainer picoContainer = underTest.getComponentContainer().getPicoContainer();

    assertThat(picoContainer.getLifecycleState().isStarted()).isFalse();
    assertThat(picoContainer.getLifecycleState().isStopped()).isFalse();

    underTest.start();
    underTest.stop();

    assertThat(picoContainer.getLifecycleState().isStarted()).isFalse();
    assertThat(picoContainer.getLifecycleState().isStopped()).isFalse();
    assertThat(picoContainer.getLifecycleState().isDisposed()).isTrue();
  }
}

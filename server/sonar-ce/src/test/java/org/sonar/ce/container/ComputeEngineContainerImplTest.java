/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.ce.container;

import com.hazelcast.core.HazelcastInstance;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.MutablePicoContainer;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.ce.cluster.HazelcastClientWrapperImpl;
import org.sonar.ce.cluster.HazelcastTestHelper;
import org.sonar.ce.CeDistributedInformationImpl;
import org.sonar.ce.StandaloneCeDistributedInformation;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;
import static org.sonar.process.ProcessProperties.PATH_DATA;
import static org.sonar.process.ProcessProperties.PATH_HOME;
import static org.sonar.process.ProcessProperties.PATH_TEMP;

public class ComputeEngineContainerImplTest {
  private static final int CONTAINER_ITSELF = 1;
  private static final int COMPONENTS_IN_LEVEL_1_AT_CONSTRUCTION = CONTAINER_ITSELF + 1;
  private static final String CLUSTER_NAME = "test";

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ComputeEngineContainerImpl underTest = new ComputeEngineContainerImpl();

  @Test
  public void constructor_does_not_create_container() {
    assertThat(underTest.getComponentContainer()).isNull();
  }

  @Test
  public void real_start_with_cluster() throws IOException {
    int port = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
    HazelcastInstance hzInstance = HazelcastTestHelper.createHazelcastCluster(CLUSTER_NAME, port);

    Properties properties = getProperties();
    properties.setProperty(ProcessProperties.CLUSTER_ENABLED, "true");
    properties.setProperty(ProcessProperties.CLUSTER_LOCALENDPOINT, String.format("%s:%d", hzInstance.getCluster().getLocalMember().getAddress().getHost(), port));
    properties.setProperty(ProcessProperties.CLUSTER_NAME, CLUSTER_NAME);

    // required persisted properties
    insertProperty(CoreProperties.SERVER_ID, "a_startup_id");
    insertProperty(CoreProperties.SERVER_STARTTIME, DateUtils.formatDateTime(new Date()));

    underTest
      .start(new Props(properties));

    MutablePicoContainer picoContainer = underTest.getComponentContainer().getPicoContainer();
    assertThat(
      picoContainer.getComponentAdapters().stream()
        .map(ComponentAdapter::getComponentImplementation)
        .collect(Collectors.toList())
    ).contains((Class) HazelcastClientWrapperImpl.class,
      (Class) CeDistributedInformationImpl.class
    );
    underTest.stop();
  }

  @Test
  public void real_start_without_cluster() throws IOException {
    Properties properties = getProperties();

    // required persisted properties
    insertProperty(CoreProperties.SERVER_ID, "a_startup_id");
    insertProperty(CoreProperties.SERVER_STARTTIME, DateUtils.formatDateTime(new Date()));

    underTest
      .start(new Props(properties));

    MutablePicoContainer picoContainer = underTest.getComponentContainer().getPicoContainer();
    assertThat(picoContainer.getComponentAdapters())
      .hasSize(
        CONTAINER_ITSELF
          + 74 // level 4
          + 4 // content of CeConfigurationModule
          + 4 // content of CeQueueModule
          + 3 // content of CeHttpModule
          + 3 // content of CeTaskCommonsModule
          + 4 // content of ProjectAnalysisTaskModule
          + 4 // content of CeTaskProcessorModule
          + 3 // CeCleaningModule + its content
          + 1 // CeDistributedInformation
    );
    assertThat(picoContainer.getParent().getComponentAdapters()).hasSize(
      CONTAINER_ITSELF
        + 5 // level 3
    );
    assertThat(picoContainer.getParent().getParent().getComponentAdapters()).hasSize(
      CONTAINER_ITSELF
        + 10 // MigrationConfigurationModule
        + 17 // level 2
    );
    assertThat(picoContainer.getParent().getParent().getParent().getComponentAdapters()).hasSize(
      COMPONENTS_IN_LEVEL_1_AT_CONSTRUCTION
        + 23 // level 1
        + 46 // content of DaoModule
        + 3 // content of EsSearchModule
        + 57 // content of CorePropertyDefinitions
    );
    assertThat(
      picoContainer.getComponentAdapters().stream()
        .map(ComponentAdapter::getComponentImplementation)
        .collect(Collectors.toList())
    ).doesNotContain((Class) HazelcastClientWrapperImpl.class,
      (Class) CeDistributedInformationImpl.class
    ).contains(
      (Class) StandaloneCeDistributedInformation.class
    );
    assertThat(picoContainer.getParent().getParent().getParent().getParent()).isNull();
    underTest.stop();

    assertThat(picoContainer.getLifecycleState().isStarted()).isFalse();
    assertThat(picoContainer.getLifecycleState().isStopped()).isFalse();
    assertThat(picoContainer.getLifecycleState().isDisposed()).isTrue();
  }

  private Properties getProperties() throws IOException {
    Properties properties = ProcessProperties.defaults();
    File homeDir = tempFolder.newFolder();
    File dataDir = new File(homeDir, "data");
    File tmpDir = new File(homeDir, "tmp");
    properties.setProperty(PATH_HOME, homeDir.getAbsolutePath());
    properties.setProperty(PATH_DATA, dataDir.getAbsolutePath());
    properties.setProperty(PATH_TEMP, tmpDir.getAbsolutePath());
    properties.setProperty(PROPERTY_PROCESS_INDEX, valueOf(ProcessId.COMPUTE_ENGINE.getIpcIndex()));
    properties.setProperty(PROPERTY_SHARED_PATH, tmpDir.getAbsolutePath());
    properties.setProperty(DatabaseProperties.PROP_URL, ((BasicDataSource) dbTester.database().getDataSource()).getUrl());
    properties.setProperty(DatabaseProperties.PROP_USER, "sonar");
    properties.setProperty(DatabaseProperties.PROP_PASSWORD, "sonar");
    return properties;
  }

  private void insertProperty(String key, String value) {
    PropertyDto dto = new PropertyDto().setKey(key).setValue(value);
    dbTester.getDbClient().propertiesDao().saveProperty(dbTester.getSession(), dto);
    dbTester.commit();
  }
}

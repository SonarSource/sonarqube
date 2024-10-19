/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.application.es;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ES_HTTP_KEYSTORE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_PASSWORD;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

public class EsInstallationTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void constructor_fails_with_IAE_if_sq_home_property_is_not_defined() {
    Props props = new Props(new Properties());

    assertThatThrownBy(() ->  new EsInstallation(props))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Property sonar.path.home is not set");
  }

  @Test
  public void constructor_fails_with_IAE_if_temp_dir_property_is_not_defined() throws IOException {
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), temp.newFolder().getAbsolutePath());

    assertThatThrownBy(() -> new EsInstallation(props))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Property sonar.path.temp is not set");
  }

  @Test
  public void constructor_fails_with_IAE_if_data_dir_property_is_not_defined() throws IOException {
    Props props = new Props(new Properties());
    props.set(PATH_HOME.getKey(), temp.newFolder().getAbsolutePath());

    assertThatThrownBy(() -> new EsInstallation(props))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing property: sonar.path.data");
  }

  @Test
  public void getHomeDirectory_is_elasticsearch_subdirectory_of_sq_home_directory() throws IOException {
    File sqHomeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), sqHomeDir.getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getHomeDirectory()).isEqualTo(new File(sqHomeDir, "elasticsearch"));
  }

  @Test
  public void override_data_dir() throws Exception {
    File sqHomeDir = temp.newFolder();
    File tempDir = temp.newFolder();
    File dataDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_HOME.getKey(), sqHomeDir.getAbsolutePath());
    props.set(PATH_TEMP.getKey(), tempDir.getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());

    props.set(PATH_DATA.getKey(), dataDir.getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getDataDirectory()).isEqualTo(new File(dataDir, "es8"));
  }

  @Test
  public void getLogDirectory_is_configured_with_non_nullable_PATH_LOG_variable() throws IOException {
    File sqHomeDir = temp.newFolder();
    File logDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), sqHomeDir.getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), logDir.getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getLogDirectory()).isEqualTo(logDir);
  }

  @Test
  public void getOutdatedSearchDirectories_returns_all_previously_used_es_data_directory_names() throws IOException {
    File sqHomeDir = temp.newFolder();
    File logDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), sqHomeDir.getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), logDir.getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getOutdatedSearchDirectories())
      .extracting(File::getName)
      .containsExactlyInAnyOrder("es", "es5", "es6", "es7");
  }

  @Test
  public void conf_directory_is_conf_es_subdirectory_of_sq_temp_directory() throws IOException {
    File tempDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), tempDir.getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getConfDirectory()).isEqualTo(new File(tempDir, "conf/es"));
  }

  @Test
  public void getExecutable_resolve_executable_for_platform() throws IOException {
    File sqHomeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), sqHomeDir.getAbsolutePath());
    props.set(PATH_TEMP.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getExecutable()).isEqualTo(new File(sqHomeDir, "elasticsearch/bin/elasticsearch"));
  }

  @Test
  public void getLog4j2Properties_is_in_es_conf_directory() throws IOException {
    File tempDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), tempDir.getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getLog4j2PropertiesLocation()).isEqualTo(new File(tempDir, "conf/es/log4j2.properties"));
  }

  @Test
  public void getElasticsearchYml_is_in_es_conf_directory() throws IOException {
    File tempDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), tempDir.getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getElasticsearchYml()).isEqualTo(new File(tempDir, "conf/es/elasticsearch.yml"));
  }

  @Test
  public void getJvmOptions_is_in_es_conf_directory() throws IOException {
    File tempDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), tempDir.getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);

    assertThat(underTest.getJvmOptions()).isEqualTo(new File(tempDir, "conf/es/jvm.options"));
  }

  @Test
  public void isHttpEncryptionEnabled_shouldReturnCorrectValue() throws IOException {
    File sqHomeDir = temp.newFolder();
    Props props = new Props(new Properties());
    props.set(PATH_DATA.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_HOME.getKey(), temp.newFolder().getAbsolutePath());
    props.set(PATH_TEMP.getKey(), sqHomeDir.getAbsolutePath());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());
    props.set(CLUSTER_ENABLED.getKey(), "true");
    props.set(CLUSTER_SEARCH_PASSWORD.getKey(), "password");
    props.set(CLUSTER_ES_HTTP_KEYSTORE.getKey(), sqHomeDir.getAbsolutePath());

    EsInstallation underTest = new EsInstallation(props);
    assertThat(underTest.isHttpEncryptionEnabled()).isTrue();

    props.set(CLUSTER_ENABLED.getKey(), "false");
    props.set(CLUSTER_ES_HTTP_KEYSTORE.getKey(), sqHomeDir.getAbsolutePath());

    underTest = new EsInstallation(props);
    assertThat(underTest.isHttpEncryptionEnabled()).isFalse();

    props.set(CLUSTER_ENABLED.getKey(), "true");
    props.rawProperties().remove(CLUSTER_ES_HTTP_KEYSTORE.getKey());

    underTest = new EsInstallation(props);
    assertThat(underTest.isHttpEncryptionEnabled()).isFalse();
  }
}

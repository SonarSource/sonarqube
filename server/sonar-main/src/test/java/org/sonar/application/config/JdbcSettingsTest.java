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
package org.sonar.application.config;

import java.io.File;
import java.net.InetAddress;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.MessageException;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.application.config.JdbcSettings.Provider;
import static org.sonar.process.ProcessProperties.Property.JDBC_ADDITIONAL_LIB_PATHS;
import static org.sonar.process.ProcessProperties.Property.JDBC_DRIVER_PATH;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;

public class JdbcSettingsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final JdbcSettings underTest = new JdbcSettings();
  private File homeDir;

  @Before
  public void setUp() throws Exception {
    homeDir = temp.newFolder();
  }

  @Test
  public void resolve_H2_provider_when_props_is_empty_and_set_URL_to_default_H2() {
    Props props = newProps();

    assertThat(underTest.resolveProviderAndEnforceNonnullJdbcUrl(props))
      .isEqualTo(Provider.H2);
    assertThat(props.nonNullValue(JDBC_URL.getKey())).isEqualTo(String.format("jdbc:h2:tcp://%s:9092/sonar;NON_KEYWORDS=VALUE", InetAddress.getLoopbackAddress().getHostAddress()));
  }

  @Test
  public void resolve_Oracle_when_jdbc_url_contains_oracle_in_any_case() {
    checkProviderForUrlAndUnchangedUrl("jdbc:oracle:foo", Provider.ORACLE);
    checkProviderForUrlAndUnchangedUrl("jdbc:OrAcLe:foo", Provider.ORACLE);
  }

  @Test
  public void resolve_SqlServer_when_jdbc_url_contains_sqlserver_in_any_case() {
    checkProviderForUrlAndUnchangedUrl("jdbc:sqlserver:foo", Provider.SQLSERVER);

    checkProviderForUrlAndUnchangedUrl("jdbc:SQLSeRVeR:foo", Provider.SQLSERVER);
  }

  @Test
  public void resolve_POSTGRESQL_when_jdbc_url_contains_POSTGRESQL_in_any_case() {
    checkProviderForUrlAndUnchangedUrl("jdbc:postgresql:foo", Provider.POSTGRESQL);

    checkProviderForUrlAndUnchangedUrl("jdbc:POSTGRESQL:foo", Provider.POSTGRESQL);
  }

  private void checkProviderForUrlAndUnchangedUrl(String url, Provider expected) {
    Props props = newProps(JDBC_URL.getKey(), url);

    assertThat(underTest.resolveProviderAndEnforceNonnullJdbcUrl(props)).isEqualTo(expected);
    assertThat(props.nonNullValue(JDBC_URL.getKey())).isEqualTo(url);
  }

  @Test
  public void fail_with_MessageException_when_provider_is_not_supported() {
    Props props = newProps(JDBC_URL.getKey(), "jdbc:microsoft:sqlserver://localhost");

    assertThatThrownBy(() -> underTest.resolveProviderAndEnforceNonnullJdbcUrl(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Unsupported JDBC driver provider: microsoft");
  }

  @Test
  public void fail_with_MessageException_when_url_does_not_have_jdbc_prefix() {
    Props props = newProps(JDBC_URL.getKey(), "oracle:thin:@localhost/XE");

    assertThatThrownBy(() -> underTest.resolveProviderAndEnforceNonnullJdbcUrl(props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Bad format of JDBC URL: oracle:thin:@localhost/XE");
  }

  @Test
  public void checkAndComplete_sets_driver_path_for_oracle() throws Exception {
    File driverFile = new File(homeDir, "extensions/jdbc-driver/oracle/ojdbc6.jar");
    FileUtils.touch(driverFile);

    Props props = newProps(JDBC_URL.getKey(), "jdbc:oracle:thin:@localhost/XE");
    underTest.accept(props);
    assertThat(props.nonNullValueAsFile(JDBC_DRIVER_PATH.getKey())).isEqualTo(driverFile);
  }

  @Test
  public void sets_driver_path_for_h2() throws Exception {
    File driverFile = new File(homeDir, "lib/jdbc/h2/h2.jar");
    FileUtils.touch(driverFile);

    Props props = newProps(JDBC_URL.getKey(), "jdbc:h2:tcp://localhost:9092/sonar");
    underTest.accept(props);
    assertThat(props.nonNullValueAsFile(JDBC_DRIVER_PATH.getKey())).isEqualTo(driverFile);
  }

  @Test
  public void checkAndComplete_sets_driver_path_for_postgresql() throws Exception {
    File driverFile = new File(homeDir, "lib/jdbc/postgresql/pg.jar");
    FileUtils.touch(driverFile);

    Props props = newProps(JDBC_URL.getKey(), "jdbc:postgresql://localhost/sonar");
    underTest.accept(props);
    assertThat(props.nonNullValueAsFile(JDBC_DRIVER_PATH.getKey())).isEqualTo(driverFile);
  }

  @Test
  public void checkAndComplete_sets_driver_path_for_mssql() throws Exception {
    File driverFile = new File(homeDir, "lib/jdbc/mssql/mssql-jdbc.jar");
    File additionalDriveFile = new File(homeDir, "lib/jdbc/mssql/entraid.jar");
    FileUtils.touch(driverFile);
    FileUtils.touch(additionalDriveFile);

    Props props = newProps(JDBC_URL.getKey(), "jdbc:sqlserver://localhost/sonar;SelectMethod=Cursor");
    underTest.accept(props);
    assertThat(props.nonNullValueAsFile(JDBC_DRIVER_PATH.getKey())).isEqualTo(driverFile);
    assertThat(props.nonNullValueAsFile(JDBC_ADDITIONAL_LIB_PATHS.getKey())).isEqualTo(additionalDriveFile);
  }

  @Test
  public void driver_file() throws Exception {
    File driverFile = new File(homeDir, "extensions/jdbc-driver/oracle/ojdbc6.jar");
    FileUtils.touch(driverFile);

    String path = underTest.driverPath(homeDir, Provider.ORACLE);
    assertThat(path).isEqualTo(driverFile.getAbsolutePath());
  }

  @Test
  public void driver_dir_does_not_exist() {
    assertThatThrownBy(() -> underTest.driverPath(homeDir, Provider.ORACLE))
      .isInstanceOf(MessageException.class)
      .hasMessage("Directory does not exist: extensions/jdbc-driver/oracle");
  }

  @Test
  public void no_files_in_driver_dir() throws Exception {
    FileUtils.forceMkdir(new File(homeDir, "extensions/jdbc-driver/oracle"));

    assertThatThrownBy(() -> underTest.driverPath(homeDir, Provider.ORACLE))
      .isInstanceOf(MessageException.class)
      .hasMessage("Directory does not contain JDBC driver: extensions/jdbc-driver/oracle");
  }

  @Test
  public void too_many_files_in_driver_dir() throws Exception {
    FileUtils.touch(new File(homeDir, "extensions/jdbc-driver/oracle/ojdbc5.jar"));
    FileUtils.touch(new File(homeDir, "extensions/jdbc-driver/oracle/ojdbc6.jar"));

    assertThatThrownBy(() -> underTest.driverPath(homeDir, Provider.ORACLE))
      .isInstanceOf(MessageException.class)
      .hasMessage("Directory must contain only one JAR file: extensions/jdbc-driver/oracle");
  }

  private Props newProps(String... params) {
    Properties properties = new Properties();
    for (int i = 0; i < params.length; i++) {
      properties.setProperty(params[i], params[i + 1]);
      i++;
    }
    properties.setProperty(PATH_HOME.getKey(), homeDir.getAbsolutePath());
    return new Props(properties);
  }
}

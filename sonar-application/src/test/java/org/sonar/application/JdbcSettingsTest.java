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
package org.sonar.application;

import java.io.File;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.application.JdbcSettings.Provider;
import static org.sonar.process.ProcessProperties.JDBC_URL;

public class JdbcSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  JdbcSettings settings = new JdbcSettings();

  @Test
  public void resolve_H2_provider_when_props_is_empty_and_set_URL_to_default_H2() {
    Props props = newProps();

    assertThat(settings.resolveProviderAndEnforceNonnullJdbcUrl(props))
      .isEqualTo(Provider.H2);
    assertThat(props.nonNullValue(JDBC_URL)).isEqualTo("jdbc:h2:tcp://localhost:9092/sonar");
  }

  @Test
  public void resolve_Oracle_when_jdbc_url_contains_oracle_in_any_case() {
    checkProviderForUrlAndUnchangedUrl("jdbc:oracle:foo", Provider.ORACLE);
    checkProviderForUrlAndUnchangedUrl("jdbc:OrAcLe:foo", Provider.ORACLE);
  }

  @Test
  public void resolve_MySql_when_jdbc_url_contains_mysql_in_any_case() {
    checkProviderForUrlAndUnchangedUrl("jdbc:mysql:foo", Provider.MYSQL);

    checkProviderForUrlAndUnchangedUrl("jdbc:mYsQL:foo", Provider.MYSQL);
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
    Props props = newProps(JDBC_URL, url);

    assertThat(settings.resolveProviderAndEnforceNonnullJdbcUrl(props)).isEqualTo(expected);
    assertThat(props.nonNullValue(JDBC_URL)).isEqualTo(url);
  }

  @Test
  public void fail_with_MessageException_when_provider_is_not_supported() {
    Props props = newProps(JDBC_URL, "jdbc:microsoft:sqlserver://localhost");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Unsupported JDBC driver provider: microsoft");

    settings.resolveProviderAndEnforceNonnullJdbcUrl(props);
  }

  @Test
  public void fail_with_MessageException_when_url_does_not_have_jdbc_prefix() {
    Props props = newProps(JDBC_URL, "oracle:thin:@localhost/XE");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Bad format of JDBC URL: oracle:thin:@localhost/XE");

    settings.resolveProviderAndEnforceNonnullJdbcUrl(props);
  }

  @Test
  public void check_mysql_parameters() {
    // minimal -> ok
    settings.checkUrlParameters(Provider.MYSQL,
      "jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8");

    // full -> ok
    settings.checkUrlParameters(Provider.MYSQL,
      "jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance");

    // missing required -> ko
    expectedException.expect(MessageException.class);
    expectedException.expectMessage("JDBC URL must have the property 'useUnicode=true'");

    settings.checkUrlParameters(Provider.MYSQL, "jdbc:mysql://localhost:3306/sonar?characterEncoding=utf8");
  }

  @Test
  public void checkAndComplete_sets_driver_path_for_oracle() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "extensions/jdbc-driver/oracle/ojdbc6.jar");
    FileUtils.touch(driverFile);

    Props props = newProps(JDBC_URL, "jdbc:oracle:thin:@localhost/XE");
    settings.checkAndComplete(home, props);
    assertThat(props.nonNullValueAsFile(ProcessProperties.JDBC_DRIVER_PATH)).isEqualTo(driverFile);
  }

  @Test
  public void checkAndComplete_sets_driver_path_for_h2() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "lib/jdbc/h2/h2.jar");
    FileUtils.touch(driverFile);

    Props props = newProps(JDBC_URL, "jdbc:h2:tcp://localhost:9092/sonar");
    settings.checkAndComplete(home, props);
    assertThat(props.nonNullValueAsFile(ProcessProperties.JDBC_DRIVER_PATH)).isEqualTo(driverFile);
  }

  @Test
  public void checkAndComplete_sets_driver_path_for_postgresql() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "lib/jdbc/postgresql/pg.jar");
    FileUtils.touch(driverFile);

    Props props = newProps(JDBC_URL, "jdbc:postgresql://localhost/sonar");
    settings.checkAndComplete(home, props);
    assertThat(props.nonNullValueAsFile(ProcessProperties.JDBC_DRIVER_PATH)).isEqualTo(driverFile);
  }

  @Test
  public void checkAndComplete_sets_driver_path_for_mssql() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "lib/jdbc/mssql/sqljdbc4.jar");
    FileUtils.touch(driverFile);

    Props props = newProps(JDBC_URL, "jdbc:sqlserver://localhost/sonar;SelectMethod=Cursor");
    settings.checkAndComplete(home, props);
    assertThat(props.nonNullValueAsFile(ProcessProperties.JDBC_DRIVER_PATH)).isEqualTo(driverFile);
  }

  @Test
  public void driver_file() throws Exception {
    File home = temp.newFolder();
    File driverFile = new File(home, "extensions/jdbc-driver/oracle/ojdbc6.jar");
    FileUtils.touch(driverFile);

    String path = settings.driverPath(home, Provider.ORACLE);
    assertThat(path).isEqualTo(driverFile.getAbsolutePath());
  }

  @Test
  public void driver_dir_does_not_exist() throws Exception {
    File home = temp.newFolder();

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Directory does not exist: extensions/jdbc-driver/oracle");

    settings.driverPath(home, Provider.ORACLE);
  }

  @Test
  public void no_files_in_driver_dir() throws Exception {
    File home = temp.newFolder();
    FileUtils.forceMkdir(new File(home, "extensions/jdbc-driver/oracle"));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Directory does not contain JDBC driver: extensions/jdbc-driver/oracle");

    settings.driverPath(home, Provider.ORACLE);
  }

  @Test
  public void too_many_files_in_driver_dir() throws Exception {
    File home = temp.newFolder();
    FileUtils.touch(new File(home, "extensions/jdbc-driver/oracle/ojdbc5.jar"));
    FileUtils.touch(new File(home, "extensions/jdbc-driver/oracle/ojdbc6.jar"));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Directory must contain only one JAR file: extensions/jdbc-driver/oracle");

    settings.driverPath(home, Provider.ORACLE);
  }

  private Props newProps(String... params) {
    Properties properties = new Properties();
    for (int i = 0; i < params.length; i++) {
      properties.setProperty(params[i], params[i + 1]);
      i++;
    }
    return new Props(properties);
  }
}

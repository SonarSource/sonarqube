/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.serverid;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbcUrlSanitizerTest {
  private JdbcUrlSanitizer underTest = new JdbcUrlSanitizer();

  @Test
  public void sanitize_h2_url() {
    verifyJdbcUrl("jdbc:h2:tcp://dbserv:8084/~/sample", "jdbc:h2:tcp://dbserv:8084/~/sample");
    verifyJdbcUrl("jdbc:h2:tcp://localhost/mem:test", "jdbc:h2:tcp://localhost/mem:test");
    verifyJdbcUrl("jdbc:h2:tcp://localhost/mem:TEST", "jdbc:h2:tcp://localhost/mem:test");
  }

  @Test
  public void sanitize_mysql_url() {
    verifyJdbcUrl("jdbc:mysql://127.0.0.1:3306/sonarqube?useUnicode=true&characterEncoding=utf8", "jdbc:mysql://127.0.0.1:3306/sonarqube");
    verifyJdbcUrl("jdbc:mysql://127.0.0.1:3306/sonarqube", "jdbc:mysql://127.0.0.1:3306/sonarqube");
    verifyJdbcUrl("jdbc:mysql://127.0.0.1:3306/SONARQUBE", "jdbc:mysql://127.0.0.1:3306/sonarqube");
  }

  @Test
  public void sanitize_oracle_url() {
    verifyJdbcUrl("sonar.jdbc.url=jdbc:oracle:thin:@localhost:1521/XE", "sonar.jdbc.url=jdbc:oracle:thin:@localhost:1521/xe");
    verifyJdbcUrl("sonar.jdbc.url=jdbc:oracle:thin:@localhost/XE", "sonar.jdbc.url=jdbc:oracle:thin:@localhost/xe");
    verifyJdbcUrl("sonar.jdbc.url=jdbc:oracle:thin:@localhost/XE?foo", "sonar.jdbc.url=jdbc:oracle:thin:@localhost/xe");
    verifyJdbcUrl("sonar.jdbc.url=jdbc:oracle:thin:@LOCALHOST/XE?foo", "sonar.jdbc.url=jdbc:oracle:thin:@localhost/xe");
  }

  @Test
  public void sanitize_sqlserver_url() {
    // see examples listed at https://docs.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url
    verifyJdbcUrl("jdbc:sqlserver://localhost;user=MyUserName;password=*****;", "jdbc:sqlserver://localhost");
    verifyJdbcUrl("jdbc:sqlserver://;servername=server_name;integratedSecurity=true;authenticationScheme=JavaKerberos", "jdbc:sqlserver://server_name");
    verifyJdbcUrl("jdbc:sqlserver://localhost;integratedSecurity=true;", "jdbc:sqlserver://localhost");
    verifyJdbcUrl("jdbc:sqlserver://localhost;databaseName=AdventureWorks;integratedSecurity=true;", "jdbc:sqlserver://localhost/adventureworks");
    verifyJdbcUrl("jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;integratedSecurity=true;", "jdbc:sqlserver://localhost:1433/adventureworks");
    verifyJdbcUrl("jdbc:sqlserver://localhost;databaseName=AdventureWorks;integratedSecurity=true;applicationName=MyApp;", "jdbc:sqlserver://localhost/adventureworks");
    verifyJdbcUrl("jdbc:sqlserver://localhost;instanceName=instance1;integratedSecurity=true;", "jdbc:sqlserver://localhost");
    verifyJdbcUrl("jdbc:sqlserver://;serverName=3ffe:8311:eeee:f70f:0:5eae:10.203.31.9\\\\instance1;integratedSecurity=true;", "jdbc:sqlserver://3ffe:8311:eeee:f70f:0:5eae:10.203.31.9\\\\instance1");

    // test parameter aliases
    verifyJdbcUrl("jdbc:sqlserver://;server=server_name", "jdbc:sqlserver://server_name");
    verifyJdbcUrl("jdbc:sqlserver://;server=server_name;portNumber=1234", "jdbc:sqlserver://server_name:1234");
    verifyJdbcUrl("jdbc:sqlserver://;server=server_name;port=1234", "jdbc:sqlserver://server_name:1234");

    // case-insensitive
    verifyJdbcUrl("jdbc:sqlserver://LOCALHOST;user=MyUserName;password=*****;", "jdbc:sqlserver://localhost");

  }

  @Test
  public void sanitize_postgres_url() {
    verifyJdbcUrl("jdbc:postgresql://localhost/sonar", "jdbc:postgresql://localhost/sonar");
    verifyJdbcUrl("jdbc:postgresql://localhost:1234/sonar", "jdbc:postgresql://localhost:1234/sonar");
    verifyJdbcUrl("jdbc:postgresql://localhost:1234/sonar?foo", "jdbc:postgresql://localhost:1234/sonar");

    // case-insensitive
    verifyJdbcUrl("jdbc:postgresql://localhost:1234/SONAR?foo", "jdbc:postgresql://localhost:1234/sonar");
  }

  private void verifyJdbcUrl(String url, String expectedResult) {
    assertThat(underTest.sanitize(url)).isEqualTo(expectedResult);
  }


}

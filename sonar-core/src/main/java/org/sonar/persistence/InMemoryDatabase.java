/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.persistence;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.io.Resources;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

public class InMemoryDatabase implements Database {

  private BasicDataSource datasource;

  public void start() {
    startDatabase();
    executeDdl();
  }

  private void startDatabase() {
    try {
      Properties properties = new Properties();
      properties.put("driverClassName", "org.apache.derby.jdbc.EmbeddedDriver");
      properties.put("username", "sonar");
      properties.put("password", "sonar");
      properties.put("url", "jdbc:derby:memory:sonar;create=true;user=sonar;password=sonar");
      datasource = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to start Derby", e);
    }
  }

  private void executeDdl() {
    try {
      Connection connection = datasource.getConnection();
      List<String> lines = IOUtils.readLines(getClass().getResourceAsStream("/org/sonar/persistence/master_derby.ddl"));
      for (String line : lines) {
        if (StringUtils.isNotBlank(line) && !StringUtils.startsWith(line, "--")) {
          Statement statement = connection.createStatement();
          statement.execute(line);
          statement.close();
        }
      }
      connection.commit();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stop() {
    try {
      if (datasource != null) {
        datasource.close();
      }
      DriverManager.getConnection("jdbc:derby:memory:sonar;shutdown=true;user=sonar;password=sonar");

    } catch (SQLException e) {
      throw new IllegalStateException("Fail to stop Derby", e);
    }
  }

  public DataSource getDataSource() {
    return datasource;
  }
}

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
package org.sonar.batch.bootstrap;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

/**
 * Companion of {@link JdbcDriverHolder} and allows it to deregister JDBC drivers.
 * <p>
 * Some hacks are involved in the loading of the class - see {@link JdbcDriverHolder#stop()},
 * so this class can refer to classes only from java.* package and must not be referred from other classes.
 * Placement and naming of this class and methods are very important, since it loaded and invoked via reflection.
 * </p>
 */
public class JdbcLeakPrevention {

  /**
   * @return list of names of deregistered drivers
   */
  public List<String> clearJdbcDriverRegistrations() throws SQLException {
    List<String> driverNames = new ArrayList<String>();
    HashSet<Driver> originalDrivers = new HashSet<Driver>();
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      originalDrivers.add(drivers.nextElement());
    }
    drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      if (driver.getClass().getClassLoader() != this.getClass().getClassLoader()) {
        continue;
      }
      if (originalDrivers.contains(driver)) {
        driverNames.add(driver.getClass().getCanonicalName());
      }
      DriverManager.deregisterDriver(driver);
    }
    return driverNames;
  }

}

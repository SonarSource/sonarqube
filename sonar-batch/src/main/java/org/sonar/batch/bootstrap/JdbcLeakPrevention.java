/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
   * @return names of the drivers that have been unregistered
   */
  public List<String> unregisterDrivers() throws SQLException {
    Set<Driver> registeredDrivers = registeredDrivers();

    List<String> unregisteredNames = new ArrayList<String>();
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      if (driver.getClass().getClassLoader() != this.getClass().getClassLoader()) {
        continue;
      }
      if (registeredDrivers.contains(driver)) {
        unregisteredNames.add(driver.getClass().getCanonicalName());
      }
      DriverManager.deregisterDriver(driver);
    }
    return unregisteredNames;
  }

  private Set<Driver> registeredDrivers() {
    Set<Driver> registeredDrivers = new HashSet<Driver>();
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      registeredDrivers.add(driver);
    }
    return registeredDrivers;
  }

}

/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.database;

import org.apache.commons.dbcp.BasicDataSourceFactory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.sql.DataSource;

public class UniqueDatasourceFactory extends BasicDataSourceFactory {

  /**
   * getObjectInstance can be called multiple times (at each jndi.lookup calls), BasicDataSourceFactory getObjectInstance
   * method impl return each times a new datasource, so cache it and make sure that this factory
   * always return the same datasource object instance
   */
  private static DataSource ds = null;
  
  @Override
  public Object getObjectInstance(Object arg0, Name arg1, Context arg2, Hashtable arg3) throws Exception {
    synchronized (UniqueDatasourceFactory.class) {
      if (ds == null) {
        ds = (DataSource)super.getObjectInstance(arg0, arg1, arg2, arg3);
      }
    }
    return ds;
  }
  
}

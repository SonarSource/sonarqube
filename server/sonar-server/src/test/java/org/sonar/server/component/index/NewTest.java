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
package org.sonar.server.component.index;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.EsTester;

public class NewTest {

  static {
    System.setProperty("log4j.shutdownHookEnabled", "false");
    // we can not shutdown logging when tests are running or the next test that runs within the
    // same JVM will try to initialize logging after a security manager has been installed and
    // this will fail
    System.setProperty("es.log4j.shutdownEnabled", "false");
    System.setProperty("log4j2.disable.jmx", "true");
    System.setProperty("log4j.skipJansi", "true"); // jython has this crazy shaded Jansi version that log4j2 tries to load
  }

  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings().asConfig()));

//  @AfterClass
//  public static void afterClass() throws Exception {
//    try {
//      es.after();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    es = null;
//  }

  @Test
  public void name() throws Exception {
    IndicesExistsResponse x = es.client().prepareIndicesExist("components").get();
    System.out.println(x.isExists());
    IndicesExistsResponse x2 = es.client().prepareIndicesExist("components").get();
    System.out.println(x2.isExists());
  }
}

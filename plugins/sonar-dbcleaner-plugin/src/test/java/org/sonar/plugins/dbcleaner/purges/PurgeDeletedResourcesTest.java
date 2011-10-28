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
package org.sonar.plugins.dbcleaner.purges;

import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.sql.Connection;
import java.sql.SQLException;

public class PurgeDeletedResourcesTest extends AbstractDbUnitTestCase {

  @Test
  public void purgeDeletedResources() throws SQLException {
    setupData("sharedFixture", "purgeDeletedResources");

    final Connection c = getConnection().getConnection();
    // TODO Godin: next line was here with HSQL
    //c.prepareStatement("SET REFERENTIAL_INTEGRITY FALSE; ").execute();
    c.prepareStatement("delete from projects where id=3").executeUpdate();
    c.commit();

    final PurgeDeletedResources purge = new PurgeDeletedResources(getSession());
    purge.purge(null);

    checkTables("purgeDeletedResources", "snapshots", "project_measures", "measure_data", "rule_failures", "snapshot_sources");
  }
}

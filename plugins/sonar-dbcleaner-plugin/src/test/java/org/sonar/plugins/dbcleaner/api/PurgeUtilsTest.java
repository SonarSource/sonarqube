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
package org.sonar.plugins.dbcleaner.api;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.plugins.dbcleaner.api.PurgeUtils;

import java.sql.SQLException;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PurgeUtilsTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldReturnDefaultMinimumPeriod() {
    assertThat(PurgeUtils.getMinimumPeriodInHours(new PropertiesConfiguration()), is(PurgeUtils.DEFAULT_MINIMUM_PERIOD_IN_HOURS));
  }

  @Test
  public void shouldReturnMinimumPeriod() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(PurgeUtils.PROP_KEY_MINIMUM_PERIOD_IN_HOURS, "9");
    assertThat(PurgeUtils.getMinimumPeriodInHours(conf), is(9));
  }

  @Test
  public void purgeSnapshots() throws SQLException {
    setupData("purgeSnapshots");

    PurgeUtils.deleteSnapshotsData(getSession(), Arrays.asList(3, 4));

    checkTables("purgeSnapshots", "snapshots", "project_measures", "measure_data", "rule_failures", "snapshot_sources", "dependencies");
  }
}

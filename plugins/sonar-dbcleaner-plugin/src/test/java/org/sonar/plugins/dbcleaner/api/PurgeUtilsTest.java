/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import com.google.common.collect.Lists;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.database.DatabaseSession;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import javax.persistence.Query;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  public void purgeSnapshots() {
    setupData("purgeSnapshots");

    PurgeUtils.deleteSnapshotsData(getSession(), Arrays.asList(3, 4));

    checkTables("purgeSnapshots", "snapshots", "project_measures", "measure_data", "rule_failures", "snapshot_sources", "dependencies", "events", "duplications_index");
  }

  @Test
  public void shouldPaginate() throws Exception {
    DatabaseSession session = mock(DatabaseSession.class);
    Query query = mock(Query.class);
    when(session.createQuery(anyString())).thenReturn(query);
    when(session.createNativeQuery(anyString())).thenReturn(query);

    List<Integer> ids = Lists.newArrayList();
    for (int i = 0; i < (PurgeUtils.MAX_IN_ELEMENTS + 1); i++) {
      ids.add(i);
    }

    // createQuery() and createNativeQuery() should be invoked as many times as commit(), because it starts new transaction

    PurgeUtils.executeQuery(session, "description", ids, "hql");
    verify(session, times(2)).createQuery(anyString());
    verify(session, times(2)).commit();

    PurgeUtils.executeNativeQuery(session, "description", ids, "sql");
    verify(session, times(2)).createNativeQuery(anyString());
    verify(session, times(4)).commit();
  }

}

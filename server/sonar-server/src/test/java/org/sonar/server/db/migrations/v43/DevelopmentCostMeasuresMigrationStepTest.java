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

package org.sonar.server.db.migrations.v43;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DevelopmentCostMeasuresMigrationStepTest {

  @ClassRule
  public static DbTester db = new DbTester().schema(DevelopmentCostMeasuresMigrationStepTest.class, "schema.sql");

  @Mock
  PropertiesDao propertiesDao;

  DevelopmentCostMeasuresMigrationStep migration;

  @Before
  public void setUp() throws Exception {
    when(propertiesDao.selectGlobalProperty(WorkDurationConvertor.HOURS_IN_DAY_PROPERTY)).thenReturn(new PropertyDto().setValue("8"));

    migration = new DevelopmentCostMeasuresMigrationStep(db.database(), propertiesDao);
  }

  @Test
  public void migrate_dev_cost_measures() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_dev_cost_measures.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "migrate_dev_cost_measures_result.xml", "project_measures");
  }

  @Test
  public void metric_does_not_exist() throws Exception {
    db.prepareDbUnit(getClass(), "metric_does_not_exist.xml");

    migration.execute();

    db.assertDbUnit(getClass(), "metric_does_not_exist_result.xml", "project_measures");
  }
}

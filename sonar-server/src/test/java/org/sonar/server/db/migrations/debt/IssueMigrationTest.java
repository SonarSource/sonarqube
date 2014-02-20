/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.db.migrations.debt;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.TestDatabase;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueMigrationTest {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(IssueMigrationTest.class, "schema.sql");

  @Mock
  System2 system2;

  Settings settings;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-02-19").getTime());
    settings = new Settings();
  }

  @Test
  public void migrate_violations() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_issues_debt.xml");

    settings.setProperty(IssueMigration.HOURS_IN_DAY_PROPERTY, 8);
    new IssueMigration(db.database(), settings, system2).execute();

    db.assertDbUnit(getClass(), "migrate_issues_debt_result.xml", "issues");
  }

  @Test
  public void use_default_value_for_hours_in_day_when_no_property() throws Exception {
    db.prepareDbUnit(getClass(), "use_default_value_for_hours_in_day_when_no_property.xml");

    new IssueMigration(db.database(), settings, system2).execute();

    db.assertDbUnit(getClass(), "use_default_value_for_hours_in_day_when_no_property_result.xml", "issues");
  }

  @Test
  public void fail_on_bad_hours_in_day_settings() throws Exception {
    db.prepareDbUnit(getClass(), "migrate_issues_debt.xml");

    try {
      settings.setProperty(IssueMigration.HOURS_IN_DAY_PROPERTY, -2);
      new IssueMigration(db.database(), settings, system2).execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class);
    }
  }

}

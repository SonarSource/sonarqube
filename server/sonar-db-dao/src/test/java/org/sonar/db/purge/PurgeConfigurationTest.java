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
package org.sonar.db.purge;

import java.util.Collections;
import java.util.Date;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.config.PurgeConstants;
import org.sonar.core.config.PurgeProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgeConfigurationTest {
  @Test
  public void should_delete_all_closed_issues() {
    PurgeConfiguration conf = new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 0, System2.INSTANCE, Collections.emptyList());
    assertThat(conf.maxLiveDateOfClosedIssues()).isNull();

    conf = new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], -1, System2.INSTANCE, Collections.emptyList());
    assertThat(conf.maxLiveDateOfClosedIssues()).isNull();
  }

  @Test
  public void should_delete_only_old_closed_issues() {
    Date now = DateUtils.parseDate("2013-05-18");

    PurgeConfiguration conf = new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30, System2.INSTANCE, Collections.emptyList());
    Date toDate = conf.maxLiveDateOfClosedIssues(now);

    assertThat(toDate.getYear()).isEqualTo(113);// =2013
    assertThat(toDate.getMonth()).isEqualTo(3); // means April
    assertThat(toDate.getDate()).isEqualTo(18);
  }

  @Test
  public void do_not_delete_directory_by_default() {
    MapSettings settings = new MapSettings(new PropertyDefinitions(PurgeProperties.all()));
    settings.setProperty(PurgeConstants.PROPERTY_CLEAN_DIRECTORY, false);
    settings.setProperty(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES, 5);
    Date now = new Date();

    PurgeConfiguration underTest = PurgeConfiguration.newDefaultPurgeConfiguration(settings.asConfig(), new IdUuidPair(42L, "any-uuid"), Collections.emptyList());

    assertThat(underTest.scopesWithoutHistoricalData()).contains(Scopes.FILE)
      .doesNotContain(Scopes.DIRECTORY);
    assertThat(underTest.maxLiveDateOfClosedIssues(now)).isEqualTo(DateUtils.addDays(now, -5));
  }

  @Test
  public void delete_directory_if_in_settings() {
    MapSettings settings = new MapSettings(new PropertyDefinitions(PurgeProperties.all()));
    settings.setProperty(PurgeConstants.PROPERTY_CLEAN_DIRECTORY, true);

    PurgeConfiguration underTest = PurgeConfiguration.newDefaultPurgeConfiguration(settings.asConfig(), new IdUuidPair(42L, "any-uuid"), Collections.emptyList());

    assertThat(underTest.scopesWithoutHistoricalData()).contains(Scopes.DIRECTORY, Scopes.FILE);
  }
}

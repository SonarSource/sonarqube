/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Date;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.config.PurgeConstants;
import org.sonar.core.config.PurgeProperties;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class PurgeConfigurationTest {
  @Test
  public void should_delete_all_closed_issues() {
    PurgeConfiguration conf = new PurgeConfiguration("root", "project", emptyList(), 0, Optional.empty(), System2.INSTANCE, emptyList());
    assertThat(conf.maxLiveDateOfClosedIssues()).isNull();

    conf = new PurgeConfiguration("root", "project", emptyList(), -1, Optional.empty(), System2.INSTANCE, emptyList());
    assertThat(conf.maxLiveDateOfClosedIssues()).isNull();
  }

  @Test
  public void should_delete_only_old_closed_issues() {
    Date now = DateUtils.parseDate("2013-05-18");

    PurgeConfiguration conf = new PurgeConfiguration("root", "project", emptyList(), 30, Optional.empty(), System2.INSTANCE, emptyList());
    Date toDate = conf.maxLiveDateOfClosedIssues(now);

    assertThat(toDate.getYear()).isEqualTo(113);// =2013
    assertThat(toDate.getMonth()).isEqualTo(3); // means April
    assertThat(toDate.getDate()).isEqualTo(18);
  }

  @Test
  public void should_have_empty_branch_purge_date() {
    PurgeConfiguration conf = new PurgeConfiguration("root", "project", emptyList(), 30, Optional.of(10), System2.INSTANCE, emptyList());
    assertThat(conf.maxLiveDateOfInactiveShortLivingBranches()).isNotEmpty();
    long tenDaysAgo = DateUtils.addDays(new Date(System2.INSTANCE.now()), -10).getTime();
    assertThat(conf.maxLiveDateOfInactiveShortLivingBranches().get().getTime()).isBetween(tenDaysAgo - 5000, tenDaysAgo + 5000);
  }

  @Test
  public void should_calculate_branch_purge_date() {
    PurgeConfiguration conf = new PurgeConfiguration("root", "project", emptyList(), 30, Optional.empty(), System2.INSTANCE, emptyList());
    assertThat(conf.maxLiveDateOfInactiveShortLivingBranches()).isEmpty();
  }

  @Test
  public void delete_files_and_directories() {
    MapSettings settings = new MapSettings(new PropertyDefinitions(PurgeProperties.all()));
    settings.setProperty(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES, 5);
    Date now = new Date();

    PurgeConfiguration underTest = PurgeConfiguration.newDefaultPurgeConfiguration(settings.asConfig(), "root", "project", emptyList());

    assertThat(underTest.getScopesWithoutHistoricalData())
      .containsExactlyInAnyOrder(Scopes.DIRECTORY, Scopes.FILE);
    assertThat(underTest.maxLiveDateOfClosedIssues(now)).isEqualTo(DateUtils.addDays(now, -5));
  }
}

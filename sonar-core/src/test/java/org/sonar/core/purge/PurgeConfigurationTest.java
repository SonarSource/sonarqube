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
package org.sonar.core.purge;

import org.junit.Test;
import org.sonar.api.utils.DateUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgeConfigurationTest {
  @Test
  public void should_delete_all_closed_issues() throws Exception {
    PurgeConfiguration conf = new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 0);
    assertThat(conf.maxLiveDateOfClosedIssues()).isNull();

    conf = new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], -1);
    assertThat(conf.maxLiveDateOfClosedIssues()).isNull();
  }

  @Test
  public void should_delete_only_old_closed_issues() throws Exception {
    Date now = DateUtils.parseDate("2013-05-18");

    PurgeConfiguration conf = new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30);
    Date toDate = conf.maxLiveDateOfClosedIssues(now);

    assertThat(toDate.getYear()).isEqualTo(113);//=2013
    assertThat(toDate.getMonth()).isEqualTo(3); // means April
    assertThat(toDate.getDate()).isEqualTo(18);
  }
}

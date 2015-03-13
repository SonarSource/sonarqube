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

package org.sonar.server.computation.measure;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.persistence.MetricDao;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricCacheTest {

  @ClassRule
  public static DbTester db = new DbTester();

  MetricCache sut;

  @Before
  public void setUp() throws Exception {
    db.prepareDbUnit(getClass(), "metrics.xml");
    sut = new MetricCache(new DbClient(db.database(), db.myBatis(), new MetricDao()));
  }

  @Test
  public void cache_give_access_to_enabled_metrics() throws Exception {
    assertThat(sut.get("ncloc").getId()).isEqualTo(1);
    assertThat(sut.get("coverage").getId()).isEqualTo(2);
  }

  @Test(expected = NotFoundException.class)
  public void fail_when_metric_not_found() throws Exception {
    sut.get("complexity");
  }
}

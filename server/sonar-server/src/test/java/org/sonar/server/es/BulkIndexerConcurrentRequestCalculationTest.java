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
package org.sonar.server.es;

import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.AbstractIntegerAssert;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class BulkIndexerConcurrentRequestCalculationTest {

  private BulkIndexer indexer = mock(BulkIndexer.class);

  @Test
  public void should_parallelize() {
    setNumberOfProcesses(12);
    setNumberOfShards(2);

    assertConcurrentRequests()
      .isGreaterThan(0);
  }

  @Test
  public void should_parallelize_and_setting_about_shards_is_zero() {
    setNumberOfProcesses(12);
    setNumberOfShards(0);

    assertConcurrentRequests()
      .isGreaterThan(0);
  }

  @Test
  public void should_parallelize_and_setting_about_shards_is_not_available() {
    setNumberOfProcesses(12);
    setNumberOfShards(null);

    assertConcurrentRequests()
      .isGreaterThan(0);
  }

  @Test
  public void should_parallelize_and_setting_about_shards_is_unreadable() {
    setNumberOfProcesses(12);
    setNumberOfShards("not a number");

    assertConcurrentRequests()
      .isGreaterThan(0);
  }

  /**
   * @see https://jira.sonarsource.com/browse/SONAR-8075
   */
  @Test
  public void should_heavily_parallelize_on_96_cores() {
    setNumberOfProcesses(96);
    setNumberOfShards(5);

    assertConcurrentRequests()
      .isEqualTo(18);
  }

  private AbstractIntegerAssert<?> assertConcurrentRequests() {
    return assertThat(BulkIndexer.getConcurrentRequests(indexer));
  }

  private void setNumberOfShards(Object numberOfShards) {
    Map<String, Object> settings = new HashMap<>();
    settings.put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, numberOfShards);
    doReturn(settings).when(indexer).getLargeInitialSettings();
  }

  private void setNumberOfProcesses(int numberOfProcesses) {
    doReturn(numberOfProcesses).when(indexer).getProcesses();
  }
}

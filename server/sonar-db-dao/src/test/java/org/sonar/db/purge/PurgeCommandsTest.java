/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;


public class PurgeCommandsTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private PurgeProfiler profiler = new PurgeProfiler();

  /**
   * Test that SQL queries execution do not fail with a huge number of parameter
   */
  @Test
  public void should_not_fail_when_deleting_huge_number_of_analyses() {
    new PurgeCommands(dbTester.getSession(), profiler).deleteAnalyses(getHugeNumberOfIdUuidPairs());
    // The goal of this test is only to check that the query do no fail, not to check result
  }

  /**
   * Test that all related data is purged.
   */
  @Test
  public void shouldPurgeAnalysis() {
    dbTester.prepareDbUnit(getClass(), "shouldPurgeAnalysis.xml");

    new PurgeCommands(dbTester.getSession(), profiler).purgeAnalyses(singletonList(new IdUuidPair(1, "u1")));

    dbTester.assertDbUnit(getClass(), "shouldPurgeAnalysis-result.xml", "snapshots", "project_measures", "duplications_index", "events");
  }

  @Test
  public void delete_wasted_measures_when_purging_analysis() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteWastedMeasuresWhenPurgingAnalysis.xml");

    new PurgeCommands(dbTester.getSession(), profiler).purgeAnalyses(singletonList(new IdUuidPair(1, "u1")));

    dbTester.assertDbUnit(getClass(), "shouldDeleteWastedMeasuresWhenPurgingAnalysis-result.xml", "project_measures");
  }

  /**
   * Test that SQL queries execution do not fail with a huge number of parameter
   */
  @Test
  public void should_not_fail_when_purging_huge_number_of_analyses() {
    new PurgeCommands(dbTester.getSession(), profiler).purgeAnalyses(getHugeNumberOfIdUuidPairs());
    // The goal of this test is only to check that the query do no fail, not to check result
  }

  @Test
  public void shouldDeleteComponentsAndChildrenTables() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteResource.xml");

    PurgeCommands purgeCommands = new PurgeCommands(dbTester.getSession(), profiler);
    purgeCommands.deleteComponents(newArrayList(new IdUuidPair(1L, "uuid_1")));

    assertThat(dbTester.countRowsOfTable("projects")).isZero();
    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(1);
    assertThat(dbTester.countRowsOfTable("events")).isEqualTo(2);
    assertThat(dbTester.countRowsOfTable("issues")).isZero();
    assertThat(dbTester.countRowsOfTable("issue_changes")).isZero();
    assertThat(dbTester.countRowsOfTable("authors")).isZero();
  }


  @Test
  public void shouldDeleteAnalyses() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteResource.xml");

    PurgeCommands purgeCommands = new PurgeCommands(dbTester.getSession(), profiler);
    purgeCommands.deleteAnalyses("uuid_1");

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(1);
    assertThat(dbTester.countRowsOfTable("snapshots")).isZero();
    assertThat(dbTester.countRowsOfTable("events")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("issues")).isEqualTo(1);
    assertThat(dbTester.countRowsOfTable("issue_changes")).isEqualTo(1);
    assertThat(dbTester.countRowsOfTable("authors")).isEqualTo(2);
  }

  /**
   * Test that SQL queries execution do not fail with a huge number of parameter
   */
  @Test
  public void should_not_fail_when_deleting_huge_number_of_resources() {
    new PurgeCommands(dbTester.getSession(), profiler).deleteComponents(getHugeNumberOfIdUuids());
    // The goal of this test is only to check that the query do no fail, not to check result
  }

  private List<IdUuidPair> getHugeNumberOfIdUuids() {
    List<IdUuidPair> hugeNbOfSnapshotIds = newArrayList();
    for (long i = 0; i < 4500; i++) {
      hugeNbOfSnapshotIds.add(new IdUuidPair(i, String.valueOf(i)));
    }
    return hugeNbOfSnapshotIds;
  }

  private List<IdUuidPair> getHugeNumberOfIdUuidPairs() {
    List<IdUuidPair> hugeNbOfSnapshotIds = newArrayList();
    for (long i = 0; i < 4500; i++) {
      hugeNbOfSnapshotIds.add(new IdUuidPair(i, "uuid_" + i));
    }
    return hugeNbOfSnapshotIds;
  }

}

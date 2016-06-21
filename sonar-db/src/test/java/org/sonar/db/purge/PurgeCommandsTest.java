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
import static org.assertj.core.api.Assertions.assertThat;


public class PurgeCommandsTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private PurgeProfiler profiler = new PurgeProfiler();

  /**
   * Test that all related data is deleted.
   */
  @Test
  public void shouldDeleteSnapshot() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteSnapshot.xml");

    new PurgeCommands(dbTester.getSession(), profiler).deleteSnapshots(PurgeSnapshotQuery.create().setId(5L));

    dbTester.assertDbUnit(getClass(), "shouldDeleteSnapshot-result.xml", "snapshots", "project_measures", "duplications_index", "events");
  }

  /**
   * Test that SQL queries execution do not fail with a huge number of parameter
   */
  @Test
  public void should_not_fail_when_deleting_huge_number_of_snapshots() {
    new PurgeCommands(dbTester.getSession(), profiler).deleteSnapshots(getHugeNumberOfIdUuidPairs());
    // The goal of this test is only to check that the query do no fail, not to check result
  }

  /**
   * Test that all related data is purged.
   */
  @Test
  public void shouldPurgeSnapshot() {
    dbTester.prepareDbUnit(getClass(), "shouldPurgeSnapshot.xml");

    new PurgeCommands(dbTester.getSession(), profiler).purgeSnapshots(PurgeSnapshotQuery.create().setId(1L));

    dbTester.assertDbUnit(getClass(), "shouldPurgeSnapshot-result.xml", "snapshots", "project_measures", "duplications_index", "events");
  }

  @Test
  public void delete_wasted_measures_when_purging_snapshot() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteWastedMeasuresWhenPurgingSnapshot.xml");

    new PurgeCommands(dbTester.getSession(), profiler).purgeSnapshots(PurgeSnapshotQuery.create().setId(1L));

    dbTester.assertDbUnit(getClass(), "shouldDeleteWastedMeasuresWhenPurgingSnapshot-result.xml", "project_measures");
  }

  /**
   * Test that SQL queries execution do not fail with a huge number of parameter
   */
  @Test
  public void should_not_fail_when_purging_huge_number_of_snapshots() {
    new PurgeCommands(dbTester.getSession(), profiler).purgeSnapshots(getHugeNumberOfIdUuidPairs());
    // The goal of this test is only to check that the query do no fail, not to check result
  }

  @Test
  public void shouldDeleteResource() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteResource.xml");

    new PurgeCommands(dbTester.getSession(), profiler).deleteComponents(newArrayList(new IdUuidPair(1L, "uuid_1")));

    assertThat(dbTester.countRowsOfTable("projects")).isZero();
    assertThat(dbTester.countRowsOfTable("snapshots")).isZero();
    assertThat(dbTester.countRowsOfTable("events")).isZero();
    assertThat(dbTester.countRowsOfTable("issues")).isZero();
    assertThat(dbTester.countRowsOfTable("issue_changes")).isZero();
    assertThat(dbTester.countRowsOfTable("authors")).isZero();
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

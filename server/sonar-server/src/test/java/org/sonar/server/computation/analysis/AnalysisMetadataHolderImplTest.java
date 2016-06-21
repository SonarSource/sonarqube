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
package org.sonar.server.computation.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.computation.snapshot.Snapshot;

import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisMetadataHolderImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  static Snapshot BASE_PROJECT_SNAPSHOT = new Snapshot.Builder()
    .setId(1)
    .setUuid("uuid_1")
    .setCreatedAt(123456789L)
    .build();

  static long SOME_DATE = 10000000L;

  @Test
  public void getAnalysisDate_returns_date_with_same_time_as_the_one_set_with_setAnalysisDate() throws InterruptedException {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setAnalysisDate(SOME_DATE);

    assertThat(underTest.getAnalysisDate()).isEqualTo(SOME_DATE);
  }

  @Test
  public void getAnalysisDate_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis date has not been set");

    new AnalysisMetadataHolderImpl().getAnalysisDate();
  }

  @Test
  public void setAnalysisDate_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setAnalysisDate(SOME_DATE);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis date has already been set");

    underTest.setAnalysisDate(SOME_DATE);
  }

  @Test
  public void isFirstAnalysis_return_true() throws Exception {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setBaseProjectSnapshot(null);
    assertThat(underTest.isFirstAnalysis()).isTrue();
  }

  @Test
  public void isFirstAnalysis_return_false() throws Exception {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
    assertThat(underTest.isFirstAnalysis()).isFalse();
  }

  @Test
  public void isFirstAnalysis_throws_ISE_when_base_project_snapshot_is_not_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Base project snapshot has not been set");

    new AnalysisMetadataHolderImpl().isFirstAnalysis();
  }

  @Test
  public void baseProjectSnapshot_throws_ISE_when_base_project_snapshot_is_not_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Base project snapshot has not been set");

    new AnalysisMetadataHolderImpl().getBaseProjectSnapshot();
  }

  @Test
  public void setBaseProjectSnapshot_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Base project snapshot has already been set");
    underTest.setBaseProjectSnapshot(BASE_PROJECT_SNAPSHOT);
  }

  @Test
  public void isCrossProjectDuplicationEnabled_return_true() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setCrossProjectDuplicationEnabled(true);

    assertThat(underTest.isCrossProjectDuplicationEnabled()).isEqualTo(true);
  }

  @Test
  public void isCrossProjectDuplicationEnabled_return_false() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setCrossProjectDuplicationEnabled(false);

    assertThat(underTest.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

  @Test
  public void isCrossProjectDuplicationEnabled_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cross project duplication flag has not been set");

    new AnalysisMetadataHolderImpl().isCrossProjectDuplicationEnabled();
  }

  @Test
  public void setIsCrossProjectDuplicationEnabled_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setCrossProjectDuplicationEnabled(true);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cross project duplication flag has already been set");
    underTest.setCrossProjectDuplicationEnabled(false);
  }

  @Test
  public void set_branch() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setBranch("origin/master");

    assertThat(underTest.getBranch()).isEqualTo("origin/master");
  }

  @Test
  public void set_no_branch() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setBranch(null);

    assertThat(underTest.getBranch()).isNull();
  }

  @Test
  public void branch_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has not been set");

    new AnalysisMetadataHolderImpl().getBranch();
  }

  @Test
  public void setBranch_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setBranch("origin/master");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has already been set");
    underTest.setBranch("origin/master");
  }

  @Test
  public void getRootComponentRef() throws InterruptedException {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setRootComponentRef(10);

    assertThat(underTest.getRootComponentRef()).isEqualTo(10);
  }

  @Test
  public void getRootComponentRef_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Root component ref has not been set");

    new AnalysisMetadataHolderImpl().getRootComponentRef();
  }

  @Test
  public void setRootComponentRef_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setRootComponentRef(10);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Root component ref has already been set");
    underTest.setRootComponentRef(9);
  }
}

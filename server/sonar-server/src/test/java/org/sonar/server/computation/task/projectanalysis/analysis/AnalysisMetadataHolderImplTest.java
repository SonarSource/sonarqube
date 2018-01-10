/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.BranchType;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.computation.task.projectanalysis.component.DefaultBranchImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalysisMetadataHolderImplTest {

  private static Analysis baseProjectAnalysis = new Analysis.Builder()
    .setId(1)
    .setUuid("uuid_1")
    .setCreatedAt(123456789L)
    .build();
  private static long SOME_DATE = 10000000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

  @Test
  public void getOrganization_throws_ISE_if_organization_is_not_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Organization has not been set");

    underTest.getOrganization();
  }

  @Test
  public void setOrganization_throws_NPE_is_parameter_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Organization can't be null");

    underTest.setOrganization(null);
  }

  @Test
  public void setOrganization_throws_ISE_if_called_twice() {
    Organization organization = Organization.from(new OrganizationDto().setUuid("uuid").setKey("key").setName("name").setDefaultQualityGateUuid("anyuuidr"));
    underTest.setOrganization(organization);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Organization has already been set");

    underTest.setOrganization(organization);
  }

  @Test
  public void getUuid_throws_ISE_if_organization_uuid_is_not_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis uuid has not been set");

    underTest.getUuid();
  }

  @Test
  public void setUuid_throws_NPE_is_parameter_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Analysis uuid can't be null");

    underTest.setUuid(null);
  }

  @Test
  public void setUuid_throws_ISE_if_called_twice() {
    underTest.setUuid("org1");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis uuid has already been set");

    underTest.setUuid("org1");
  }

  @Test
  public void getAnalysisDate_returns_date_with_same_time_as_the_one_set_with_setAnalysisDate() {

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
  public void hasAnalysisDateBeenSet_returns_false_when_holder_is_not_initialized() {
    assertThat(new AnalysisMetadataHolderImpl().hasAnalysisDateBeenSet()).isFalse();
  }

  @Test
  public void hasAnalysisDateBeenSet_returns_true_when_holder_date_is_set() {
    AnalysisMetadataHolderImpl holder = new AnalysisMetadataHolderImpl();
    holder.setAnalysisDate(46532);
    assertThat(holder.hasAnalysisDateBeenSet()).isTrue();
  }

  @Test
  public void isFirstAnalysis_return_true() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setBaseAnalysis(null);
    assertThat(underTest.isFirstAnalysis()).isTrue();
  }

  @Test
  public void isFirstAnalysis_return_false() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    underTest.setBaseAnalysis(baseProjectAnalysis);
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

    new AnalysisMetadataHolderImpl().getBaseAnalysis();
  }

  @Test
  public void setBaseProjectSnapshot_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setBaseAnalysis(baseProjectAnalysis);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Base project snapshot has already been set");
    underTest.setBaseAnalysis(baseProjectAnalysis);
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

    underTest.setBranch(new DefaultBranchImpl("master"));

    assertThat(underTest.getBranch().getName()).isEqualTo("master");
  }

  @Test
  public void getBranch_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has not been set");

    new AnalysisMetadataHolderImpl().getBranch();
  }

  @Test
  public void setBranch_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setBranch(new DefaultBranchImpl("master"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has already been set");
    underTest.setBranch(new DefaultBranchImpl("master"));
  }

  @Test
  public void set_and_get_project() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();

    Project project = new Project("U", "K", "N");
    underTest.setProject(project);

    assertThat(underTest.getProject()).isSameAs(project);
  }

  @Test
  public void getProject_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Project has not been set");

    new AnalysisMetadataHolderImpl().getProject();
  }

  @Test
  public void setProject_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setProject(new Project("U", "K", "N"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Project has already been set");
    underTest.setProject(new Project("U", "K", "N"));
  }

  @Test
  public void getRootComponentRef() {
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

  @Test
  public void getIsShortLivingBranch_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has not been set");

    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.isShortLivingBranch();
  }

  @Test
  public void getIsShortLivingBranch_returns_true() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.SHORT);

    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl();
    underTest.setBranch(branch);

    assertThat(underTest.isShortLivingBranch()).isTrue();
  }
}

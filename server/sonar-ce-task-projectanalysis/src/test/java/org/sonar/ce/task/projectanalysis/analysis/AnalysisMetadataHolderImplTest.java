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
package org.sonar.ce.task.projectanalysis.analysis;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.component.BranchType;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.platform.EditionProvider.Edition;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

@RunWith(DataProviderRunner.class)
public class AnalysisMetadataHolderImplTest {

  private static Analysis baseProjectAnalysis = new Analysis.Builder()
    .setId(1)
    .setUuid("uuid_1")
    .setCreatedAt(123456789L)
    .build();
  private static long SOME_DATE = 10000000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

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

    new AnalysisMetadataHolderImpl(editionProvider).getAnalysisDate();
  }

  @Test
  public void setAnalysisDate_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setAnalysisDate(SOME_DATE);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis date has already been set");

    underTest.setAnalysisDate(SOME_DATE);
  }

  @Test
  public void hasAnalysisDateBeenSet_returns_false_when_holder_is_not_initialized() {
    assertThat(new AnalysisMetadataHolderImpl(editionProvider).hasAnalysisDateBeenSet()).isFalse();
  }

  @Test
  public void hasAnalysisDateBeenSet_returns_true_when_holder_date_is_set() {
    AnalysisMetadataHolderImpl holder = new AnalysisMetadataHolderImpl(editionProvider);
    holder.setAnalysisDate(46532);
    assertThat(holder.hasAnalysisDateBeenSet()).isTrue();
  }

  @Test
  public void isFirstAnalysis_return_true() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    underTest.setBaseAnalysis(null);
    assertThat(underTest.isFirstAnalysis()).isTrue();
  }

  @Test
  public void isFirstAnalysis_return_false() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    underTest.setBaseAnalysis(baseProjectAnalysis);
    assertThat(underTest.isFirstAnalysis()).isFalse();
  }

  @Test
  public void isFirstAnalysis_throws_ISE_when_base_project_snapshot_is_not_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Base project snapshot has not been set");

    new AnalysisMetadataHolderImpl(editionProvider).isFirstAnalysis();
  }

  @Test
  public void baseProjectSnapshot_throws_ISE_when_base_project_snapshot_is_not_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Base project snapshot has not been set");

    new AnalysisMetadataHolderImpl(editionProvider).getBaseAnalysis();
  }

  @Test
  public void setBaseProjectSnapshot_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setBaseAnalysis(baseProjectAnalysis);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Base project snapshot has already been set");
    underTest.setBaseAnalysis(baseProjectAnalysis);
  }

  @Test
  public void isCrossProjectDuplicationEnabled_return_true() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    underTest.setCrossProjectDuplicationEnabled(true);

    assertThat(underTest.isCrossProjectDuplicationEnabled()).isEqualTo(true);
  }

  @Test
  public void isCrossProjectDuplicationEnabled_return_false() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    underTest.setCrossProjectDuplicationEnabled(false);

    assertThat(underTest.isCrossProjectDuplicationEnabled()).isEqualTo(false);
  }

  @Test
  public void isCrossProjectDuplicationEnabled_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cross project duplication flag has not been set");

    new AnalysisMetadataHolderImpl(editionProvider).isCrossProjectDuplicationEnabled();
  }

  @Test
  public void setIsCrossProjectDuplicationEnabled_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setCrossProjectDuplicationEnabled(true);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Cross project duplication flag has already been set");
    underTest.setCrossProjectDuplicationEnabled(false);
  }

  @Test
  public void set_branch() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    underTest.setBranch(new DefaultBranchImpl());

    assertThat(underTest.getBranch().getName()).isEqualTo("master");
  }

  @Test
  public void getBranch_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has not been set");

    new AnalysisMetadataHolderImpl(editionProvider).getBranch();
  }

  @Test
  public void setBranch_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setBranch(new DefaultBranchImpl());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has already been set");
    underTest.setBranch(new DefaultBranchImpl());
  }

  @Test
  @UseDataProvider("anyEditionIncludingNone")
  public void setBranch_does_not_fail_if_main_branch_on_any_edition(@Nullable Edition edition) {
    when(editionProvider.get()).thenReturn(Optional.ofNullable(edition));
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(true);
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    underTest.setBranch(branch);

    assertThat(underTest.getBranch()).isSameAs(branch);
  }

  @Test
  @UseDataProvider("anyEditionIncludingNoneButCommunity")
  public void setBranch_does_not_fail_if_non_main_on_any_edition_but_Community(@Nullable Edition edition) {
    when(editionProvider.get()).thenReturn(Optional.ofNullable(edition));
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    underTest.setBranch(branch);

    assertThat(underTest.getBranch()).isSameAs(branch);
  }

  @Test
  public void setBranch_fails_if_non_main_branch_on_Community_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(Edition.COMMUNITY));
    Branch branch = mock(Branch.class);
    when(branch.isMain()).thenReturn(false);
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branches and Pull Requests are not supported in Community Edition");

    underTest.setBranch(branch);
  }

  @DataProvider
  public static Object[][] anyEditionIncludingNone() {
    return Stream.concat(
      Stream.of((Edition) null),
      Arrays.stream(Edition.values()))
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @DataProvider
  public static Object[][] anyEditionIncludingNoneButCommunity() {
    return Stream.concat(
      Stream.of((Edition) null),
      Arrays.stream(Edition.values())).filter(t -> t != Edition.COMMUNITY)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void setPullRequestId() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    String pullRequestId = "pr-123";
    underTest.setPullRequestKey(pullRequestId);

    assertThat(underTest.getPullRequestKey()).isEqualTo(pullRequestId);
  }

  @Test
  public void getPullRequestId_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Pull request key has not been set");

    new AnalysisMetadataHolderImpl(editionProvider).getPullRequestKey();
  }

  @Test
  public void setPullRequestId_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setPullRequestKey("pr-123");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Pull request key has already been set");
    underTest.setPullRequestKey("pr-234");
  }

  @Test
  public void set_and_get_project() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    Project project = Project.from(newPrivateProjectDto(newOrganizationDto()));
    underTest.setProject(project);

    assertThat(underTest.getProject()).isSameAs(project);
  }

  @Test
  public void getProject_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Project has not been set");

    new AnalysisMetadataHolderImpl(editionProvider).getProject();
  }

  @Test
  public void setProject_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setProject(Project.from(newPrivateProjectDto(newOrganizationDto())));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Project has already been set");

    underTest.setProject(Project.from(newPrivateProjectDto(newOrganizationDto())));
  }

  @Test
  public void getRootComponentRef() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    underTest.setRootComponentRef(10);

    assertThat(underTest.getRootComponentRef()).isEqualTo(10);
  }

  @Test
  public void getRootComponentRef_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Root component ref has not been set");

    new AnalysisMetadataHolderImpl(editionProvider).getRootComponentRef();
  }

  @Test
  public void setRootComponentRef_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setRootComponentRef(10);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Root component ref has already been set");
    underTest.setRootComponentRef(9);
  }

  @Test
  public void getIsShortLivingBranch_throws_ISE_when_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Branch has not been set");

    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.isShortLivingBranch();
  }

  @Test
  public void getIsShortLivingBranch_returns_true() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.SHORT);

    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setBranch(branch);

    assertThat(underTest.isShortLivingBranch()).isTrue();
  }

  @Test
  public void getIsSLBorPR_returns_true() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.SHORT);

    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setBranch(branch);

    assertThat(underTest.isSLBorPR()).isTrue();
  }

  @Test
  public void getIsSLBorPR_returns_false() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.LONG);

    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setBranch(branch);

    assertThat(underTest.isSLBorPR()).isFalse();
  }

  @Test
  public void getPullRequestBranch_returns_true() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);

    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setBranch(branch);

    assertThat(underTest.isPullRequest()).isTrue();
  }

  @Test
  public void setScmRevision_throws_ISE_when_called_twice() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setScmRevision("bd56dab");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("ScmRevision has already been set");
    underTest.setScmRevision("bd56dab");
  }

  @Test
  public void getScmRevision_returns_empty_if_scmRevision_is_not_initialized() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);

    assertThat(underTest.getScmRevision()).isNotPresent();
  }

  @Test
  public void getScmRevision_returns_scmRevision_if_scmRevision_is_initialized() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setScmRevision("bd56dab");
    assertThat(underTest.getScmRevision()).hasValue("bd56dab");
  }

  @Test
  public void getScmRevision_does_not_return_empty_string() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setScmRevision("");
    assertThat(underTest.getScmRevision()).isEmpty();
  }

  @Test
  public void getScmRevision_does_not_return_blank_string() {
    AnalysisMetadataHolderImpl underTest = new AnalysisMetadataHolderImpl(editionProvider);
    underTest.setScmRevision("    ");
    assertThat(underTest.getScmRevision()).isEmpty();
  }
}

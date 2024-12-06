/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.report;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ReportSubscriptionDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ReportSubscriptionDao underTest = db.getDbClient().reportSubscriptionDao();


  @Test
  void insert_shouldInsertSubscriptionCorrectly() {
    underTest.insert(db.getSession(), createSubscriptionDto("uuid").setPortfolioUuid("pf").setUserUuid("userUuid"));
    Set<ReportSubscriptionDto> reportSubscriptionDtos = underTest.selectAll(db.getSession());
    assertThat(reportSubscriptionDtos).hasSize(1);
    assertThat(reportSubscriptionDtos.iterator().next()).extracting(ReportSubscriptionDto::getUuid, ReportSubscriptionDto::getUserUuid, ReportSubscriptionDto::getBranchUuid,
        ReportSubscriptionDto::getPortfolioUuid)
      .containsExactly("uuid", "userUuid", null, "pf");
  }

  @Test
  void insert_shouldPersistOnlyOneSubscription() {
    ReportSubscriptionDto subscriptionDto = createSubscriptionDto("uuid").setPortfolioUuid("pf").setUserUuid("userUuid");
    underTest.insert(db.getSession(), subscriptionDto);
    db.getSession().commit();

    assertThatThrownBy(() -> underTest.insert(db.getSession(), subscriptionDto)).isNotNull();
  }

  @Test
  void insert_shouldPersistDifferentSubscriptions() {
    underTest.insert(db.getSession(), createSubscriptionDto("uuid").setBranchUuid("branch").setUserUuid("userUuid"));
    underTest.insert(db.getSession(), createSubscriptionDto("uuid2").setBranchUuid("branch").setUserUuid("userUuid2"));
    underTest.insert(db.getSession(), createSubscriptionDto("uuid3").setBranchUuid("branch2").setUserUuid("userUuid"));
    underTest.insert(db.getSession(), createSubscriptionDto("uuid4").setPortfolioUuid("pf").setUserUuid("userUuid"));

    Set<ReportSubscriptionDto> reportSubscriptionDtos = underTest.selectAll(db.getSession());
    assertThat(reportSubscriptionDtos).hasSize(4);
  }

  @Test
  void delete_shouldRemoveExistingSubscription() {
    ReportSubscriptionDto subscriptionPf = createSubscriptionDto("uuid").setPortfolioUuid("pf").setUserUuid("userUuid");
    ReportSubscriptionDto subscriptionBranch = createSubscriptionDto("uuid2").setBranchUuid("branch").setUserUuid("userUuid2");

    underTest.insert(db.getSession(), subscriptionPf);
    underTest.insert(db.getSession(), subscriptionBranch);

    underTest.delete(db.getSession(), subscriptionPf);

    assertThat(underTest.selectAll(db.getSession())).hasSize(1)
      .extracting(ReportSubscriptionDto::getUuid).containsExactly("uuid2");

    underTest.delete(db.getSession(), subscriptionBranch);

    assertThat(underTest.selectAll(db.getSession())).isEmpty();
  }

  @Test
  void selectByPortfolio_shouldReturnRelatedListOfSubscriptions() {
    ReportSubscriptionDto subscriptionPf = createSubscriptionDto("uuid").setPortfolioUuid("pf").setUserUuid("userUuid");
    ReportSubscriptionDto subscriptionPf2 = createSubscriptionDto("uuid2").setPortfolioUuid("pf").setUserUuid("userUuid2");
    ReportSubscriptionDto subscriptionBranch = createSubscriptionDto("uuid3").setBranchUuid("branch").setUserUuid("userUuid2");

    underTest.insert(db.getSession(), subscriptionPf);
    underTest.insert(db.getSession(), subscriptionPf2);
    underTest.insert(db.getSession(), subscriptionBranch);

    List<ReportSubscriptionDto> reportSubscriptionDtos = underTest.selectByPortfolio(db.getSession(), subscriptionPf.getPortfolioUuid());

    assertThat(reportSubscriptionDtos).hasSize(2).extracting(ReportSubscriptionDto::getUuid, ReportSubscriptionDto::getUserUuid, ReportSubscriptionDto::getPortfolioUuid)
      .containsExactly(tuple("uuid", "userUuid", "pf"), tuple("uuid2", "userUuid2", "pf"));
  }

  @Test
  void selectByProjectBranch_shouldReturnRelatedListOfSubscriptions() {
    ReportSubscriptionDto subscriptionBranch = createSubscriptionDto("uuid").setBranchUuid("branch").setUserUuid("userUuid");
    ReportSubscriptionDto subscriptionBranch2 = createSubscriptionDto("uuid2").setBranchUuid("branch").setUserUuid("userUuid2");
    ReportSubscriptionDto subscriptionPf = createSubscriptionDto("uuid3").setPortfolioUuid("pf1").setUserUuid("userUuid2");

    underTest.insert(db.getSession(), subscriptionBranch);
    underTest.insert(db.getSession(), subscriptionBranch2);
    underTest.insert(db.getSession(), subscriptionPf);

    List<ReportSubscriptionDto> reportSubscriptionDtos = underTest.selectByProjectBranch(db.getSession(), "branch");

    assertThat(reportSubscriptionDtos).hasSize(2).extracting(ReportSubscriptionDto::getUuid, ReportSubscriptionDto::getUserUuid, ReportSubscriptionDto::getBranchUuid)
      .containsExactly(tuple("uuid", "userUuid", "branch"), tuple("uuid2", "userUuid2", "branch"));
  }

  @Test
  void selectByUserAndPortfolio_shouldReturnRelatedSubscription() {
    ReportSubscriptionDto subscriptionPf = createSubscriptionDto("uuid").setPortfolioUuid("pf").setUserUuid("userUuid");
    ReportSubscriptionDto subscriptionPf2 = createSubscriptionDto("uuid2").setPortfolioUuid("pf").setUserUuid("userUuid2");
    ReportSubscriptionDto subscriptionBranch = createSubscriptionDto("uuid3").setBranchUuid("branch").setUserUuid("userUuid2");

    underTest.insert(db.getSession(), subscriptionPf);
    underTest.insert(db.getSession(), subscriptionPf2);
    underTest.insert(db.getSession(), subscriptionBranch);

    Optional<ReportSubscriptionDto> reportSubscriptionDtos = underTest.selectByUserAndPortfolio(db.getSession(),
      subscriptionPf.getPortfolioUuid(), subscriptionPf.getUserUuid());

    assertThat(reportSubscriptionDtos).isPresent().get().extracting(ReportSubscriptionDto::getUuid).isEqualTo("uuid");
  }

  @Test
  void selectByUserAndBranch_shouldReturnRelatedSubscription() {
    ReportSubscriptionDto subscriptionPf = createSubscriptionDto("uuid").setPortfolioUuid("pf").setUserUuid("userUuid");
    ReportSubscriptionDto subscriptionPf2 = createSubscriptionDto("uuid2").setPortfolioUuid("pf").setUserUuid("userUuid2");
    ReportSubscriptionDto subscriptionBranch = createSubscriptionDto("uuid3").setBranchUuid("branch").setUserUuid("userUuid2");

    underTest.insert(db.getSession(), subscriptionPf);
    underTest.insert(db.getSession(), subscriptionPf2);
    underTest.insert(db.getSession(), subscriptionBranch);

    Optional<ReportSubscriptionDto> reportSubscriptionDtos = underTest.selectByUserAndBranch(db.getSession(),
      subscriptionBranch.getBranchUuid(), subscriptionBranch.getUserUuid());

    assertThat(reportSubscriptionDtos).isPresent().get().extracting(ReportSubscriptionDto::getUuid).isEqualTo("uuid3");
  }

  @Test
  void countByQualifier_shouldReturnCorrectValue() {
    ProjectData projectData1 = db.components().insertPrivateProject( p -> p.setQualifier("APP"));
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPrivateProject( p -> p.setQualifier("TRK"));
    ComponentDto mainBranch2 = projectData2.getMainBranchComponent();

    ComponentDto branch1 = db.components().insertProjectBranch(mainBranch1);
    ComponentDto branch2 = db.components().insertProjectBranch(mainBranch2);

    ReportSubscriptionDto subscriptionBranch1 = createSubscriptionDto("uuid2").setBranchUuid(branch1.branchUuid()).setUserUuid("userUuid2");
    ReportSubscriptionDto subscriptionBranch2 = createSubscriptionDto("uuid3").setBranchUuid(branch2.branchUuid()).setUserUuid("userUuid3");
    ReportSubscriptionDto subscriptionBranch3 = createSubscriptionDto("uuid4").setBranchUuid(branch2.branchUuid()).setUserUuid("userUuid4");
    ReportSubscriptionDto subscriptionBranch4 = createSubscriptionDto("uuid").setPortfolioUuid("pf_uuid").setUserUuid("userUuid");

    underTest.insert(db.getSession(), subscriptionBranch1);
    underTest.insert(db.getSession(), subscriptionBranch2);
    underTest.insert(db.getSession(), subscriptionBranch3);
    underTest.insert(db.getSession(), subscriptionBranch4);

    assertThat(underTest.countByQualifier(db.getSession(), "APP")).isEqualTo(1);
    assertThat(underTest.countByQualifier(db.getSession(), "TRK")).isEqualTo(2);
  }

  @Test
  void countPerProject_shouldReturnCorrectValue() {
    ProjectData projectData1 = db.components().insertPrivateProject(p -> p.setQualifier("TRK"));
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPrivateProject(p -> p.setQualifier("TRK"));
    ComponentDto mainBranch2 = projectData2.getMainBranchComponent();

    ComponentDto branch1 = db.components().insertProjectBranch(mainBranch1);
    ComponentDto branch2 = db.components().insertProjectBranch(mainBranch2);

    ReportSubscriptionDto subscriptionBranch1 = createSubscriptionDto("uuid2").setBranchUuid(branch1.branchUuid()).setUserUuid("userUuid2");
    ReportSubscriptionDto subscriptionBranch2 = createSubscriptionDto("uuid3").setBranchUuid(branch2.branchUuid()).setUserUuid("userUuid3");
    ReportSubscriptionDto subscriptionBranch3 = createSubscriptionDto("uuid4").setBranchUuid(branch2.branchUuid()).setUserUuid("userUuid4");
    ReportSubscriptionDto subscriptionBranch4 = createSubscriptionDto("uuid").setPortfolioUuid("pf_uuid").setUserUuid("userUuid");

    underTest.insert(db.getSession(), subscriptionBranch1);
    underTest.insert(db.getSession(), subscriptionBranch2);
    underTest.insert(db.getSession(), subscriptionBranch3);
    underTest.insert(db.getSession(), subscriptionBranch4);
    assertThat(underTest.countPerProject(db.getSession())).hasSize(2)
      .containsEntry(projectData1.projectUuid(), 1)
      .containsEntry(projectData2.projectUuid(), 2);
  }

  @NotNull
  private static ReportSubscriptionDto createSubscriptionDto(String uuid) {
    return new ReportSubscriptionDto().setUuid(uuid);
  }


}

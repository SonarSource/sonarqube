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
package org.sonar.server.ce.queue;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.ce.queue.BranchSupport.ComponentKey;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

@RunWith(DataProviderRunner.class)
public class BranchSupportTest {
  private static final Map<String, String> NO_CHARACTERISTICS = Collections.emptyMap();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private BranchSupportDelegate branchSupportDelegate = mock(BranchSupportDelegate.class);
  private BranchSupport underTestNoBranch = new BranchSupport();
  private BranchSupport underTestWithBranch = new BranchSupport(branchSupportDelegate);

  @Test
  public void createComponentKey_of_main_branch() {
    String projectKey = randomAlphanumeric(12);

    ComponentKey componentKey = underTestNoBranch.createComponentKey(projectKey, null, NO_CHARACTERISTICS);

    assertThat(componentKey)
      .isEqualTo(underTestWithBranch.createComponentKey(projectKey, null, NO_CHARACTERISTICS));
    assertThat(componentKey.getKey()).isEqualTo(projectKey);
    assertThat(componentKey.getDbKey()).isEqualTo(projectKey);
    assertThat(componentKey.isDeprecatedBranch()).isFalse();
    assertThat(componentKey.getMainBranchComponentKey()).isSameAs(componentKey);
    assertThat(componentKey.getBranch()).isEmpty();
    assertThat(componentKey.getPullRequestKey()).isEmpty();
  }

  @Test
  public void createComponentKey_of_deprecated_branch() {
    String projectKey = randomAlphanumeric(12);
    String deprecatedBranchName = randomAlphanumeric(12);

    ComponentKey componentKey = underTestNoBranch.createComponentKey(projectKey, deprecatedBranchName, NO_CHARACTERISTICS);

    assertThat(componentKey)
      .isEqualTo(underTestWithBranch.createComponentKey(projectKey, deprecatedBranchName, NO_CHARACTERISTICS));
    assertThat(componentKey.getKey()).isEqualTo(projectKey);
    assertThat(componentKey.getDbKey()).isEqualTo(projectKey + ":" + deprecatedBranchName);
    assertThat(componentKey.isDeprecatedBranch()).isTrue();
    assertThat(componentKey.getMainBranchComponentKey()).isSameAs(componentKey);
    assertThat(componentKey.getBranch()).isEmpty();
    assertThat(componentKey.getPullRequestKey()).isEmpty();
  }

  @Test
  @UseDataProvider("nullOrNonEmpty")
  public void createComponentKey_with_ISE_if_characteristics_is_not_empty_and_delegate_is_null(String deprecatedBranchName) {
    String projectKey = randomAlphanumeric(12);
    Map<String, String> nonEmptyMap = newRandomNonEmptyMap();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Current edition does not support branch feature");
    
    underTestNoBranch.createComponentKey(projectKey, deprecatedBranchName, nonEmptyMap);
  }

  @Test
  public void createComponentKey_fails_with_IAE_if_characteristics_is_not_empty_and_deprecatedBranchName_is_non_null() {
    String projectKey = randomAlphanumeric(12);
    String deprecatedBranchName = randomAlphanumeric(13);
    Map<String, String> nonEmptyMap = newRandomNonEmptyMap();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Deprecated branch feature can't be used at the same time as new branch support");
    
    underTestWithBranch.createComponentKey(projectKey, deprecatedBranchName, nonEmptyMap);
  }

  @Test
  public void createComponentKey_delegates_to_delegate_if_characteristics_is_not_empty() {
    String projectKey = randomAlphanumeric(12);
    Map<String, String> nonEmptyMap = newRandomNonEmptyMap();
    ComponentKey expected = mock(ComponentKey.class);
    when(branchSupportDelegate.createComponentKey(projectKey, nonEmptyMap)).thenReturn(expected);

    ComponentKey componentKey = underTestWithBranch.createComponentKey(projectKey, null, nonEmptyMap);

    assertThat(componentKey).isSameAs(expected);
  }

  @Test
  public void createBranchComponent_fails_with_ISE_if_delegate_is_null() {
    DbSession dbSession = mock(DbSession.class);
    ComponentKey componentKey = mock(ComponentKey.class);
    OrganizationDto organization = new OrganizationDto();
    ComponentDto mainComponentDto = new ComponentDto();
    BranchDto mainComponentBranchDto = new BranchDto();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Current edition does not support branch feature");
    
    underTestNoBranch.createBranchComponent(dbSession, componentKey, organization, mainComponentDto, mainComponentBranchDto);
  }

  @Test
  public void createBranchComponent_delegates_to_delegate() {
    DbSession dbSession = mock(DbSession.class);
    ComponentKey componentKey = mock(ComponentKey.class);
    OrganizationDto organization = new OrganizationDto();
    ComponentDto mainComponentDto = new ComponentDto();
    ComponentDto expected = new ComponentDto();
    BranchDto mainComponentBranchDto = new BranchDto();
    when(branchSupportDelegate.createBranchComponent(dbSession, componentKey, organization, mainComponentDto, mainComponentBranchDto))
      .thenReturn(expected);

    ComponentDto dto = underTestWithBranch.createBranchComponent(dbSession, componentKey, organization, mainComponentDto, mainComponentBranchDto);

    assertThat(dto).isSameAs(expected);
  }

  @DataProvider
  public static Object[][] nullOrNonEmpty() {
    return new Object[][] {
      {null},
      {randomAlphabetic(5)},
    };
  }

  private static Map<String, String> newRandomNonEmptyMap() {
    return IntStream.range(0, 1 + new Random().nextInt(10)).boxed().collect(uniqueIndex(i -> "key_" + i, i -> "val_" + i));
  }
}

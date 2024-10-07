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
package org.sonar.server.ce.queue;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.ce.queue.BranchSupport.ComponentKey;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.ce.CeTaskCharacteristics.PULL_REQUEST;

@RunWith(DataProviderRunner.class)
public class BranchSupportTest {
  private static final Map<String, String> NO_CHARACTERISTICS = Collections.emptyMap();

  private final BranchSupportDelegate branchSupportDelegate = mock(BranchSupportDelegate.class);
  private final BranchSupport underTestNoBranch = new BranchSupport(null);
  private final BranchSupport underTestWithBranch = new BranchSupport(branchSupportDelegate);

  @Test
  public void createComponentKey_of_main_branch() {
    String projectKey = secure().nextAlphanumeric(12);

    ComponentKey componentKey = underTestNoBranch.createComponentKey(projectKey, NO_CHARACTERISTICS);

    assertThat(componentKey).isEqualTo(underTestWithBranch.createComponentKey(projectKey, NO_CHARACTERISTICS));
    assertThat(componentKey.getKey()).isEqualTo(projectKey);
    assertThat(componentKey.getBranchName()).isEmpty();
    assertThat(componentKey.getPullRequestKey()).isEmpty();
    verifyNoInteractions(branchSupportDelegate);
  }

  @Test
  public void createComponentKey_whenCharacteristicsIsRandom_returnsComponentKey() {
    String projectKey = secure().nextAlphanumeric(12);
    Map<String, String> nonEmptyMap = newRandomNonEmptyMap();

    ComponentKey componentKey = underTestWithBranch.createComponentKey(projectKey, nonEmptyMap);

    assertThat(componentKey).isEqualTo(underTestWithBranch.createComponentKey(projectKey, NO_CHARACTERISTICS));
    assertThat(componentKey.getKey()).isEqualTo(projectKey);
    assertThat(componentKey.getBranchName()).isEmpty();
    assertThat(componentKey.getPullRequestKey()).isEmpty();
    verifyNoInteractions(branchSupportDelegate);
  }

  @Test
  public void createComponentKey_whenCharacteristicsIsBranchRelated_delegates() {
    String projectKey = secure().nextAlphanumeric(12);
    Map<String, String> nonEmptyMap = Map.of(PULL_REQUEST, "PR-2");
    ComponentKey expected = mock(ComponentKey.class);
    when(branchSupportDelegate.createComponentKey(projectKey, nonEmptyMap)).thenReturn(expected);

    ComponentKey componentKey = underTestWithBranch.createComponentKey(projectKey, nonEmptyMap);

    assertThat(componentKey).isSameAs(expected);
  }

  @Test
  public void createBranchComponent_fails_with_ISE_if_delegate_is_null() {
    DbSession dbSession = mock(DbSession.class);
    ComponentKey componentKey = mock(ComponentKey.class);
    ComponentDto mainComponentDto = new ComponentDto();
    BranchDto mainComponentBranchDto = new BranchDto();

    assertThatThrownBy(() -> underTestNoBranch.createBranchComponent(dbSession, componentKey, mainComponentDto, mainComponentBranchDto))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Current edition does not support branch feature");
  }

  @Test
  public void createBranchComponent_delegates_to_delegate() {
    DbSession dbSession = mock(DbSession.class);
    ComponentKey componentKey = mock(ComponentKey.class);
    ComponentDto mainComponentDto = new ComponentDto();
    ComponentDto expected = new ComponentDto();
    BranchDto mainComponentBranchDto = new BranchDto();
    when(branchSupportDelegate.createBranchComponent(dbSession, componentKey, mainComponentDto, mainComponentBranchDto))
      .thenReturn(expected);

    ComponentDto dto = underTestWithBranch.createBranchComponent(dbSession, componentKey, mainComponentDto, mainComponentBranchDto);

    assertThat(dto).isSameAs(expected);
  }

  @DataProvider
  public static Object[][] nullOrNonEmpty() {
    return new Object[][] {
      {null},
      {secure().nextAlphabetic(5)},
    };
  }

  private static Map<String, String> newRandomNonEmptyMap() {
    return IntStream.range(0, 1 + new Random().nextInt(10)).boxed().collect(Collectors.toMap(i -> "key_" + i, i1 -> "val_" + i1));
  }
}

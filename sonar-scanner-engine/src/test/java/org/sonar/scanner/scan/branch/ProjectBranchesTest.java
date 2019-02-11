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
package org.sonar.scanner.scan.branch;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ProjectBranchesTest {

  private static final BranchInfo mainBranch = new BranchInfo("main", BranchType.LONG, true, null);
  private static final BranchInfo shortBranch = new BranchInfo("short", BranchType.SHORT, false, null);
  private static final BranchInfo longBranch = new BranchInfo("long", BranchType.LONG, false, null);
  private static final BranchInfo pullRequest = new BranchInfo("pull-request", BranchType.PULL_REQUEST, false, null);

  private static final List<BranchInfo> nonMainBranches = Arrays.asList(shortBranch, longBranch, pullRequest);

  private static final List<BranchInfo> allBranches = Arrays.asList(shortBranch, longBranch, pullRequest, mainBranch);

  private final ProjectBranches underTest = new ProjectBranches(allBranches);

  @Test
  public void defaultBranchName() {
    for (int i = 0; i <= nonMainBranches.size(); i++) {
      List<BranchInfo> branches = new ArrayList<>(nonMainBranches);
      branches.add(i, mainBranch);
      assertThat(new ProjectBranches(branches).defaultBranchName()).isEqualTo(mainBranch.name());
    }
  }

  @Test
  @UseDataProvider("branchNamesAndBranches")
  public void get(String branchName, BranchInfo branchInfo) {
    assertThat(underTest.get(branchName)).isEqualTo(branchInfo);
  }

  @DataProvider
  public static Object[][] branchNamesAndBranches() {
    return allBranches.stream()
      .map(b -> new Object[]{b.name(), b})
      .toArray(Object[][]::new);
  }

  @Test
  public void isEmpty() {
    assertThat(underTest.isEmpty()).isFalse();
    assertThat(new ProjectBranches(Collections.emptyList()).isEmpty()).isTrue();
  }
}

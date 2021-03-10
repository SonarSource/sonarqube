/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

package org.sonar.alm.client.azure;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class GsonAzureRepoTest {
  @Test
  @UseDataProvider("data")
  public void extract_branch_name_from_full_branch_name(String actualFullBranchName, String expectedBranchName) {
    GsonAzureRepo repo = new GsonAzureRepo(
      "repo_id",
      "repo_name",
      "repo_url",
      new GsonAzureProject(),
      actualFullBranchName
    );

    assertThat(repo.getDefaultBranchName()).isEqualTo(expectedBranchName);
  }

  @DataProvider
  public static Object[][] data() {
    return new Object[][]{
      {"refs/heads/default_branch_name", "default_branch_name"},
      {"refs/HEADS/default_branch_name", "default_branch_name"},
      {"refs/heads/Default_branch_name", "Default_branch_name"},
      {"branch_Name_without_prefix", "branch_Name_without_prefix"},
      {"", null},
      {null, null}
    };
  }
}

/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AzureDevOpsUrlsTest {

  private final String url;
  private final boolean expected;

  public AzureDevOpsUrlsTest(@Nullable String url, boolean expected) {
    this.url = url;
    this.expected = expected;
  }

  @Parameterized.Parameters(name = "{index} = {0}")
  public static List<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {"https://dev.azure.com/myorg", true},
      {"https://dev.azure.com", true},
      {"https://myorg.visualstudio.com", true},
      {"https://myorg.visualstudio.com/", true},
      {"https://tfs.corp.example.com/tfs", false},
      {"https://ado.sonarqube.com/", false},
      {"https://dev.azure.com.evil.com", false},
      {"https://notmyvisualstudio.com", false},
      {"https://evilvisualstudio.com", false},
      {null, false},
      {"", false},
      {"not a url", false},
    });
  }

  @Test
  public void isAzureDevOpsServices_matches_expected() {
    assertThat(AzureDevOpsUrls.isAzureDevOpsServices(url)).isEqualTo(expected);
  }
}

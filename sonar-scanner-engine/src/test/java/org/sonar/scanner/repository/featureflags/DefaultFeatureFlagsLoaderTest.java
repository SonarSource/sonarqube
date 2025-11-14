/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scanner.repository.featureflags;

import com.google.gson.Gson;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import wiremock.org.apache.hc.core5.http.HttpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DefaultFeatureFlagsLoaderTest {

  private DefaultFeatureFlagsLoader loader;
  private DefaultScannerWsClient wsClient;

  @BeforeEach
  void setUp() {
    wsClient = mock(DefaultScannerWsClient.class);
    BranchConfiguration branchConfig = mock(BranchConfiguration.class);
    when(branchConfig.isPullRequest()).thenReturn(false);
    loader = new DefaultFeatureFlagsLoader(wsClient);
  }

  @Test
  void load_shouldRequestFeatureFlagsAndParseResponse() {
    WsTestUtil.mockReader(wsClient, "/api/features/list", response());

    Set<String> features = loader.load();
    assertThat(features).containsExactlyInAnyOrder("feature1", "feature2");

    WsTestUtil.verifyCall(wsClient, "/api/features/list");

    verifyNoMoreInteractions(wsClient);
  }

  @Test
  void load_whenHasSomeError_shouldThrowIllegalStateException() {
    when(wsClient.call(any())).thenThrow(MessageException.of("You're not authorized"));

    assertThatException().isThrownBy(loader::load)
      .isInstanceOf(IllegalStateException.class)
      .withMessage("Unable to load feature flags");
  }

  private Reader response() {
    return toReader(List.of("feature1", "feature2"));
  }

  private static Reader toReader(List<String> featureFlags) {
    String json = new Gson().toJson(featureFlags);
    return new StringReader(json);
  }

}

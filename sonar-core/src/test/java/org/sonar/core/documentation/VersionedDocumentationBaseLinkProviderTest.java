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
package org.sonar.core.documentation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VersionedDocumentationBaseLinkProviderTest {

  @CsvSource({
    "http://base/, 1.2, http://base/1.2",
    "http://base, 1.2, http://base/1.2",
    "http://base/, 1.2-SNAPSHOT, http://base/latest",
    "http://base, 1.2-SNAPSHOT, http://base/latest",
  })
  @ParameterizedTest
  void formatBaseUrlWithVersion(String baseUrl, String version, String expected) {
    SonarQubeVersion sonarQubeVersion = mock();

    when(sonarQubeVersion.get()).thenReturn(Version.parse(version));

    VersionedDocumentationBaseLinkProvider versionedDocumentationBaseLinkProvider = new VersionedDocumentationBaseLinkProvider(baseUrl, sonarQubeVersion);

    assertThat(versionedDocumentationBaseLinkProvider.getDocumentationBaseUrl())
      .isEqualTo(expected);
  }
}

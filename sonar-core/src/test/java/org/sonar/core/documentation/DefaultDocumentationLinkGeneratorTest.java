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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.core.platform.SonarQubeVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDocumentationLinkGeneratorTest {
  private static final String TEST_SUFFIX = "/documentation/analyzing-source-code/scm-integration/";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SonarQubeVersion sonarQubeVersion;

  private DefaultDocumentationLinkGenerator documentationLinkGenerator;

  @Before
  public void setUp() {
    when(sonarQubeVersion.get().major()).thenReturn(100);
    when(sonarQubeVersion.get().minor()).thenReturn(1000);
    documentationLinkGenerator = new DefaultDocumentationLinkGenerator(sonarQubeVersion);
  }

  @Test
  public void getDocumentationLink_whenSuffixProvided_concatenatesIt() {
    String generatedLink = documentationLinkGenerator.getDocumentationLink(TEST_SUFFIX);

    assertThat(generatedLink).isEqualTo("https://docs.sonarqube.org/100.1000/documentation/analyzing-source-code/scm-integration/");
  }

  @Test
  public void getDocumentationLink_whenSuffixNotProvided_returnsBaseUrl() {
    String generatedLink = documentationLinkGenerator.getDocumentationLink(null);

    assertThat(generatedLink).isEqualTo("https://docs.sonarqube.org/100.1000");
  }
}

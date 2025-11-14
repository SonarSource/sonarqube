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
package org.sonar.core.documentation;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.config.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.DOCUMENTATION_BASE_URL;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDocumentationLinkGeneratorTest {
  private static final String TEST_SUFFIX = "/documentation/analyzing-source-code/scm-integration/";
  public static final String BASE_URL = "http://base";

  @Mock
  private Configuration configuration;
  @Mock
  private DocumentationBaseLinkProvider documentationBaseLinkProvider;

  private DefaultDocumentationLinkGenerator documentationLinkGenerator;

  @Before
  public void setUp() {
    when(configuration.get(DOCUMENTATION_BASE_URL)).thenReturn(Optional.empty());
    when(documentationBaseLinkProvider.getDocumentationBaseUrl()).thenReturn(BASE_URL);

    documentationLinkGenerator = new DefaultDocumentationLinkGenerator(configuration, documentationBaseLinkProvider);
  }

  @Test
  public void getDocumentationLink_whenSuffixProvided_concatenatesIt() {
    String generatedLink = documentationLinkGenerator.getDocumentationLink(TEST_SUFFIX);

    assertThat(generatedLink).isEqualTo(BASE_URL + "/documentation/analyzing-source-code/scm-integration/");
  }

  @Test
  public void getDocumentationLink_whenSuffixNotProvided_returnsBaseUrl() {
    String generatedLink = documentationLinkGenerator.getDocumentationLink(null);

    assertThat(generatedLink).isEqualTo(BASE_URL);
  }

  @Test
  public void getDocumentationLink_suffixProvided_withPropertyOverride() {
    String propertyValue = "https://new-url.sonarqube.org/";
    when(configuration.get(DOCUMENTATION_BASE_URL)).thenReturn(Optional.of(propertyValue));
    documentationLinkGenerator = new DefaultDocumentationLinkGenerator(configuration, documentationBaseLinkProvider);

    String generatedLink = documentationLinkGenerator.getDocumentationLink(TEST_SUFFIX);

    assertThat(generatedLink).isEqualTo(propertyValue + "documentation/analyzing-source-code/scm-integration/");
  }

  @Test
  public void getDocumentationLink_suffixNotProvided_withPropertyOverride() {
    String propertyValue = "https://new-url.sonarqube.org/";
    when(configuration.get(DOCUMENTATION_BASE_URL)).thenReturn(Optional.of(propertyValue));
    documentationLinkGenerator = new DefaultDocumentationLinkGenerator(configuration, documentationBaseLinkProvider);

    String generatedLink = documentationLinkGenerator.getDocumentationLink(null);

    assertThat(generatedLink).isEqualTo("https://new-url.sonarqube.org");
  }

  @Test
  public void getDocumentationLink_suffixNotProvided_withPropertyOverride_missingSlash() {
    String propertyValue = "https://new-url.sonarqube.org";
    when(configuration.get(DOCUMENTATION_BASE_URL)).thenReturn(Optional.of(propertyValue));
    documentationLinkGenerator = new DefaultDocumentationLinkGenerator(configuration, documentationBaseLinkProvider);

    String generatedLink = documentationLinkGenerator.getDocumentationLink(null);

    assertThat(generatedLink).isEqualTo(propertyValue);
  }
}

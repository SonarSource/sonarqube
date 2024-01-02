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
package org.sonar.auth.saml;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.auth.saml.SamlAuthStatusPageGenerator.getSamlAuthStatusHtml;

public class SamlAuthStatusPageGeneratorTest {
  private static final String EMPTY_HTML_TEMPLATE_NAME = "samlAuthResultEmpty.html";

  @Test
  public void test_full_html_generation_with_empty_values() {
    SamlAuthenticationStatus samlAuthenticationStatus = mock(SamlAuthenticationStatus.class);

    when(samlAuthenticationStatus.getStatus()).thenReturn(null);
    when(samlAuthenticationStatus.getErrors()).thenReturn(new ArrayList<>());
    when(samlAuthenticationStatus.getWarnings()).thenReturn(new ArrayList<>());
    when(samlAuthenticationStatus.getAvailableAttributes()).thenReturn(new HashMap<>());
    when(samlAuthenticationStatus.getMappedAttributes()).thenReturn(new HashMap<>());

    String completeHtmlTemplate = getSamlAuthStatusHtml(samlAuthenticationStatus);
    String expectedTemplate = loadTemplateFromResources(EMPTY_HTML_TEMPLATE_NAME);

    assertEquals(expectedTemplate, completeHtmlTemplate);

  }

  private String loadTemplateFromResources(String templateName) {
    URL url = Resources.getResource(templateName);
    try {
      return Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

}

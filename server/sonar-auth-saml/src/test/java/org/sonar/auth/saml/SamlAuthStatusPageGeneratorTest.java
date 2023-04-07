/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.auth.saml.SamlAuthStatusPageGenerator.getSamlAuthStatusHtml;

public class SamlAuthStatusPageGeneratorTest {
  private static final String EMPTY_DATA_RESPONSE = "eyJ3YXJuaW5ncyI6W10sImF2YWlsYWJsZUF0dHJpYnV0ZXMiOnt9LCJlcnJvcnMiOltdLCJtYXBwZWRBdHRyaWJ1dGVzIjp7fX0=";

  @Test
  public void getSamlAuthStatusHtml_whenCalled_shouldGeneratePageWithData() {
    SamlAuthenticationStatus samlAuthenticationStatus = mock(SamlAuthenticationStatus.class);
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

    when(samlAuthenticationStatus.getStatus()).thenReturn(null);
    when(samlAuthenticationStatus.getErrors()).thenReturn(new ArrayList<>());
    when(samlAuthenticationStatus.getWarnings()).thenReturn(new ArrayList<>());
    when(samlAuthenticationStatus.getAvailableAttributes()).thenReturn(new HashMap<>());
    when(samlAuthenticationStatus.getMappedAttributes()).thenReturn(new HashMap<>());
    when(httpServletRequest.getContextPath()).thenReturn("context");

    String completeHtmlTemplate = getSamlAuthStatusHtml(httpServletRequest, samlAuthenticationStatus);

    assertThat(completeHtmlTemplate).contains(EMPTY_DATA_RESPONSE);
  }
}

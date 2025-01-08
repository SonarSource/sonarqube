/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.api.impl.ws;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.server.ws.WebService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidatingRequestTest {

  ValidatingRequest underTest = Mockito.mock(ValidatingRequest.class, Mockito.CALLS_REAL_METHODS);
  @Test
  public void paramReturnsTheDefaultValue_whenDefaultIsSetAndValueIsNot() {
    underTest.setAction(mock(WebService.Action.class));
    WebService.Param param = mockParamWithDefaultValue("default");

    when(underTest.action().param("param")).thenReturn(param);

    assertThat(underTest.param("param")).isEqualTo("default");
  }

  @Test
  public void paramAsStringsReturnsTheDefaultValues_whenDefaultIsSetAndValueIsNot() {
    underTest.setAction(mock(WebService.Action.class));
    WebService.Param param = mockParamWithDefaultValue("default,values");

    when(underTest.action().param("param")).thenReturn(param);

    assertThat(underTest.paramAsStrings("param")).containsExactly("default", "values");
  }

  private static WebService.Param mockParamWithDefaultValue(String defaultValue) {
    WebService.Param param = mock(WebService.Param.class);
    when(param.defaultValue()).thenReturn(defaultValue);
    when(param.possibleValues()).thenReturn(null);
    when(param.maximumLength()).thenReturn(10);
    when(param.maximumLength()).thenReturn(10);
    when(param.maximumValue()).thenReturn(null);
    when(param.maxValuesAllowed()).thenReturn(2);
    return param;
  }
}

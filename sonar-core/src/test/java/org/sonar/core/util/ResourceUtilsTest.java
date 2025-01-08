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
package org.sonar.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

public class ResourceUtilsTest {

  @Test
  public void whenReadValidClasspathResource_thenReadIt() {
    String result = ResourceUtils.readClasspathResource(ResourceUtilsTest.class, "classpath_resource.txt");
    assertThat(result)
      .isEqualTo("OK\n");
  }

  @Test
  public void whenReadInexistantClasspathResource_thenThrow() {
    assertThatThrownBy(
      () -> ResourceUtils.readClasspathResource(ResourceUtilsTest.class, "inexistant_resource.txt"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to read classpath resource: inexistant_resource.txt of class: org.sonar.core.util");
  }

  @Test
  public void whenReadClasspathResourceFails_thenThrow() {
    try (MockedStatic<IOUtils> scopedMock = mockStatic(IOUtils.class)) {
      scopedMock.when(() -> IOUtils.toString(any(InputStream.class), any(Charset.class))).thenThrow(new IOException("error"));
      assertThatThrownBy(() -> ResourceUtils.readClasspathResource(ResourceUtilsTest.class, "classpath_resource.txt"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Fail to read classpath resource: classpath_resource.txt of class: org.sonar.core.util");
    }
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.server.ws.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import org.junit.Test;
import org.sonar.api.server.ws.Request;

public class SimpleGetRequestTest {

  SimpleGetRequest underTest = new SimpleGetRequest();

  @Test
  public void method() {
    assertThat(underTest.method()).isEqualTo("GET");

    underTest.setParam("foo", "bar");
    assertThat(underTest.param("foo")).isEqualTo("bar");
    assertThat(underTest.param("unknown")).isNull();
  }

  @Test
  public void has_param() {
    assertThat(underTest.method()).isEqualTo("GET");

    underTest.setParam("foo", "bar");
    assertThat(underTest.hasParam("foo")).isTrue();
    assertThat(underTest.hasParam("unknown")).isFalse();
  }

  @Test
  public void get_part() throws Exception {
    InputStream inputStream = mock(InputStream.class);
    underTest.setPart("key", inputStream, "filename");

    Request.Part part = underTest.paramAsPart("key");
    assertThat(part.getInputStream()).isEqualTo(inputStream);
    assertThat(part.getFileName()).isEqualTo("filename");

    assertThat(underTest.paramAsPart("unknown")).isNull();
  }
}

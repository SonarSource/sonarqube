/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.ws;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CacheWriterTest {
  private Writer writer = new StringWriter();
  private CacheWriter underTest = new CacheWriter(writer);

  @Test
  public void write_content_when_closing_resource() throws IOException {
    underTest.write("content");
    assertThat(writer.toString()).isEmpty();

    underTest.close();

    assertThat(writer.toString()).isEqualTo("content");
  }

  @Test
  public void close_encapsulated_writer_once() throws IOException {
    writer = mock(Writer.class);
    underTest = new CacheWriter(writer);

    underTest.close();
    underTest.close();

    verify(writer, times(1)).close();
  }
}

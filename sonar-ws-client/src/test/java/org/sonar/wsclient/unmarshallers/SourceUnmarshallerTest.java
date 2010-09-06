/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.unmarshallers;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.wsclient.services.Source;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SourceUnmarshallerTest {

  @Test
  public void toModel() throws IOException {
    Source source = new SourceUnmarshaller().toModel("[]");
    assertThat(source, nullValue());

    source = new SourceUnmarshaller().toModel(loadFile("/sources/source.json"));
    assertThat(source.getLines().size(), is(236));
    assertThat(source.getLine(3), is(" * Copyright (C) 2009 SonarSource SA"));
  }

  @Test
  public void fromLineToLine() throws IOException {
    Source source = new SourceUnmarshaller().toModel(loadFile("/sources/from_line_to_line.json"));
    assertThat(source.getLines().size(), is(15));
    assertThat(source.getLine(1), nullValue());
    assertThat(source.getLine(3), is(" * Copyright (C) 2009 SonarSource SA"));
  }

  private static String loadFile(String path) throws IOException {
    return IOUtils.toString(PropertyUnmarshallerTest.class.getResourceAsStream(path));
  }
}

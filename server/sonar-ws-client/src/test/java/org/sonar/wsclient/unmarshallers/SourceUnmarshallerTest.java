/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Source;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SourceUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void toModel() {
    Source source = new SourceUnmarshaller().toModel("[]");
    assertThat(source, nullValue());

    source = new SourceUnmarshaller().toModel(loadFile("/sources/source.json"));
    assertThat(source.getLines().size(), is(236));
    assertThat(source.getLine(3), is(" * Copyright (C) 2008-2014 SonarSource"));
  }

  @Test
  public void fromLineToLine() {
    Source source = new SourceUnmarshaller().toModel(loadFile("/sources/from_line_to_line.json"));
    assertThat(source.getLines().size(), is(15));
    assertThat(source.getLine(1), nullValue());
    assertThat(source.getLine(3), is(" * Copyright (C) 2008-2014 SonarSource"));
  }

}

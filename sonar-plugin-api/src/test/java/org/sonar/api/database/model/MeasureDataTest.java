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
package org.sonar.api.database.model;

import java.io.UnsupportedEncodingException;

import com.google.common.base.Charsets;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MeasureDataTest {
  @Test
  public void text_is_utf8() throws UnsupportedEncodingException {
    String s = "accents éà and special characters ç€";

    MeasureData data = new MeasureData();
    data.setData(s.getBytes(Charsets.UTF_8.name()));

    assertThat(data.getText()).isEqualTo(s);
  }
}

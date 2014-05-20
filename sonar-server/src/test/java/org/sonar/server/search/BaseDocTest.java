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
package org.sonar.server.search;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class BaseDocTest {

  @Test
  public void getField() throws Exception {
    Map<String, Object> fields = Maps.newHashMap();
    fields.put("a_string", "foo");
    fields.put("a_int", 42);
    fields.put("a_null", null);
    BaseDoc doc = new BaseDoc(fields) {
    };

    assertThat(doc.getField("a_string")).isEqualTo("foo");
    assertThat(doc.getField("a_int")).isEqualTo(42);
    assertThat(doc.getField("a_null")).isNull();
  }

  @Test
  public void getField_fails_if_missing_field() throws Exception {
    Map<String, Object> fields = Collections.emptyMap();
    BaseDoc doc = new BaseDoc(fields) {
    };

    try {
      doc.getField("a_string");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field a_string not specified in query options");
    }
  }
}

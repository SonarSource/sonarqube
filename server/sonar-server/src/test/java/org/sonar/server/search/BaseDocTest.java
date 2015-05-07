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
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class BaseDocTest {

  @Test
  public void getField() {
    Map<String, Object> fields = Maps.newHashMap();
    fields.put("a_string", "foo");
    fields.put("a_int", 42);
    fields.put("a_null", null);
    BaseDoc doc = new BaseDoc(fields) {
    };

    assertThat(doc.getNullableField("a_string")).isEqualTo("foo");
    assertThat(doc.getNullableField("a_int")).isEqualTo(42);
    assertThat(doc.getNullableField("a_null")).isNull();
  }

  @Test
  public void getField_fails_if_missing_field() {
    Map<String, Object> fields = Collections.emptyMap();
    BaseDoc doc = new BaseDoc(fields) {
    };

    try {
      doc.getNullableField("a_string");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field a_string not specified in query options");
    }
  }

  @Test
  public void getFieldAsDate() {
    BaseDoc doc = new BaseDoc(Maps.<String,Object>newHashMap()) {
    };
    long now = System.currentTimeMillis();
    doc.setField("javaDate", new Date(now));
    assertThat(doc.getFieldAsDate("javaDate").getTime()).isEqualTo(now);

    doc.setField("stringDate", IndexUtils.format(new Date(now)));
    assertThat(doc.getFieldAsDate("stringDate").getTime()).isEqualTo(now);
  }

  @Test
  public void getNullableFieldAsDate() {
    BaseDoc doc = new BaseDoc(Maps.<String,Object>newHashMap()) {
    };
    long now = System.currentTimeMillis();
    doc.setField("javaDate", new Date(now));
    assertThat(doc.getNullableFieldAsDate("javaDate").getTime()).isEqualTo(now);

    doc.setField("stringDate", IndexUtils.format(new Date(now)));
    assertThat(doc.getNullableFieldAsDate("stringDate").getTime()).isEqualTo(now);

    doc.setField("noValue", null);
    assertThat(doc.getNullableFieldAsDate("noValue")).isNull();
  }
}

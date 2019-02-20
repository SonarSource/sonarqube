/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.search;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class BaseDocTest {
  private final IndexType.IndexMainType someType = IndexType.main(Index.simple("bar"), "donut");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void getField() {
    Map<String, Object> fields = Maps.newHashMap();
    fields.put("a_string", "foo");
    fields.put("a_int", 42);
    fields.put("a_null", null);
    BaseDoc doc = new BaseDoc(someType, fields) {
      @Override
      public String getId() {
        return null;
      }

    };

    assertThat((String) doc.getNullableField("a_string")).isEqualTo("foo");
    assertThat((int) doc.getNullableField("a_int")).isEqualTo(42);
    assertThat((String) doc.getNullableField("a_null")).isNull();
  }

  @Test
  public void getField_fails_if_missing_field() {
    Map<String, Object> fields = Collections.emptyMap();
    BaseDoc doc = new BaseDoc(someType, fields) {
      @Override
      public String getId() {
        return null;
      }

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
    BaseDoc doc = new BaseDoc(someType, Maps.newHashMap()) {
      @Override
      public String getId() {
        return null;
      }

    };
    Date now = new Date();
    doc.setField("javaDate", now);
    assertThat(doc.getFieldAsDate("javaDate")).isEqualToIgnoringMillis(now);

    doc.setField("stringDate", EsUtils.formatDateTime(now));
    assertThat(doc.getFieldAsDate("stringDate")).isEqualToIgnoringMillis(now);
  }

  @Test
  public void getNullableFieldAsDate() {
    BaseDoc doc = new BaseDoc(someType, Maps.newHashMap()) {
      @Override
      public String getId() {
        return null;
      }

    };
    Date now = new Date();
    doc.setField("javaDate", now);
    assertThat(doc.getNullableFieldAsDate("javaDate")).isEqualToIgnoringMillis(now);

    doc.setField("stringDate", EsUtils.formatDateTime(now));
    assertThat(doc.getNullableFieldAsDate("stringDate")).isEqualToIgnoringMillis(now);

    doc.setField("noValue", null);
    assertThat(doc.getNullableFieldAsDate("noValue")).isNull();
  }

  @Test
  public void getFields_fails_with_ISE_if_setParent_has_not_been_called_on_IndexRelationType() {
    IndexType.IndexRelationType relationType = IndexType.relation(IndexType.main(Index.withRelations("foo"), "bar"), "donut");
    BaseDoc doc = new BaseDoc(relationType) {

      @Override
      public String getId() {
        throw new UnsupportedOperationException("getId not implemented");
      }

    };

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("parent must be set on a doc associated to a IndexRelationType (see BaseDoc#setParent(String))");

    doc.getFields();
  }

  @Test
  public void getFields_contains_join_field_and_indexType_field_when_setParent_has_been_called_on_IndexRelationType() {
    Index index = Index.withRelations("foo");
    IndexType.IndexRelationType relationType = IndexType.relation(IndexType.main(index, "bar"), "donut");
    BaseDoc doc = new BaseDoc(relationType) {
      {
        setParent("miam");
      }

      @Override
      public String getId() {
        throw new UnsupportedOperationException("getId not implemented");
      }

    };

    Map<String, Object> fields = doc.getFields();

    assertThat((Map) fields.get(index.getJoinField()))
      .isEqualTo(ImmutableMap.of("name", relationType.getName(), "parent", "miam"));
    assertThat(fields.get("indexType")).isEqualTo(relationType.getName());
  }
}

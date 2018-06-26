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
package org.sonar.server.es.textsearch;

import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;

import static org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.createQuery;
import static org.sonar.test.JsonAssert.assertJson;

public class ComponentTextSearchQueryFactoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query() {
    QueryBuilder result = createQuery(ComponentTextSearchQuery.builder()
      .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build(),
      ComponentTextSearchFeatureRepertoire.KEY);

    assertJson(result.toString()).isSimilarTo("{" +
      "  \"bool\" : {" +
      "    \"must\" : [{" +
      "      \"bool\" : {" +
      "        \"should\" : [{" +
      "          \"match\" : {" +
      "            \"key.sortable_analyzer\" : {" +
      "              \"query\" : \"SonarQube\"," +
      "              \"boost\" : 50.0\n" +
      "            }" +
      "          }" +
      "        }]" +
      "      }" +
      "    }]" +
      "  }" +
      "}");
  }

  @Test
  public void fail_to_create_query_when_no_feature() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("features cannot be empty");

    createQuery(ComponentTextSearchQuery.builder()
      .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build());
  }

  @Test
  public void fail_to_create_query_when_no_query_text() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("query text cannot be null");

    ComponentTextSearchQuery.builder().setFieldKey("key").setFieldName("name").build();
  }

  @Test
  public void fail_to_create_query_when_no_field_key() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("field key cannot be null");

    ComponentTextSearchQuery.builder().setQueryText("SonarQube").setFieldName("name").build();
  }

  @Test
  public void fail_to_create_query_when_no_field_name() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("field name cannot be null");

    ComponentTextSearchQuery.builder().setQueryText("SonarQube").setFieldKey("key").build();
  }
}

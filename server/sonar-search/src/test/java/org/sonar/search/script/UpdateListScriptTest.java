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
package org.sonar.search.script;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.script.ExecutableScript;
import org.junit.Before;
import org.junit.Test;
import org.sonar.process.ProcessProperties;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class UpdateListScriptTest {

  ListUpdate.UpdateListScriptFactory factory;

  @Before
  public void setUp() {
    factory = new ListUpdate.UpdateListScriptFactory();
  }

  @Test
  public void fail_missing_attributes_field() {
    Map<String, Object> params = new HashMap<>();

    // Missing everything
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing 'idField' parameter");
    }

    // Missing ID_VALUE
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD, "test");
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing 'idValue' parameter");
    }

    // Missing FIELD
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_VALUE, "test");
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing 'field' parameter");
    }

    // Has all required attributes and Null Value
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_FIELD, "test");
    ExecutableScript script = factory.newScript(params);
    assertThat(script).isNotNull();

    // Has all required attributes and VALUE of wrong type
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE, new Integer(52));
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {

    }

    // Has all required attributes and Proper VALUE
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE, ImmutableMap.of("key", "value"));
    script = factory.newScript(params);
    assertThat(script).isNotNull();

    // Missing ID_FIELD
    params.remove(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD);
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing 'idField' parameter");
    }
  }

  @Test
  public void update_list() {

    String listField = "listField";
    Collection<Map<String, Object>> mapFields;
    Map source = new HashMap<>();
    source.put("field1", "value1");

    // 0 Create list when field does not exists
    Map<String, Object> params = new HashMap<>();
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_FIELD, listField);
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD, "key");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_VALUE, "1");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE, mapOf("key", "1", "value", "A"));

    ExecutableScript script = factory.newScript(params);
    script.setNextVar("ctx", ImmutableMap.of("_source", source));
    script.run();

    mapFields = (Collection) source.get(listField);
    System.out.println("source = " + source);
    assertThat(mapFields).hasSize(1);

    // Add item to existing list
    params = new HashMap<>();
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_FIELD, listField);
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD, "key");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_VALUE, "2");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE, mapOf("key", "2", "value", "B"));
    script = factory.newScript(params);
    script.setNextVar("ctx", ImmutableMap.of("_source", source));
    script.run();
    mapFields = (Collection) source.get(listField);
    assertThat(mapFields).hasSize(2);

    // updated first item in list
    params = new HashMap<>();
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_FIELD, listField);
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD, "key");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_VALUE, "1");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE, mapOf("key", "1", "value", "a"));
    script = factory.newScript(params);
    script.setNextVar("ctx", ImmutableMap.of("_source", source));
    script.run();
    mapFields = (Collection) source.get(listField);
    assertThat(mapFields).hasSize(2);

    // updated second item in list
    params = new HashMap<>();
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_FIELD, listField);
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD, "key");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_VALUE, "2");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE, mapOf("key", "2", "value", "b"));
    script = factory.newScript(params);
    script.setNextVar("ctx", ImmutableMap.of("_source", source));
    script.run();
    mapFields = (Collection) source.get(listField);
    assertThat(mapFields).hasSize(2);

    // delete first item
    params = new HashMap<>();
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_FIELD, listField);
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD, "key");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_VALUE, "1");
    params.put(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE, null);
    script = factory.newScript(params);
    script.setNextVar("ctx", ImmutableMap.of("_source", source));
    script.run();
    mapFields = (Collection) source.get(listField);
    assertThat(mapFields).hasSize(1);
  }

  private Map<String, Object> mapOf(String k, String v, String k1, String v1) {
    Map<String, Object> map = new HashMap<>();
    map.put(k, v);
    map.put(k1, v1);
    return map;
  }
}

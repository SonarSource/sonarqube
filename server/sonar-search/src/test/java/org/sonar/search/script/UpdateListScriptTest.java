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

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.script.ExecutableScript;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class UpdateListScriptTest {

  ListUpdate.UpdateListScriptFactory factory;


  @Before
  public void setUp() throws Exception {
    factory = new ListUpdate.UpdateListScriptFactory();
  }

  @Test
  public void fail_missing_attributes_field() {
    Map<String, Object> params = new HashMap<String, Object>();

    // Missing everything
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing 'idField' parameter");
    }

    // Missing ID_VALUE
    params.put(ListUpdate.ID_FIELD, "test");
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing 'idValue' parameter");
    }

    // Missing FIELD
    params.put(ListUpdate.ID_VALUE, "test");
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing 'field' parameter");
    }

    // Has all required attributes and Null Value
    params.put(ListUpdate.FIELD, "test");
    ExecutableScript script = factory.newScript(params);
    assertThat(script).isNotNull();

    // Has all required attributes and VALUE of wrong type
    params.put(ListUpdate.VALUE, new Integer(52));
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {

    }

    // Has all required attributes and Proper VALUE
    params.put(ListUpdate.VALUE, ImmutableMap.of("key", "value"));
    script = factory.newScript(params);
    assertThat(script).isNotNull();

    // Missing ID_FIELD
    params.remove(ListUpdate.ID_FIELD);
    try {
      factory.newScript(params);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Missing 'idField' parameter");
    }
  }
}
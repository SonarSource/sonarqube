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

package org.sonar.server.properties;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ProjectSettingsTest {

  private ProjectSettings sut;

  private Settings settings;

  @Before
  public void before() throws Exception {
    this.settings = mock(Settings.class);
  }

  @Test
  public void call_global_settings_method_when_no_project_specific_settings() throws Exception {
    this.sut = new ProjectSettings(settings, Maps.<String, String>newHashMap());

    sut.getInt("anyKey");
    sut.getBoolean("anyKey");
    sut.getString("anyKey");

    verify(settings, times(1)).getBoolean(anyString());
    verify(settings, times(1)).getInt(anyString());
    verify(settings, times(1)).getString(anyString());
  }

  @Test(expected = NumberFormatException.class)
  public void getInt_property_throws_exception_when_value_is_not_formatted_correctly() throws Exception {
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("intKey", "wrongIntValue");
    this.sut = new ProjectSettings(settings, properties);

    sut.getInt("intKey");
  }

  @Test
  public void getInt_property_return_0_when_empty_property() throws Exception {
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("intKey", "");
    this.sut = new ProjectSettings(settings, properties);

    int value = sut.getInt("intKey");

    assertThat(value).isEqualTo(0);
  }

  @Test
  public void getInt_property_return_the_int_value() throws Exception {
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("intKey", "123");
    this.sut = new ProjectSettings(settings, properties);

    int value = sut.getInt("intKey");

    assertThat(value).isEqualTo(123);
  }

  @Test
  public void getString_returns_String_property() throws Exception {
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("stringKey", "stringValue");
    this.sut = new ProjectSettings(settings, properties);

    String value = sut.getString("stringKey");

    assertThat(value).isEqualTo("stringValue");
  }

  @Test
  public void getBoolean_returns_exception_when_value_is_not_formatted_correctly() throws Exception {
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("boolKey", "wronglyFormattedBoolean");
    this.sut = new ProjectSettings(settings, properties);

    boolean key = sut.getBoolean("boolKey");

    assertThat(key).isFalse();
  }

  @Test
  public void getBoolean_returns_false_when_value_is_empty() throws Exception {
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("boolKey", "");
    this.sut = new ProjectSettings(settings, properties);

    boolean key = sut.getBoolean("boolKey");

    assertThat(key).isFalse();
  }

  @Test
  public void getBoolean_returns_true_when_value_is_true_ignoring_case() throws Exception {
    HashMap<String, String> properties = Maps.newHashMap();
    properties.put("boolKey1", "true");
    properties.put("boolKey2", "True");
    this.sut = new ProjectSettings(settings, properties);

    boolean key1 = sut.getBoolean("boolKey1");
    boolean key2 = sut.getBoolean("boolKey2");

    assertThat(key1).isTrue();
    assertThat(key2).isTrue();
  }
}

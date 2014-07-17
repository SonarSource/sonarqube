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
package org.sonar.batch.protocol.input;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.StringReader;
import java.util.HashMap;

import static org.fest.assertions.Assertions.assertThat;

public class ProjectReferentialsTest {

  @Test
  public void testToJson() throws JSONException {
    ProjectReferentials ref = new ProjectReferentials();
    HashMap<String, String> projectSettings = new HashMap<String, String>();
    projectSettings.put("sonar.foo", "bar");
    ref.setProjectSettings("foo", projectSettings);

    JSONAssert.assertEquals("{languages: [], projectSettings: {foo: {'sonar.foo': 'bar'}}, timestamp: 0}", ref.toJson(), true);
  }

  @Test
  public void testFromJson() throws JSONException {
    ProjectReferentials ref = ProjectReferentials.fromJson(new StringReader("{languages: [], projectSettings: {foo: {'sonar.foo': 'bar'}}, timestamp: 1}"));

    assertThat(ref.timestamp()).isEqualTo(1);
  }
}

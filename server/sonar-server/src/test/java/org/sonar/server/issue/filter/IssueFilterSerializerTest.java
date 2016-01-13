/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.issue.filter;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.assertj.core.api.Assertions.assertThat;

public class IssueFilterSerializerTest {

  IssueFilterSerializer issueFilterSerializer = new IssueFilterSerializer();

  @Test
  public void should_serialize() {
    Map<String, Object> map = newLinkedHashMap();
    map.put("issues", newArrayList("ABCDE1234"));
    map.put("severities", newArrayList("MAJOR", "MINOR"));
    map.put("resolved", true);
    map.put("pageSize", 10l);
    map.put("pageIndex", 50);

    String result = issueFilterSerializer.serialize(map);

    assertThat(result).isEqualTo("issues=ABCDE1234|severities=MAJOR,MINOR|resolved=true|pageSize=10|pageIndex=50");
  }

  @Test
  public void should_remove_empty_value_when_serializing() {
    Map<String, Object> map = newLinkedHashMap();
    map.put("issues", newArrayList("ABCDE1234"));
    map.put("resolved", null);
    map.put("pageSize", "");

    String result = issueFilterSerializer.serialize(map);

    assertThat(result).isEqualTo("issues=ABCDE1234");
  }

  @Test
  public void should_deserialize() {
    String data = "issues=ABCDE1234|severities=MAJOR,MINOR|resolved=true|pageSize=10|pageIndex=50";

    Map<String, Object> map = issueFilterSerializer.deserialize(data);

    assertThat(map).hasSize(5);
    assertThat(map.get("issues")).isEqualTo("ABCDE1234");
    assertThat(map.get("severities")).isInstanceOf(List.class);
    assertThat((List<String>) map.get("severities")).contains("MAJOR", "MINOR");
    assertThat(map.get("resolved")).isEqualTo("true");
    assertThat(map.get("pageSize")).isEqualTo("10");
    assertThat(map.get("pageIndex")).isEqualTo("50");
  }

  @Test
  public void should_remove_empty_value_when_deserializing() {
    String data = "issues=ABCDE1234|severities=";

    Map<String, Object> map = issueFilterSerializer.deserialize(data);

    assertThat(map).hasSize(1);
    assertThat(map.get("issues")).isEqualTo("ABCDE1234");
  }

}

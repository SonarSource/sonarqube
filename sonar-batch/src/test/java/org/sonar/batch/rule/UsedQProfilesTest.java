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
package org.sonar.batch.rule;

import org.junit.Test;
import org.sonar.api.batch.rules.QProfile;

import java.util.Arrays;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class UsedQProfilesTest {

  @Test
  public void from_and_to_json() throws Exception {
    QProfile java = new QProfile("p1", "Sonar Way", "java");
    QProfile php = new QProfile("p2", "Sonar Way", "php");

    UsedQProfiles used = new UsedQProfiles().add(java).add(php);
    String json = "[{\"key\":\"p1\",\"language\":\"java\",\"name\":\"Sonar Way\"},{\"key\":\"p2\",\"language\":\"php\",\"name\":\"Sonar Way\"}]";
    assertThat(used.toJson()).isEqualTo(json);

    used = UsedQProfiles.fromJson(json);
    assertThat(used.profiles()).hasSize(2);
    assertThat(used.profiles().first().key()).isEqualTo("p1");
    assertThat(used.profiles().last().key()).isEqualTo("p2");
  }

  @Test
  public void do_not_duplicate_profiles() throws Exception {
    QProfile java = new QProfile("p1", "Sonar Way", "java");
    QProfile php = new QProfile("p2", "Sonar Way", "php");

    UsedQProfiles used = new UsedQProfiles().addAll(Arrays.asList(java, java, php));
    assertThat(used.profiles()).hasSize(2);
  }

  @Test
  public void group_profiles_by_key() throws Exception {
    QProfile java = new QProfile("p1", "Sonar Way", "java");
    QProfile php = new QProfile("p2", "Sonar Way", "php");

    UsedQProfiles used = new UsedQProfiles().addAll(Arrays.asList(java, java, php));
    Map<String, QProfile> map = used.profilesByKey();
    assertThat(map).hasSize(2);
    assertThat(map.get("p1")).isSameAs(java);
    assertThat(map.get("p2")).isSameAs(php);
  }
}

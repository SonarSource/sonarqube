/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.qualityprofile;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class QProfileKeyTest {

  @Test
  public void create_key(){
    QProfileKey key = QProfileKey.of("Sonar Way", "java");
    assertThat(key.name()).isEqualTo("Sonar Way");
    assertThat(key.language()).isEqualTo("java");
  }

  @Test
  public void fail_to_create_key_on_missing_name(){
    try {
      QProfileKey.of(null, "java");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Name must be set");
    }
  }

  @Test
  public void fail_to_create_key_on_missing_language(){
    try {
      QProfileKey.of("Sonar Way", null);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Language must be set");
    }
  }

  @Test
  public void create_from_string(){
    QProfileKey key = QProfileKey.parse("Sonar Way_java");
    assertThat(key.name()).isEqualTo("Sonar Way");
    assertThat(key.language()).isEqualTo("java");
  }

  @Test
  public void test_equals(){
    assertThat(QProfileKey.of("Sonar Way", "java")).isEqualTo(QProfileKey.of("Sonar Way", "java"));
    assertThat(QProfileKey.of("Sonar Way", "java")).isNotEqualTo(QProfileKey.of("Sonar Way", "js"));
    assertThat(QProfileKey.of("Default", "java")).isNotEqualTo(QProfileKey.of("Sonar Way", "java"));
  }

  @Test
  public void test_hashcode(){
    assertThat(QProfileKey.of("Sonar Way", "java").hashCode()).isEqualTo(QProfileKey.of("Sonar Way", "java").hashCode());
    assertThat(QProfileKey.of("Sonar Way", "java").hashCode()).isNotEqualTo(QProfileKey.of("Sonar Way", "js").hashCode());
    assertThat(QProfileKey.of("Default", "java").hashCode()).isNotEqualTo(QProfileKey.of("Sonar Way", "java").hashCode());
  }

  @Test
  public void test_to_string(){
    assertThat(QProfileKey.of("Sonar Way", "java").toString()).isEqualTo("Sonar Way_java");
  }

}

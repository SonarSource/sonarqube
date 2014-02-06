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

public class QProfileTest {

  @Test
  public void test_getters_and_setters() {
    QProfile profile = new QProfile().setId(1).setName("Default").setLanguage("java").setParent("Parent").setUsed(true).setVersion(1);

    assertThat(profile.id()).isEqualTo(1);
    assertThat(profile.name()).isEqualTo("Default");
    assertThat(profile.language()).isEqualTo("java");
    assertThat(profile.parent()).isEqualTo("Parent");
    assertThat(profile.used()).isTrue();
    assertThat(profile.version()).isEqualTo(1);
  }

  @Test
  public void to_string() throws Exception {
    assertThat(new QProfile().setId(1).setName("Default").setLanguage("java").setParent("Parent").setUsed(true).setVersion(1).toString())
      .contains("[id=1,name=Default,language=java,parent=Parent,version=1,used=true]");
  }

  @Test
  public void is_inherited() throws Exception {
    assertThat(new QProfile().setId(1).setName("Default").setLanguage("java").setParent("Parent").isInherited()).isTrue();
    assertThat(new QProfile().setId(1).setName("Default").setLanguage("java").setParent(null).isInherited()).isFalse();
  }
}

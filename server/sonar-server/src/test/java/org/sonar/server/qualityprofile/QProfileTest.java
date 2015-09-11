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

package org.sonar.server.qualityprofile;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QProfileTest {

  @Test
  public void test_getters_and_setters() {
    QProfile profile = new QProfile().setId(1).setName("Default").setLanguage("java").setParent("Parent");

    assertThat(profile.id()).isEqualTo(1);
    assertThat(profile.name()).isEqualTo("Default");
    assertThat(profile.language()).isEqualTo("java");
    assertThat(profile.parent()).isEqualTo("Parent");
  }

  @Test
  public void to_string() {
    assertThat(new QProfile().setId(1).setName("Default").setLanguage("java").setParent("Parent").toString())
      .contains("[id=1,key=<null>,name=Default,language=java,parent=Parent,isDefault=false,rulesUpdatedAt=<null>]");
  }

  @Test
  public void is_inherited() {
    assertThat(new QProfile().setId(1).setName("Default").setLanguage("java").setParent("Parent").isInherited()).isTrue();
    assertThat(new QProfile().setId(1).setName("Default").setLanguage("java").setParent(null).isInherited()).isFalse();
  }
}

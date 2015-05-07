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

public class QProfileNameTest {

  @Test
  public void equals_and_hashcode() {
    QProfileName xooP1 = new QProfileName("xoo", "p1");
    assertThat(xooP1).isEqualTo(xooP1);
    assertThat(xooP1).isNotEqualTo(new QProfileName("xoo", "p2"));
    assertThat(xooP1).isNotEqualTo("xxx");
    assertThat(xooP1).isNotEqualTo(null);

    // same name but different lang
    assertThat(xooP1).isNotEqualTo(new QProfileName("other_lang", "p1"));

    assertThat(xooP1.hashCode()).isEqualTo(xooP1.hashCode());
  }

  @Test
  public void to_string() {
    assertThat(new QProfileName("xoo", "p1").toString()).isEqualTo("{lang=xoo, name=p1}");
  }
}

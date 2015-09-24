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
package org.sonar.server.computation.log;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogFileRefTest {

  @Test
  public void equals_hashCode() {
    LogFileRef ref1 = new LogFileRef("UUID_1", "COMPONENT_1");
    LogFileRef ref1bis = new LogFileRef("UUID_1", "COMPONENT_1");
    LogFileRef ref2 = new LogFileRef("UUID_2", "COMPONENT_1");

    assertThat(ref1.equals(ref1)).isTrue();
    assertThat(ref1.equals(ref1bis)).isTrue();
    assertThat(ref1.equals(ref2)).isFalse();
    assertThat(ref1.equals(null)).isFalse();
    assertThat(ref1.equals("UUID_1")).isFalse();

    assertThat(ref1.hashCode()).isEqualTo(ref1bis.hashCode());
  }

  @Test
  public void getRelativePath() {
    assertThat(new LogFileRef("UUID_1", "COMPONENT_1").getRelativePath()).isEqualTo("COMPONENT_1/UUID_1.log");
    assertThat(new LogFileRef("UUID_1", null).getRelativePath()).isEqualTo("UUID_1.log");
  }
}

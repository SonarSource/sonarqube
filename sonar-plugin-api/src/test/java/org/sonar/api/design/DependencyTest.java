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
package org.sonar.api.design;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencyTest {

  @Test
  public void equalsAndHashCode() {
    DependencyDto dep1 = new DependencyDto().setFromSnapshotId(10).setToSnapshotId(30);
    DependencyDto dep1Clone = new DependencyDto().setFromSnapshotId(10).setToSnapshotId(30);
    DependencyDto dep2 = new DependencyDto().setFromSnapshotId(10).setToSnapshotId(31);

    assertThat(dep1.equals(dep2)).isFalse();
    assertThat(dep1.equals(dep1)).isTrue();
    assertThat(dep1.equals(dep1Clone)).isTrue();

    assertThat(dep1.hashCode()).isEqualTo(dep1.hashCode());
    assertThat(dep1.hashCode()).isEqualTo(dep1Clone.hashCode());
    assertThat(dep1.toString()).isEqualTo(dep1.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void weightCanNotBeNegative() {
    new DependencyDto().setWeight(-2);
  }
}

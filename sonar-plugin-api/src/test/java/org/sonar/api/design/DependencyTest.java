/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.design;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DependencyTest {

  @Test
  public void equalsAndHashCode() {
    DependencyDto dep1 = new DependencyDto().setFromSnapshotId(10).setToSnapshotId(30);
    DependencyDto dep1Clone = new DependencyDto().setFromSnapshotId(10).setToSnapshotId(30);
    DependencyDto dep2 = new DependencyDto().setFromSnapshotId(10).setToSnapshotId(31);

    assertFalse(dep1.equals(dep2));
    assertTrue(dep1.equals(dep1));
    assertTrue(dep1.equals(dep1Clone));

    assertEquals(dep1.hashCode(), dep1.hashCode());
    assertEquals(dep1.hashCode(), dep1Clone.hashCode());
    assertEquals(dep1.toString(), dep1.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void weightCanNotBeNegative() {
    new DependencyDto().setWeight(-2);
  }
}

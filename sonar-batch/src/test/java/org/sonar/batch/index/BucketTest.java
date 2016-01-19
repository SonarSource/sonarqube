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
package org.sonar.batch.index;

import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BucketTest {

  Directory directory = Directory.create("org/foo");
  File javaFile = File.create("org/foo/Bar.java");
  Metric ncloc = new Metric("ncloc");

  @Test
  public void shouldManageRelationships() {
    Bucket packageBucket = new Bucket(directory);
    Bucket fileBucket = new Bucket(javaFile);
    fileBucket.setParent(packageBucket);

    assertThat(fileBucket.getParent()).isEqualTo(packageBucket);
    assertThat(packageBucket.getChildren()).containsExactly(fileBucket);
  }

  @Test
  public void shouldBeEquals() {
    assertEquals(new Bucket(directory), new Bucket(directory));
    assertEquals(new Bucket(directory).hashCode(), new Bucket(directory).hashCode());
  }

  @Test
  public void shouldNotBeEquals() {
    assertFalse(new Bucket(directory).equals(new Bucket(javaFile)));
    assertThat(new Bucket(directory).hashCode()).isNotEqualTo(new Bucket(javaFile).hashCode());
  }
}

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
package org.sonar.batch.index;

import org.junit.Test;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class BucketTest {

  private JavaPackage javaPackage = new JavaPackage("org.foo");
  private JavaFile javaFile = new JavaFile("org.foo.Bar");
  private Metric ncloc = new Metric("ncloc");

  @Test
  public void shouldManageRelationships() {
    Bucket packageBucket = new Bucket(javaPackage);
    Bucket fileBucket = new Bucket(javaFile);
    fileBucket.setParent(packageBucket);

    assertThat(fileBucket.getParent(), is(packageBucket));
    assertThat(packageBucket.getChildren().size(), is(1));
    assertThat(packageBucket.getChildren(), hasItem(fileBucket));
  }

  @Test
  public void shouldCacheViolations() {
    Bucket fileBucket = new Bucket(javaFile);
    Violation violation = Violation.create(Rule.create("checkstyle", "rule1", "Rule one"), javaFile);
    fileBucket.addViolation(violation);
    assertThat(fileBucket.getViolations().size(), is(1));
    assertThat(fileBucket.getViolations(), hasItem(violation));
  }

  @Test
  public void shouldAddNewMeasure() {
    Bucket fileBucket = new Bucket(javaFile);
    Measure measure = new Measure(ncloc).setValue(1200.0);
    fileBucket.addMeasure(measure);

    assertThat(fileBucket.getMeasures(MeasuresFilters.all()).size(), is(1));
    assertThat(fileBucket.getMeasures(MeasuresFilters.metric(ncloc)), is(measure));
  }

  @Test
  public void shouldUpdateMeasure() {
    Bucket fileBucket = new Bucket(javaFile);
    Measure measure = new Measure(ncloc).setValue(1200.0);
    fileBucket.addMeasure(measure);

    assertThat(fileBucket.getMeasures(MeasuresFilters.all()).size(), is(1));
    assertThat(fileBucket.getMeasures(MeasuresFilters.metric(ncloc)).getValue(), is(1200.0));

    measure.setValue(500.0);
    fileBucket.addMeasure(measure);

    assertThat(fileBucket.getMeasures(MeasuresFilters.all()).size(), is(1));
    assertThat(fileBucket.getMeasures(MeasuresFilters.metric(ncloc)).getValue(), is(500.0));
  }

  @Test(expected = SonarException.class)
  public void shouldFailIfAddingSameMeasures() {
    Bucket fileBucket = new Bucket(javaFile);
    Measure measure = new Measure(ncloc).setValue(1200.0);
    fileBucket.addMeasure(measure);

    measure = new Measure(ncloc).setValue(500.0);
    fileBucket.addMeasure(measure);
  }

  @Test
  public void shouldBeEquals() {
    assertEquals(new Bucket(javaPackage), new Bucket(javaPackage));
    assertEquals(new Bucket(javaPackage).hashCode(), new Bucket(javaPackage).hashCode());
  }

  @Test
  public void shouldNotBeEquals() {
    assertFalse(new Bucket(javaPackage).equals(new Bucket(javaFile)));
    assertThat(new Bucket(javaPackage).hashCode(), not(is(new Bucket(javaFile).hashCode())));
  }
}

/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.surefire.data;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;

public class UnitTestIndexTest {

  @Test
  public void shouldIndexNewClassname() {
    UnitTestIndex index = new UnitTestIndex();

    UnitTestClassReport report = index.index("org.sonar.Foo");

    assertThat(report.getTests(), is(0L));
    assertThat(index.size(), is(1));
    assertSame(index.get("org.sonar.Foo"), report);
  }

  @Test
  public void shouldNotReIndex() {
    UnitTestIndex index = new UnitTestIndex();

    UnitTestClassReport report1 = index.index("org.sonar.Foo");
    UnitTestClassReport report2 = index.index("org.sonar.Foo");

    assertSame(report1, report2);
    assertThat(report1.getTests(), is(0L));
    assertThat(index.size(), is(1));
    assertSame(index.get("org.sonar.Foo"), report1);
  }

  @Test
  public void shouldRemoveClassname() {
    UnitTestIndex index = new UnitTestIndex();

    index.index("org.sonar.Foo");
    index.remove("org.sonar.Foo");

    assertThat(index.size(), is(0));
    assertThat(index.get("org.sonar.Foo"), nullValue());
  }

  @Test
  public void shouldMergeClasses() {
    UnitTestIndex index = new UnitTestIndex();
    UnitTestClassReport innerClass = index.index("org.sonar.Foo$Bar");
    innerClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR).setDurationMilliseconds(500L));
    innerClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_OK).setDurationMilliseconds(200L));
    UnitTestClassReport publicClass = index.index("org.sonar.Foo");
    publicClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR).setDurationMilliseconds(1000L));
    publicClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_FAILURE).setDurationMilliseconds(350L));

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size(), is(1));
    UnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getTests(), is(4L));
    assertThat(report.getFailures(), is(1L));
    assertThat(report.getErrors(), is(2L));
    assertThat(report.getSkipped(), is(0L));
    assertThat(report.getResults().size(), is(4));
    assertThat(report.getDurationMilliseconds(), is(500L + 200L + 1000L + 350L));
  }

  @Test
  public void shouldRenameClassWhenMergingToNewClass() {
    UnitTestIndex index = new UnitTestIndex();
    UnitTestClassReport innerClass = index.index("org.sonar.Foo$Bar");
    innerClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR).setDurationMilliseconds(500L));
    innerClass.add(new UnitTestResult().setStatus(UnitTestResult.STATUS_OK).setDurationMilliseconds(200L));

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size(), is(1));
    UnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getTests(), is(2L));
    assertThat(report.getFailures(), is(0L));
    assertThat(report.getErrors(), is(1L));
    assertThat(report.getSkipped(), is(0L));
    assertThat(report.getResults().size(), is(2));
    assertThat(report.getDurationMilliseconds(), is(500L + 200L));
  }

  @Test
  public void shouldNotFailWhenMergingUnknownClass() {
    UnitTestIndex index = new UnitTestIndex();

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size(), is(0));
  }
}

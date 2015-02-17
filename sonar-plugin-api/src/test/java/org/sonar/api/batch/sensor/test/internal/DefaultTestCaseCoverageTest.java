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
package org.sonar.api.batch.sensor.test.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTestCaseCoverageTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private InputFile mainFile = new DefaultInputFile("foo", "src/Foo.php").setType(InputFile.Type.MAIN);
  private InputFile testFile = new DefaultInputFile("foo", "test/FooTest.php").setType(InputFile.Type.TEST);

  @Test
  public void testCreation() throws Exception {
    DefaultTestCaseCoverage testCaseCoverage = new DefaultTestCaseCoverage()
      .testFile(testFile)
      .testName("myTest")
      .cover(mainFile)
      .onLines(Arrays.asList(1, 2, 3));

    assertThat(testCaseCoverage.testName()).isEqualTo("myTest");
    assertThat(testCaseCoverage.testFile()).isEqualTo(testFile);
    assertThat(testCaseCoverage.coveredFile()).isEqualTo(mainFile);
    assertThat(testCaseCoverage.coveredLines()).containsExactly(1, 2, 3);
  }

  @Test
  public void testEqualsHashCodeToString() {
    DefaultTestCaseCoverage testCaseCoverage1 = new DefaultTestCaseCoverage()
      .testFile(testFile)
      .testName("myTest")
      .cover(mainFile)
      .onLines(Arrays.asList(1, 2, 3));
    DefaultTestCaseCoverage testCaseCoverage1a = new DefaultTestCaseCoverage()
      .testFile(testFile)
      .testName("myTest")
      .cover(mainFile)
      .onLines(Arrays.asList(1, 2, 3));
    DefaultTestCaseCoverage testCaseCoverage2 = new DefaultTestCaseCoverage()
      .testFile(testFile)
      .testName("myTest2")
      .cover(mainFile)
      .onLines(Arrays.asList(1, 3, 3));

    assertThat(testCaseCoverage1).isEqualTo(testCaseCoverage1);
    assertThat(testCaseCoverage1).isEqualTo(testCaseCoverage1a);
    assertThat(testCaseCoverage1).isNotEqualTo(testCaseCoverage2);
    assertThat(testCaseCoverage1).isNotEqualTo(null);
    assertThat(testCaseCoverage1).isNotEqualTo("foo");

    assertThat(testCaseCoverage1.toString())
      .isEqualTo(
        "DefaultTestCaseCoverage[testFile=[moduleKey=foo, relative=test/FooTest.php, basedir=null],mainFile=[moduleKey=foo, relative=src/Foo.php, basedir=null],name=myTest,lines=[1, 2, 3]]");
    assertThat(testCaseCoverage1.hashCode()).isEqualTo(testCaseCoverage1a.hashCode());
  }
}

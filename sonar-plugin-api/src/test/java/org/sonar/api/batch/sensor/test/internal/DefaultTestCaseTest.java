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

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.test.TestCase.Status;
import org.sonar.api.batch.sensor.test.TestCase.Type;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultTestCaseTest {

  private InputFile parent = new DefaultInputFile("foo", "src/Foo.php");

  @Test
  public void testDefaultTestCaseTest() {
    DefaultTestCase testCase1 = new DefaultTestCase(parent, "myTest", 1L, Status.ERROR, "message", Type.UNIT, "stack");

    assertThat(testCase1.name()).isEqualTo("myTest");
    assertThat(testCase1.durationInMs()).isEqualTo(1L);
    assertThat(testCase1.status()).isEqualTo(Status.ERROR);
    assertThat(testCase1.message()).isEqualTo("message");
    assertThat(testCase1.type()).isEqualTo(Type.UNIT);
    assertThat(testCase1.stackTrace()).isEqualTo("stack");
  }

  @Test
  public void testEqualsHashCodeToString() {
    DefaultTestCase testCase1 = new DefaultTestCase(parent, "myTest", 1L, Status.ERROR, "message", Type.UNIT, "stack");
    DefaultTestCase testCase1a = new DefaultTestCase(parent, "myTest", 1L, Status.ERROR, "message", Type.UNIT, "stack");
    DefaultTestCase testCase2 = new DefaultTestCase(new DefaultInputFile("foo2", "src/Foo.php"), "myTest2", 2L, Status.FAILURE, "message2", Type.INTEGRATION, null);

    assertThat(testCase1).isEqualTo(testCase1);
    assertThat(testCase1).isEqualTo(testCase1a);
    assertThat(testCase1).isNotEqualTo(testCase2);
    assertThat(testCase1).isNotEqualTo(null);
    assertThat(testCase1).isNotEqualTo("foo");

    assertThat(testCase1.toString()).isEqualTo(
      "DefaultTestCase[file=[moduleKey=foo, relative=src/Foo.php, abs=null],name=myTest,duration=1,status=ERROR,message=message,type=UNIT,stackTrace=stack]");
    assertThat(testCase1.hashCode()).isEqualTo(testCase1a.hashCode());
  }

}

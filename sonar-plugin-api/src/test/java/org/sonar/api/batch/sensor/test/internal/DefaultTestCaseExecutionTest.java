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
import org.sonar.api.batch.sensor.test.TestCaseExecution.Status;
import org.sonar.api.batch.sensor.test.TestCaseExecution.Type;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTestCaseExecutionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private InputFile parent = new DefaultInputFile("foo", "src/Foo.php").setType(InputFile.Type.TEST);

  @Test
  public void testCreation() throws Exception {
    DefaultTestCaseExecution testCase = new DefaultTestCaseExecution(null)
      .inTestFile(parent)
      .name("myTest")
      .durationInMs(1)
      .message("message")
      .stackTrace("stack")
      .status(Status.ERROR)
      .ofType(Type.UNIT);

    assertThat(testCase.name()).isEqualTo("myTest");
    assertThat(testCase.testFile()).isEqualTo(parent);
    assertThat(testCase.durationInMs()).isEqualTo(1L);
    assertThat(testCase.message()).isEqualTo("message");
    assertThat(testCase.stackTrace()).isEqualTo("stack");
    assertThat(testCase.status()).isEqualTo(Status.ERROR);
    assertThat(testCase.type()).isEqualTo(Type.UNIT);
  }

  @Test
  public void testCreationWithDefaultValues() throws Exception {
    DefaultTestCaseExecution testCase = new DefaultTestCaseExecution(null)
      .inTestFile(parent)
      .name("myTest");

    assertThat(testCase.name()).isEqualTo("myTest");
    assertThat(testCase.testFile()).isEqualTo(parent);
    assertThat(testCase.durationInMs()).isNull();
    assertThat(testCase.message()).isNull();
    assertThat(testCase.stackTrace()).isNull();
    assertThat(testCase.status()).isEqualTo(Status.OK);
    assertThat(testCase.type()).isEqualTo(Type.UNIT);
  }

  @Test
  public void testInvalidDuration() throws Exception {
    DefaultTestCaseExecution builder = new DefaultTestCaseExecution(null)
      .inTestFile(parent)
      .name("myTest");

    thrown.expect(IllegalArgumentException.class);

    builder.durationInMs(-3);
  }

  @Test
  public void testEqualsHashCodeToString() {
    DefaultTestCaseExecution testCase1 = new DefaultTestCaseExecution(null)
      .inTestFile(parent)
      .name("myTest")
      .durationInMs(1)
      .message("message")
      .stackTrace("stack")
      .status(Status.ERROR)
      .ofType(Type.UNIT);
    DefaultTestCaseExecution testCase1a = new DefaultTestCaseExecution(null)
      .inTestFile(parent)
      .name("myTest")
      .durationInMs(1)
      .message("message")
      .stackTrace("stack")
      .status(Status.ERROR)
      .ofType(Type.UNIT);
    DefaultTestCaseExecution testCase2 = new DefaultTestCaseExecution(null)
      .inTestFile(new DefaultInputFile("foo2", "src/Foo.php").setType(InputFile.Type.TEST))
      .name("myTest2")
      .durationInMs(2)
      .message("message2")
      .stackTrace("null")
      .status(Status.FAILURE)
      .ofType(Type.INTEGRATION);

    assertThat(testCase1).isEqualTo(testCase1);
    assertThat(testCase1).isEqualTo(testCase1a);
    assertThat(testCase1).isNotEqualTo(testCase2);
    assertThat(testCase1).isNotEqualTo(null);
    assertThat(testCase1).isNotEqualTo("foo");

    assertThat(testCase1.toString()).isEqualTo(
      "DefaultTestCaseExecution[testFile=[moduleKey=foo, relative=src/Foo.php, basedir=null],name=myTest,duration=1,status=ERROR,message=message,type=UNIT,stackTrace=stack]");
    assertThat(testCase1.hashCode()).isEqualTo(testCase1a.hashCode());
  }

}

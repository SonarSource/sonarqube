/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarqube.monitoring.test.aspect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.aspectj.lang.JoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.notification.Failure;

import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runner.Description.createTestDescription;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestFailureAspectTest {

  private TestFailureAspect testFailureAspect;

  private Path fakeLogPath;

  @Before
  public void setup() throws IOException {
    Path tempDir = Files.createTempDirectory("TestFailureAspectTest");
    fakeLogPath = tempDir.resolve("fake-test-monitoring.log");
    testFailureAspect = new TestFailureAspect(fakeLogPath);
  }

  @Test
  public void afterFireTestFailure_shouldPersistMeasure() throws IOException {
    JoinPoint joinPoint = mock(JoinPoint.class);
    Failure failure = new Failure(
      createTestDescription("testClass", "testMethod"),
      new IllegalStateException("some exception"));
    when(joinPoint.getArgs()).thenReturn(new Object[]{failure});

    testFailureAspect.afterFireTestFailure(joinPoint);

    assertThat(getFileContent())
      .contains("\"timestamp\":\"" )
      .contains("\"testClass\":\"testClass\"")
      .contains("\"testMethod\":\"testMethod\"")
      .contains("\"exceptionClass\":\"java.lang.IllegalStateException\"")
      .contains("\"exceptionMessage\":\"some exception\"")
      .contains("\"exceptionLogs\":\"java.lang.IllegalStateException: some exception");
  }

  private String getFileContent() throws IOException {
    byte[] bytes = readAllBytes(fakeLogPath);
    return new String(bytes, StandardCharsets.UTF_8);
  }

}

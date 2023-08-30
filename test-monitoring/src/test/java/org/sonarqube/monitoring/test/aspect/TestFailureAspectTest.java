/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.nio.file.Path;
import java.nio.file.Paths;
import org.aspectj.lang.JoinPoint;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.notification.Failure;

import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.runner.Description.createTestDescription;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestFailureAspectTest {

  private TestFailureAspect testFailureAspect;

  private static final Path TMP_PATH = Path.of("/tmp");

  @BeforeClass
  public static void createTmpFolder() throws IOException {
    if (!exists(TMP_PATH)) {
      createDirectory(TMP_PATH);
    }
  }

  @Before
  public void setup() {
    testFailureAspect = new TestFailureAspect();
  }

  @Test
  public void afterFireTestFailure_shouldPersistMeasure() {
    JoinPoint joinPoint = mock(JoinPoint.class);
    Failure failure = new Failure(
      createTestDescription("testClass", "testMethod"),
      new IllegalStateException("some exception"));
    when(joinPoint.getArgs()).thenReturn(new Object[]{failure});

    testFailureAspect.afterFireTestFailure(joinPoint);

    String fileContent = getFileContent(Paths.get("/tmp/test-monitoring.log"));
    assertThat(fileContent)
      .contains("\"timestamp\":\"" )
      .contains("\"testClass\":\"testClass\"")
      .contains("\"testMethod\":\"testMethod\"")
      .contains("\"exceptionClass\":\"java.lang.IllegalStateException\"")
      .contains("\"exceptionMessage\":\"some exception\"")
      .contains("\"exceptionLogs\":\"java.lang.IllegalStateException: some exception");
  }

  private String getFileContent(Path path) {
    try {
      byte[] bytes = readAllBytes(path);
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (IOException e) {
      fail("Unable to read file " + path, e);
    }
    return null;
  }

}

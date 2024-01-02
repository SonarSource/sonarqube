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

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.sonarqube.monitoring.test.Measure;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonarqube.monitoring.test.Measure.MeasureBuilder.newMeasureBuilder;

@Aspect
public class TestFailureAspect {

  public static final String BRANCH_NAME = System.getenv("GITHUB_BRANCH");
  public static final String COMMIT_HASH = System.getenv("GIT_SHA1");
  public static final String BUILD_NUMBER = System.getenv("BUILD_NUMBER");
  public static final String QA_CATEGORY = System.getenv("QA_CATEGORY");

  private final Path path;
  private static final Gson GSON = new Gson();

  public TestFailureAspect() {
    this(Paths.get("/tmp/test-monitoring.log"));
  }

  public TestFailureAspect(Path path) {
    this.path = path;
    createEmptyLogFile();
  }

  private void createEmptyLogFile() {
    try {
      if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
        Files.createFile(path);
      }
      Files.write(path, "".getBytes(UTF_8));
    } catch (IOException e) {
      // Ignore
    }
  }

  @After("execution(public * org.junit.runner.notification.RunNotifier+.fireTestFailure(..))")
  public void afterFireTestFailure(JoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    if (args.length == 1) {
      Object arg = args[0];
      if (arg instanceof Failure) {
        Failure failure = (Failure) arg;
        persistMeasure(buildMeasure(failure));
      }
    }
  }

  private static Measure buildMeasure(Failure failure) {
    Throwable throwable = failure.getException();
    Description description = failure.getDescription();
    return newMeasureBuilder()
      .setTimestamp(LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      .setBranchName(BRANCH_NAME)
      .setCommit(COMMIT_HASH)
      .setBuild(BUILD_NUMBER)
      .setCategory(QA_CATEGORY)
      .setTestClass(description.getClassName())
      .setTestMethod(description.getMethodName())
      .setExceptionClass(throwable.getClass().getName())
      .setExceptionMessage(failure.getMessage())
      .setExceptionLogs(failure.getTrimmedTrace())
      .build();
  }

  private void persistMeasure(Measure measure) {
    try {
      Files.write(path, GSON.toJson(measure).getBytes(UTF_8), StandardOpenOption.APPEND);
      Files.write(path, "\n".getBytes(UTF_8), StandardOpenOption.APPEND);
    } catch (IOException e) {
      // Ignore
    }
  }
}

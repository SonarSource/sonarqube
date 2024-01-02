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
package org.sonarqube.ut.aspects;

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
import java.util.function.Consumer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AspectAssistant {
  public static final String COMMIT_HASH = System.getenv("GIT_SHA1");
  public static final String BUILD_NUMBER = System.getenv("BUILD_NUMBER");

  private static final Logger LOGGER = LoggerFactory.getLogger(AspectAssistant.class);
  private static final Path PATH = Paths.get("/tmp/ut-backend-monitoring.log");
  private static final Gson GSON = new Gson();

  static {
    try {
      if (!Files.exists(PATH, LinkOption.NOFOLLOW_LINKS)) {
        Files.createFile(PATH);
      }
    } catch (IOException e) {
      LOGGER.error("error creating log file");
    }
  }

  public static void persistMeasure(Measure measure) {
    try {
      Files.write(PATH, (GSON.toJson(measure) + "\n").getBytes(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      LOGGER.error("Error in persisting data of ut monitoring", e);
    }
  }

  public static Object measure(ProceedingJoinPoint jp, Consumer<Measure> populator) throws Throwable {
    long start = System.currentTimeMillis();
    try {
      Object proceed = jp.proceed();
      getMeasure(populator, start);
      return proceed;
    } catch (Throwable t) {
      getMeasure(populator, start);
      throw t;
    }
  }

  private static void getMeasure(Consumer<Measure> populator, long start) {
    long executionTime = System.currentTimeMillis() - start;

    if (executionTime > 0) {
      Measure measure = new Measure()
        .setDuration(executionTime)
        .setCommit(COMMIT_HASH)
        .setBuild(BUILD_NUMBER)
        .setTimestamp(LocalDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

      populator.accept(measure);
      AspectAssistant.persistMeasure(measure);
    }
  }
}

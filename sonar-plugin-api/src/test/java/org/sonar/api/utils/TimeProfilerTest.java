/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.WriterAppender;
import ch.qos.logback.core.layout.EchoLayout;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TimeProfilerTest {

  @Test
  public void testBasicProfiling() {
    StringWriter writer = new StringWriter();
    TimeProfiler profiler = new TimeProfiler(mockLogger(writer));

    profiler.start("Cycle analysis");
    assertThat(writer.toString(), containsString("[INFO] Cycle analysis..."));

    profiler.stop();
    assertThat(writer.toString(), containsString("[INFO] Cycle analysis done:"));
  }

  @Test
  public void stopOnce() throws IOException {
    StringWriter writer = new StringWriter();
    TimeProfiler profiler = new TimeProfiler(mockLogger(writer));

    profiler.start("Cycle analysis");
    profiler.stop();
    profiler.stop();
    profiler.stop();
    assertThat(StringUtils.countMatches(writer.toString(), "Cycle analysis done"), is(1));
  }

  @Test
  public void doNotLogNeverEndedTask() throws IOException {
    StringWriter writer = new StringWriter();
    TimeProfiler profiler = new TimeProfiler(mockLogger(writer));

    profiler.start("Cycle analysis");
    profiler.start("New task");
    profiler.stop();
    profiler.stop();
    assertThat(writer.toString(), not(containsString("Cycle analysis done")));
  }


  private static Logger mockLogger(Writer writer) {
    WriterAppender writerAppender = new WriterAppender();
    writerAppender.setLayout(new EchoLayout());
    writerAppender.setWriter(writer);
    writerAppender.setImmediateFlush(true);
    writerAppender.start();

    Logger logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(TimeProfilerTest.class);
    logger.addAppender(writerAppender);
    logger.setLevel(Level.INFO);
    logger.setAdditive(true);
    return logger;

  }
}

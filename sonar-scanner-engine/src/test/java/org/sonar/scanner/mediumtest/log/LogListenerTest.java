/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.mediumtest.log;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.bootstrapper.LogOutput;
import org.sonar.batch.bootstrapper.LogOutput.Level;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class LogListenerTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Pattern simpleTimePattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}");
  private List<LogEvent> logOutput;
  private StringBuilder logOutputStr;
  private ByteArrayOutputStream stdOutTarget;
  private ByteArrayOutputStream stdErrTarget;
  private static PrintStream savedStdOut;
  private static PrintStream savedStdErr;

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .setLogOutput(new SimpleLogListener());

  private File baseDir;

  private ImmutableMap.Builder<String, String> builder;

  @BeforeClass
  public static void backupStdStreams() {
    savedStdOut = System.out;
    savedStdErr = System.err;
  }

  @AfterClass
  public static void resumeStdStreams() {
    if (savedStdOut != null) {
      System.setOut(savedStdOut);
    }
    if (savedStdErr != null) {
      System.setErr(savedStdErr);
    }
  }

  @Before
  public void prepare() throws IOException {
    stdOutTarget = new ByteArrayOutputStream();
    stdErrTarget = new ByteArrayOutputStream();
    System.setOut(new PrintStream(stdOutTarget));
    System.setErr(new PrintStream(stdErrTarget));
    // logger from the batch might write to it asynchronously
    logOutput = Collections.synchronizedList(new LinkedList<LogEvent>());
    logOutputStr = new StringBuilder();

    baseDir = temp.getRoot();

    builder = ImmutableMap.<String, String>builder()
      .put("sonar.task", "scan")
      .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
      .put("sonar.projectKey", "com.foo.project")
      .put("sonar.projectName", "Foo Project")
      .put("sonar.projectVersion", "1.0-SNAPSHOT")
      .put("sonar.projectDescription", "Description of Foo Project");
  }

  private void assertNoStdOutput() {
    assertThat(new String(stdOutTarget.toByteArray())).isEmpty();
    assertThat(new String(stdErrTarget.toByteArray())).isEmpty();
  }

  /**
   *   Check that log message is not formatted, i.e. has no log level and timestamp.
   */
  private void assertMsgClean(String msg) {
    // FP: [JOURNAL_FLUSHER] WARNING Journal flush operation took 2,093ms last 8 cycles average is 262ms
    if (msg.contains("[JOURNAL_FLUSHER]")) {
      return;
    }

    for (LogOutput.Level l : LogOutput.Level.values()) {
      assertThat(msg).doesNotContain(l.toString());
    }

    Matcher matcher = simpleTimePattern.matcher(msg);
    assertThat(matcher.find()).isFalse();
  }

  @Test
  public void testChangeLogForAnalysis() throws IOException, InterruptedException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    tester.newTask()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.verbose", "true")
        .build())
      .execute();

    for (LogEvent e : logOutput) {
      savedStdOut.println("[captured]" + e.level + " " + e.msg);
    }

    // only done in DEBUG during analysis
    assertThat(logOutputStr.toString()).contains("Post-jobs : ");
  }

  @Test
  public void testNoStdLog() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    tester.newTask()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertNoStdOutput();
    assertThat(logOutput).isNotEmpty();

    synchronized (logOutput) {
      for (LogEvent e : logOutput) {
        savedStdOut.println("[captured]" + e.level + " " + e.msg);
      }
    }
  }

  @Test
  public void testNoFormattedMsgs() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    tester.newTask()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertNoStdOutput();

    synchronized (logOutput) {
      for (LogEvent e : logOutput) {
        assertMsgClean(e.msg);
        savedStdOut.println("[captured]" + e.level + " " + e.msg);
      }
    }
  }

  // SONAR-7540
  @Test
  public void testStackTrace() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");
    File xooFileMeasure = new File(srcDir, "sample.xoo.measures");
    FileUtils.write(xooFileMeasure, "foo:bar");

    try {
      tester.newTask()
        .properties(builder
          .put("sonar.sources", "src")
          .build())
        .execute();
      fail("Expected exception");
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("Error processing line 1");
    }

    assertNoStdOutput();

    synchronized (logOutput) {
      for (LogEvent e : logOutput) {
        if (e.level == Level.ERROR) {
          assertThat(e.msg).contains("Error processing line 1 of file", "src" + File.separator + "sample.xoo.measures",
            "java.lang.IllegalStateException: Unknow metric with key: foo",
            "at org.sonar.xoo.lang.MeasureSensor.saveMeasure");

        }
      }
    }
  }

  private class SimpleLogListener implements LogOutput {
    @Override
    public void log(String msg, Level level) {
      logOutput.add(new LogEvent(msg, level));
      logOutputStr.append(msg).append("\n");
    }
  }

  private static class LogEvent {
    String msg;
    LogOutput.Level level;

    LogEvent(String msg, LogOutput.Level level) {
      this.msg = msg;
      this.level = level;
    }
  }
}

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
package org.sonar.batch.util;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.Before;

public class ProgressReportTest {
  private static final String THREAD_NAME = "progress";
  private ProgressReport progressReport;

  @Before
  public void setUp() {
    progressReport = new ProgressReport(THREAD_NAME, 1000);
  }

  @Test
  public void die_on_stop() {
    progressReport.start("start");
    assertThat(isThreadAlive(THREAD_NAME)).isTrue();
    progressReport.stop("stop");
    assertThat(isThreadAlive(THREAD_NAME)).isFalse();
  }

  @Test
  public void do_not_block_app() {
    progressReport.start("start");
    assertThat(isDaemon(THREAD_NAME)).isTrue();
    progressReport.stop("stop");
  }

  private static boolean isDaemon(String name) {
    Thread t = getThread(name);
    return (t != null) && t.isDaemon();
  }

  private static boolean isThreadAlive(String name) {
    Thread t = getThread(name);
    return (t != null) && t.isAlive();
  }

  private static Thread getThread(String name) {
    Set<Thread> threads = Thread.getAllStackTraces().keySet();

    for (Thread t : threads) {
      if (t.getName().equals(name)) {
        return t;
      }
    }
    return null;
  }
}

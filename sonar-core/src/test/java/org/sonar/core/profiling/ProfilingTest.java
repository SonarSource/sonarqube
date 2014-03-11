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
package org.sonar.core.profiling;

import static org.mockito.Mockito.mock;

import org.slf4j.Logger;

import org.mockito.Mockito;
import org.sonar.core.profiling.Profiling.Level;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;

public class ProfilingTest {

  private Settings settings;
  private ProfilingLogFactory logFactory;
  private Logger logger;
  private Profiling profiling;

  private static final String BASIC_MESSAGE = "Basic message";
  private static final String FULL_MESSAGE = "Full message";

  @Before
  public void prepare() {
    settings = new Settings();
    logFactory = mock(ProfilingLogFactory.class);
    logger = mock(Logger.class);
    Mockito.when(logFactory.getLogger(Mockito.anyString())).thenReturn(logger);
    profiling = new Profiling(settings, logFactory);
  }

  @Test
  public void should_silence_all_profiling_by_default() throws Exception {
    doProfiling();
    Mockito.verifyZeroInteractions(logger);
  }

  @Test
  public void should_silence_all_profiling_when_faulty_config() throws Exception {
    settings.setProperty("sonar.log.profilingLevel", "POLOP");
    doProfiling();
    Mockito.verifyZeroInteractions(logger);
  }

  @Test
  public void should_silence_all_profiling() throws Exception {
    settings.setProperty("sonar.log.profilingLevel", "NONE");
    doProfiling();
    Mockito.verifyZeroInteractions(logger);
  }

  @Test
  public void should_log_basic_level() throws Exception {
    settings.setProperty("sonar.log.profilingLevel", "BASIC");
    doProfiling();
    Mockito.verify(logger).info(Mockito.eq("{}ms {}"), Mockito.anyLong(), Mockito.eq(BASIC_MESSAGE));
    Mockito.verifyNoMoreInteractions(logger);
  }

  @Test
  public void should_log_everything() throws Exception {
    settings.setProperty("sonar.log.profilingLevel", "FULL");
    doProfiling();
    Mockito.verify(logger).info(Mockito.eq("{}ms {}"), Mockito.anyLong(), Mockito.eq(FULL_MESSAGE));
    Mockito.verify(logger).info(Mockito.eq("{}ms {}"), Mockito.anyLong(), Mockito.eq(BASIC_MESSAGE));
  }

  private void doProfiling() throws InterruptedException {
    StopWatch basicWatch = profiling.start("basic", Level.BASIC);
    StopWatch fullWatch = profiling.start("full", Level.FULL);
    Thread.sleep(42);
    fullWatch.stop(FULL_MESSAGE);
    basicWatch.stop(BASIC_MESSAGE);
  }
  
}

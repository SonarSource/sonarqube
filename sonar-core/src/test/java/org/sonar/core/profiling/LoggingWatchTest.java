/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LoggingWatchTest {

  @Mock
  Logger logger;

  LoggingWatch loggingWatch;

  @Before
  public void setUp() throws Exception {
    loggingWatch = new LoggingWatch(logger);
  }

  @Test
  public void stop_with_params() throws Exception {
    loggingWatch.stop("Create '%s' elements of type '%s'", 10, "test");
    verify(logger).info(eq("{}ms {}"), anyLong(), eq("Create '10' elements of type 'test'"));
  }

  @Test
  public void stop_without_params() throws Exception {
    loggingWatch.stop("End of process");
    verify(logger).info(eq("{}ms {}"), anyLong(), eq("End of process"));
  }

  @Test
  public void stop_with_variable_but_without_params() throws Exception {
    loggingWatch.stop("End of process at %s");
    verify(logger).info(eq("{}ms {}"), anyLong(), eq("End of process at %s"));
  }
}

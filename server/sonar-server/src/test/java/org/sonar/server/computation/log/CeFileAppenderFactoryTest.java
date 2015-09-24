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
package org.sonar.server.computation.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.filter.Filter;
import java.io.File;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class CeFileAppenderFactoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createConfiguration() throws Exception {
    File logsDir = temp.newFolder();
    SiftingAppender siftingAppender = CeFileAppenderFactory.createConfiguration(new LoggerContext(), logsDir);

    // filter on CE logs
    List<Filter<ILoggingEvent>> filters = siftingAppender.getCopyOfAttachedFiltersList();
    assertThat(filters).hasSize(1);
    assertThat(filters.get(0)).isInstanceOf(CeLogFilter.class);
    assertThat(((CeLogFilter) filters.get(0)).isComputeEngine()).isTrue();

    assertThat(siftingAppender.getDiscriminator().getKey()).isEqualTo(CeFileAppenderFactory.MDC_LOG_PATH);
  }

  @Test
  public void buildAppender() throws Exception {
    File logsDir = temp.newFolder();
    CeFileAppenderFactory factory = new CeFileAppenderFactory(logsDir);

    FileAppender underTest = factory.buildAppender(new LoggerContext(), "uuid_1.log");

    assertThat(new File(underTest.getFile())).isEqualTo(new File(logsDir, "uuid_1.log"));

  }
}

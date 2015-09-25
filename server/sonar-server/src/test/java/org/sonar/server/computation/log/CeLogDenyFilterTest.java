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

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CeLogDenyFilterTest {

  private static final Object UNUSED = "";

  Filter underTest = new CeLogDenyFilter();

  @After
  public void tearDown() {
    MDC.clear();
  }

  @Test
  public void accept_non_ce_logs() {
    assertThat(underTest.decide(UNUSED)).isEqualTo(FilterReply.ACCEPT);
  }

  @Test
  public void deny_ce_logs() {
    MDC.put(CeLogging.MDC_LOG_PATH, "abc.log");
    assertThat(underTest.decide(UNUSED)).isEqualTo(FilterReply.DENY);
  }

}

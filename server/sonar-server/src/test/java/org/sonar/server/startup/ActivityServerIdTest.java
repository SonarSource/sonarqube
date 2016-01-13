/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.Logger;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

import static org.mockito.Mockito.*;

public class ActivityServerIdTest {

  @Mock
  private PropertiesDao dao;

  @Mock
  private Logger logger;

  private LogServerId logServerId;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    when(dao.selectGlobalProperty(CoreProperties.PERMANENT_SERVER_ID)).thenReturn(new PropertyDto().setValue("123456789"));
    when(dao.selectGlobalProperty(CoreProperties.ORGANISATION)).thenReturn(new PropertyDto().setValue("SonarSource"));
    when(dao.selectGlobalProperty(CoreProperties.SERVER_ID_IP_ADDRESS)).thenReturn(new PropertyDto().setValue("1.2.3.4"));

    logServerId = new LogServerId(dao);
  }

  @Test
  public void shouldLogMessage() {
    logServerId.logServerId(logger);

    String log =
      "Server information:\n"
        + "  - ID            : \"123456789\"\n"
        + "  - Organisation  : \"SonarSource\"\n"
        + "  - Registered IP : \"1.2.3.4\"\n";

    verify(logger, times(1)).info(log);
  }

  @Test
  public void shouldNotLogMessage() {
    when(dao.selectGlobalProperty(CoreProperties.PERMANENT_SERVER_ID)).thenReturn(null);

    logServerId.logServerId(logger);

    verify(logger, never()).info(anyString());
  }

  @Test
  public void testStartMethod() {
    // just to have 100% coverage ;-)
    logServerId.start();
  }

}

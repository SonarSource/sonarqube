/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.common;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.sonar.server.v2.config.MockConfigForControllers;
import org.sonar.server.v2.config.PlatformLevel4WebConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebAppConfiguration
@ContextConfiguration(
  classes = {PlatformLevel4WebConfig.class, MockConfigForControllers.class}
)
@RunWith(SpringRunner.class)
public abstract class ControllerIT {
  @Autowired
  protected WebApplicationContext webAppContext;

  protected MockMvc mockMvc;

  @Before
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
  }
}

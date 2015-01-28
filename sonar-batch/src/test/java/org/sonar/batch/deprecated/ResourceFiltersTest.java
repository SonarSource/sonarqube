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
package org.sonar.batch.deprecated;

import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.batch.ResourceFilter;

import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResourceFiltersTest {
  @Test
  public void warn_on_resource_filters() throws Exception {
    Logger logger = mock(Logger.class);
    ResourceFilter[] filters = {mock(ResourceFilter.class)};
    new ResourceFilters(logger, filters);
    verify(logger).warn(startsWith("ResourceFilters are not supported since version 4.2"));

    // verify that the standard constructor does not fail
    new ResourceFilters(filters);
  }

  @Test
  public void ok_if_no_resource_filters() throws Exception {
    // just for verify that it does not fail. Should check that no warning is logged.
    new ResourceFilters();
  }
}

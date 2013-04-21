/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.services;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DependencyQueryTest extends QueryTestCase {

  @Test
  public void createAllDependencies() {
    DependencyQuery query = DependencyQuery.createForResource("12");
    assertThat(query.getUrl(), is("/api/dependencies?resource=12&"));
    assertThat(query.getResourceIdOrKey(), is("12"));
    assertThat(query.getDirection(), nullValue());
  }

  @Test
  public void incomingDependencies() {
    DependencyQuery query = DependencyQuery.createForIncomingDependencies("12");
    assertThat(query.getUrl(), is("/api/dependencies?resource=12&dir=" + DependencyQuery.INCOMING_DIRECTION + "&"));
    assertThat(query.getResourceIdOrKey(), is("12"));
    assertThat(query.getDirection(), is(DependencyQuery.INCOMING_DIRECTION));
  }

  @Test
  public void outgoingDependencies() {
    DependencyQuery query = DependencyQuery.createForOutgoingDependencies("12");
    assertThat(query.getUrl(), is("/api/dependencies?resource=12&dir=" + DependencyQuery.OUTGOING_DIRECTION + "&"));
    assertThat(query.getResourceIdOrKey(), is("12"));
    assertThat(query.getDirection(), is(DependencyQuery.OUTGOING_DIRECTION));
  }
}

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
package org.sonar.wsclient.services;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SourceQueryTest extends QueryTestCase {

  @Test
  public void create() {
    assertThat(SourceQuery.create("myproject:org.foo.Bar").getUrl(), is("/api/sources?resource=myproject%3Aorg.foo.Bar&"));
    assertThat(SourceQuery.create("myproject:org.foo.Bar").getModelClass().getName(), is(Source.class.getName()));
  }

  @Test
  public void getOnlyAFewLines() {
    assertThat(SourceQuery.create("myproject:org.foo.Bar").setFromLineToLine(10, 30).getUrl(),
        is("/api/sources?resource=myproject%3Aorg.foo.Bar&from=10&to=30&"));
    assertThat(SourceQuery.create("myproject:org.foo.Bar").setLinesFromLine(10, 20).getUrl(),
        is("/api/sources?resource=myproject%3Aorg.foo.Bar&from=10&to=30&"));
  }
}

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
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EventUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void toModel() throws Exception {
    List<Event> events = new EventUnmarshaller().toModels(loadFile("/events/events.json"));
    Event event = events.get(0);
    assertThat(event.getId(), is("10"));
    assertThat(event.getName(), is("foo"));
    assertThat(event.getDescription(), is("desc"));
    assertThat(event.getCategory(), is("categ"));
    final Date expectedDate = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ssZZZZ").parse("2009-12-25T15:59:23+0000");
    assertThat(event.getDate(), is(expectedDate));
  }

}

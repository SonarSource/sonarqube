/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.unmarshallers;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sonar.wsclient.services.Event;

public class EventUnmarshallerTest {

  @Test
  public void toModel() throws Exception {
    List<Event> events = new EventUnmarshaller().toModels("[{\"id\": \"10\", \"n\": \"foo\", \"ds\": \"desc\", \"c\": \"categ\", \"dt\": \"2009-12-25T15:59:23+0000\"}]");
    Event event = events.get(0);
    assertThat(event.getId(), is("10"));
    assertThat(event.getName(), is("foo"));
    assertThat(event.getDescription(), is("desc"));
    assertThat(event.getCategory(), is("categ"));
     final Date expectedDate = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ssZZZZ").parse("2009-12-25T15:59:23+0000");
    assertThat(event.getDate(), is(expectedDate));
  }

}

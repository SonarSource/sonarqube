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

import java.util.Iterator;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MeasureTest {

  @Test
  public void getDataAsMap() {
    Measure measure = new Measure().setData("foo=1,bar=2,hello=3");
    Map<String,String> map = measure.getDataAsMap();
    assertThat(map.get("foo"), is("1"));
    assertThat(map.get("bar"), is("2"));
    assertThat(map.get("hello"), is("3"));
    assertThat(map.get("unknown"), nullValue());

    // sorted map
    Iterator<String> keys = map.keySet().iterator();
    assertThat(keys.next(), is("foo"));
    assertThat(keys.next(), is("bar"));
    assertThat(keys.next(), is("hello"));
    assertThat(keys.hasNext(), is(false));
  }
}

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
package org.sonar.batch.index;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.fest.assertions.Assertions.assertThat;

public class ComponentDataCacheTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  Caches caches;

  @Before
  public void start() throws Exception {
    caches = CachesTest.createCacheOnTemp(temp);
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void should_get_and_set_string_data() {
    ComponentDataCache cache = new ComponentDataCache(caches);
    cache.setStringData("org/struts/Action.java", "SYNTAX", "1:foo;3:bar");
    assertThat(cache.getStringData("org/struts/Action.java", "SYNTAX")).isEqualTo("1:foo;3:bar");
    assertThat(cache.getStringData("org/struts/Action.java", "OTHER")).isNull();
    assertThat(cache.getStringData("Other.java", "SYNTAX")).isNull();
    assertThat(cache.getStringData("Other.java", "OTHER")).isNull();
  }

  @Test
  public void should_get_and_set_data() {
    ComponentDataCache cache = new ComponentDataCache(caches);
    cache.setData("org/struts/Action.java", "COUNT", new LongData(1234L));
    LongData count = cache.getData("org/struts/Action.java", "COUNT");
    assertThat(count.data()).isEqualTo(1234L);
  }

  static class LongData implements Data {

    private long data;

    LongData() {
    }

    LongData(long data) {
      this.data = data;
    }

    public long data() {
      return data;
    }

    @Override
    public String writeString() {
      return String.valueOf(data);
    }

    @Override
    public void readString(String s) {
      data = Long.parseLong(s);
    }
  }
}

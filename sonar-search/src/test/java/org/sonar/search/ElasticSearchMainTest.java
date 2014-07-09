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
package org.sonar.search;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ElasticSearchMainTest {

  @Test
  public void fail_missing_arguments() {
    try {
      ElasticSearch.main(new String[]{""});
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(ElasticSearch.MISSING_ARGUMENTS);
    }
  }

  @Test
  public void fail_missing_name_arguments() {
    try {
      ElasticSearch.main(new String[]{"", "", ""});
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(ElasticSearch.MISSING_NAME_ARGUMENT);
    }
  }

  @Test
  public void fail_missing_port_arguments() {
    try {
      ElasticSearch.main(new String[]{"hello", "", ""});
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(ElasticSearch.MISSING_PORT_ARGUMENT);
    }
  }

  @Test
  public void fail_bad_port_arguments() {
    try {
      ElasticSearch.main(new String[]{"hello", "x0x0x0x0", ""});
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(ElasticSearch.COULD_NOT_PARSE_ARGUMENT_INTO_A_NUMBER);
    }
  }
}
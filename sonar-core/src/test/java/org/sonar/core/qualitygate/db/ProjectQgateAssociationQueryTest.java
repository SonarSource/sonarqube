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

package org.sonar.core.qualitygate.db;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProjectQgateAssociationQueryTest {

  @Test
  public void fail_on_null_login() throws Exception {
    ProjectQgateAssociationQuery.Builder builder = ProjectQgateAssociationQuery.builder();
    builder.gateId(null);

    try {
      builder.build();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NullPointerException.class).hasMessage("Gate ID cant be null.");
    }
  }

  @Test
  public void fail_on_invalid_membership() throws Exception {
    ProjectQgateAssociationQuery.Builder builder = ProjectQgateAssociationQuery.builder();
    builder.gateId("nelson");
    builder.membership("unknwown");

    try {
      builder.build();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Membership is not valid (got unknwown). Availables values are [all, selected, deselected]");
    }
  }

}

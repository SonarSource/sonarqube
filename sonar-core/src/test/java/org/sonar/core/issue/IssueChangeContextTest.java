/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.issue;

import java.util.Date;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueChangeContextTest {
  @Test
  public void test_scan_context() {
    Date now = new Date();
    IssueChangeContext context = IssueChangeContext.createScan(now);

    assertThat(context.scan()).isTrue();
    assertThat(context.userUuid()).isNull();
    assertThat(context.date()).isEqualTo(now);
  }

  @Test
  public void test_end_user_context() {
    Date now = new Date();
    IssueChangeContext context = IssueChangeContext.createUser(now, "user_uuid");

    assertThat(context.scan()).isFalse();
    assertThat(context.userUuid()).isEqualTo("user_uuid");
    assertThat(context.date()).isEqualTo(now);
  }
}

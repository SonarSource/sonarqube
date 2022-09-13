/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.Objects;
import javax.annotation.Nullable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByScanBuilder;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;

public class IssueChangeContextTest {

  private static final Date NOW = new Date();
  private static final String USER_UUID = "user_uuid";

  private IssueChangeContext context;

  @Test
  public void test_issueChangeContextByScanBuilder() {
    context = issueChangeContextByScanBuilder(NOW).build();

    verifyContext(null, true, false, false);
  }

  @Test
  public void test_issueChangeContextByUserBuilder() {
    context = issueChangeContextByUserBuilder(NOW, USER_UUID).build();

    verifyContext(USER_UUID, false, false, false);
  }

  @Test
  public void test_newBuilder() {
    context = IssueChangeContext.newBuilder()
      .withScan()
      .withRefreshMeasures()
      .setUserUuid(USER_UUID)
      .setDate(NOW)
      .withFromAlm()
      .build();

    verifyContext(USER_UUID, true, true, true);
  }

  @Test
  public void test_equal() {
    context = IssueChangeContext.newBuilder().setUserUuid(USER_UUID).setDate(NOW).build();
    IssueChangeContext equalContext = IssueChangeContext.newBuilder().setUserUuid(USER_UUID).setDate(NOW).build();
    IssueChangeContext notEqualContext = IssueChangeContext.newBuilder().setUserUuid("other_user_uuid").setDate(NOW).build();

    assertThat(context).isEqualTo(context)
      .isEqualTo(equalContext)
      .isNotEqualTo(notEqualContext)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object());
  }

  @Test
  public void test_hashCode() {
    context = IssueChangeContext.newBuilder().setUserUuid(USER_UUID).setDate(NOW).build();

    assertThat(context.hashCode()).isEqualTo(Objects.hash(USER_UUID, NOW, false, false, false));
  }

  @Test
  public void test_toString() {
    context = IssueChangeContext.newBuilder().setUserUuid(USER_UUID).setDate(NOW).build();
    String expected = "IssueChangeContext{userUuid='user_uuid', date=" + NOW + ", scan=false, refreshMeasures=false, fromAlm=false}";

    assertThat(context).hasToString(expected);
  }

  private void verifyContext(@Nullable String userUuid, boolean scan, boolean refreshMeasures, boolean fromAlm) {
    assertThat(context.userUuid()).isEqualTo(userUuid);
    assertThat(context.date()).isEqualTo(NOW);
    assertThat(context.scan()).isEqualTo(scan);
    assertThat(context.refreshMeasures()).isEqualTo(refreshMeasures);
    assertThat(context.fromAlm()).isEqualTo(fromAlm);
  }


}

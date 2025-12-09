/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
  private static final String EXTERNAL_USER = "toto@tata.com";
  private static final String WEBHOOK_SOURCE = "github";

  private IssueChangeContext context;

  @Test
  public void test_issueChangeContextByScanBuilder() {
    context = issueChangeContextByScanBuilder(NOW).build();

    verifyContext(true, false, null, null, null);
  }

  @Test
  public void test_issueChangeContextByUserBuilder() {
    context = issueChangeContextByUserBuilder(NOW, USER_UUID).build();

    verifyContext(false, false, USER_UUID, null, null);
  }

  @Test
  public void test_newBuilder() {
    context = IssueChangeContext.newBuilder()
      .withScan()
      .withRefreshMeasures()
      .setUserUuid(USER_UUID)
      .setDate(NOW)
      .setExternalUser(EXTERNAL_USER)
      .setWebhookSource(WEBHOOK_SOURCE)
      .build();

    verifyContext(true, true, USER_UUID, EXTERNAL_USER, WEBHOOK_SOURCE);
  }

  @Test
  public void test_equal() {
    context = IssueChangeContext.newBuilder()
      .setUserUuid(USER_UUID)
      .setDate(NOW)
      .setExternalUser(EXTERNAL_USER)
      .setWebhookSource(WEBHOOK_SOURCE)
      .build();
    IssueChangeContext equalContext = IssueChangeContext.newBuilder()
      .setUserUuid(USER_UUID)
      .setDate(NOW)
      .setExternalUser(EXTERNAL_USER)
      .setWebhookSource(WEBHOOK_SOURCE)
      .build();
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

    assertThat(context.hashCode()).isEqualTo(Objects.hash(USER_UUID, NOW, false, false, null, null));
  }

  private void verifyContext(boolean scan, boolean refreshMeasures, @Nullable String userUuid, @Nullable String externalUser,
    @Nullable String webhookSource) {
    assertThat(context.userUuid()).isEqualTo(userUuid);
    assertThat(context.date()).isEqualTo(NOW);
    assertThat(context.scan()).isEqualTo(scan);
    assertThat(context.refreshMeasures()).isEqualTo(refreshMeasures);
    assertThat(context.getExternalUser()).isEqualTo(externalUser);
    assertThat(context.getWebhookSource()).isEqualTo(webhookSource);
  }

}

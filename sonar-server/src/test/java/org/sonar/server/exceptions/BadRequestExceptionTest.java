/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.exceptions;

import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class BadRequestExceptionTest {

  @Test
  public void text_error() throws Exception {
    BadRequestException exception = BadRequestException.of("error");
    assertThat(exception.getMessage()).isEqualTo("error");
  }

  @Test
  public void text_error_with_list_of_errors() throws Exception {
    BadRequestException exception = BadRequestException.of("error", newArrayList(BadRequestException.Message.of("new error")));

    assertThat(exception.errors()).hasSize(1);
    assertThat(exception.errors().get(0).text()).isEqualTo("new error");
  }

  @Test
  public void text_error_with_list_of_l18n_errors() throws Exception {
    BadRequestException exception = BadRequestException.of("error", newArrayList(BadRequestException.Message.ofL10n("issue.error.123", "10")));

    assertThat(exception.errors()).hasSize(1);
    assertThat(exception.errors().get(0).l10nKey()).isEqualTo("issue.error.123");
    assertThat(exception.errors().get(0).l10nParams()).containsOnly("10");
  }

  @Test
  public void list_of_errors() throws Exception {
    BadRequestException exception = BadRequestException.of(newArrayList(BadRequestException.Message.of("new error")));

    assertThat(exception.errors()).hasSize(1);
    assertThat(exception.errors().get(0).text()).isEqualTo("new error");
  }

  @Test
  public void l10n_errors() throws Exception {
    BadRequestException exception = BadRequestException.ofL10n("issue.error.123", "10");
    assertThat(exception.getMessage()).isNull();
    assertThat(exception.l10nKey()).isEqualTo("issue.error.123");
    assertThat(exception.l10nParams()).containsOnly("10");
  }

  @Test
  public void l10n_errors_with_list_of_errors() throws Exception {
    BadRequestException exception = BadRequestException.ofL10n(newArrayList(BadRequestException.Message.of("new error")), "issue.error.123", "10");
    assertThat(exception.getMessage()).isNull();
    assertThat(exception.l10nKey()).isEqualTo("issue.error.123");
    assertThat(exception.l10nParams()).containsOnly("10");
    assertThat(exception.errors()).hasSize(1);
    assertThat(exception.errors().get(0).text()).isEqualTo("new error");
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    BadRequestException.Message msg = BadRequestException.Message.of("error1");
    BadRequestException.Message sameMsg = BadRequestException.Message.of("error1");
    BadRequestException.Message msg2 = BadRequestException.Message.of("error2");

    assertThat(msg.toString()).contains("error1");
    assertThat(msg).isEqualTo(sameMsg);
    assertThat(msg).isEqualTo(sameMsg);
    assertThat(msg.hashCode()).isEqualTo(msg.hashCode());
    assertThat(msg.hashCode()).isEqualTo(sameMsg.hashCode());

    assertThat(msg).isNotEqualTo(msg2);
  }

  @Test
  public void test_equals_and_hashcode_on_l10n() throws Exception {
    BadRequestException.Message msg = BadRequestException.Message.ofL10n("error.123", "10");
    BadRequestException.Message sameMsg = BadRequestException.Message.ofL10n("error.123", "10");
    BadRequestException.Message msg2 = BadRequestException.Message.ofL10n("error.123", "200");
    BadRequestException.Message msg3 = BadRequestException.Message.ofL10n("error.50");

    assertThat(msg.toString()).contains("error.123").contains("10");
    assertThat(msg).isEqualTo(msg);
    assertThat(msg).isEqualTo(sameMsg);
    assertThat(msg.hashCode()).isEqualTo(msg.hashCode());
    assertThat(msg.hashCode()).isEqualTo(sameMsg.hashCode());

    assertThat(msg).isNotEqualTo(msg2);
    assertThat(msg).isNotEqualTo(msg3);
    assertThat(msg).isNotEqualTo("issue.error.123");
  }

}

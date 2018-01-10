/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.issue;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultTest {
  @Test
  public void test_default_result() {
    Result<Object> result = Result.of();
    assertThat(result.ok()).isTrue();
    assertThat(result.errors()).isEmpty();
    assertThat(result.httpStatus()).isEqualTo(200);
    assertThat(result.get()).isNull();

    Object obj = new Object();
    result.set(obj);
    assertThat(result.get()).isSameAs(obj);
  }

  @Test
  public void test_error() {
    Result<Object> result = Result.of();
    result.addError("Something goes wrong");

    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).hasSize(1).contains(Result.Message.of("Something goes wrong"));
    assertThat(result.httpStatus()).isEqualTo(400);
    assertThat(result.get()).isNull();
  }

  @Test
  public void test_l10n_errors() {
    Result<Object> result = Result.of();
    Result.Message message = Result.Message.ofL10n("issue.error.123", "10");
    result.addError(message);

    assertThat(result.ok()).isFalse();
    assertThat(result.errors()).hasSize(1).containsOnly(message);

    message = result.errors().get(0);
    assertThat(message.text()).isNull();
    assertThat(message.l10nKey()).isEqualTo("issue.error.123");
    assertThat(message.l10nParams()).hasSize(1);
    assertThat(message.l10nParams()[0]).isEqualTo("10");
  }

  @Test
  public void test_text_message() {
    Result.Message txtMessage = Result.Message.of("the error");
    Result.Message sameMessage = Result.Message.of("the error");
    Result.Message otherMessage = Result.Message.of("other");

    assertThat(txtMessage.toString()).contains("the error");
    assertThat(txtMessage).isEqualTo(txtMessage);
    assertThat(txtMessage).isEqualTo(sameMessage);
    assertThat(txtMessage.hashCode()).isEqualTo(txtMessage.hashCode());
    assertThat(txtMessage.hashCode()).isEqualTo(sameMessage.hashCode());
    assertThat(txtMessage).isNotEqualTo(otherMessage);
    assertThat(txtMessage).isNotEqualTo("the error");
  }

  @Test
  public void test_l10n_message() {
    Result.Message msg = Result.Message.ofL10n("issue.error.123", "10");
    Result.Message sameMsg = Result.Message.ofL10n("issue.error.123", "10");
    Result.Message msg2 = Result.Message.ofL10n("issue.error.123", "200");
    Result.Message msg3 = Result.Message.ofL10n("issue.error.50");

    assertThat(msg.toString()).contains("issue.error.123").contains("10");
    assertThat(msg).isEqualTo(msg);
    assertThat(msg).isEqualTo(sameMsg);
    assertThat(msg.hashCode()).isEqualTo(msg.hashCode());
    assertThat(msg.hashCode()).isEqualTo(sameMsg.hashCode());

    assertThat(msg).isNotEqualTo(msg2);
    assertThat(msg).isNotEqualTo(msg3);
    assertThat(msg).isNotEqualTo("issue.error.123");
  }
}

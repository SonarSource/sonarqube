/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
  public void test_toString() {
    String errorMessage = "the error";
    Result.Message txtMsg = Result.Message.of(errorMessage);
    assertThat(txtMsg.toString()).contains(errorMessage);
    assertThat(txtMsg.toString()).isNotEqualTo(errorMessage);

    Result.Message msg = Result.Message.ofL10n("issue.error.123", "10");
    assertThat(msg.toString()).contains("issue.error.123").contains("10");
    assertThat(msg.toString()).isNotEqualTo("issue.error.123");
  }

  @Test
  public void test_equals_and_hashCode() {
    String errorMessage = "the error";
    Result.Message txtMsg = Result.Message.of(errorMessage);
    Result.Message sameTxtMsg = Result.Message.of(errorMessage);
    Result.Message otherTxtMessage = Result.Message.of("other");

    assertThat(txtMsg)
      .isEqualTo(txtMsg)
      .isEqualTo(sameTxtMsg)
      .isNotEqualTo(otherTxtMessage);

    Result.Message msg = Result.Message.ofL10n("issue.error.123", "10");
    Result.Message sameMsg = Result.Message.ofL10n("issue.error.123", "10");
    Result.Message otherMsg1 = Result.Message.ofL10n("issue.error.123", "200");
    Result.Message otherMsg2 = Result.Message.ofL10n("issue.error.50");

    assertThat(msg)
      .isEqualTo(msg)
      .isEqualTo(sameMsg)
      .isNotEqualTo(otherMsg1)
      .isNotEqualTo(otherMsg2)
      .hasSameHashCodeAs(msg)
      .hasSameHashCodeAs(sameMsg);
    assertThat(msg.hashCode()).isNotEqualTo(otherMsg1.hashCode());
  }
}

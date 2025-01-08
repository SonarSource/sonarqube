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
package org.sonar.server.usertoken;

import org.junit.Test;
import org.sonar.db.user.TokenType;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenGeneratorImplTest {
  private final TokenGeneratorImpl underTest = new TokenGeneratorImpl();

  @Test
  public void generate_different_tokens() {
    // this test is not enough to ensure that generated strings are unique,
    // but it still does a simple and stupid verification
    String firstToken = underTest.generate(TokenType.USER_TOKEN);
    String secondToken = underTest.generate(TokenType.USER_TOKEN);

    assertThat(firstToken)
      .isNotEqualTo(secondToken)
      .hasSize(44);
  }

  @Test
  public void generated_userToken_should_have_squ_prefix() {
    String token = underTest.generate(TokenType.USER_TOKEN);

    assertThat(token).matches("squ_.{40}");
  }

  @Test
  public void generated_projectAnalysisToken_should_have_sqp_prefix() {
    String token = underTest.generate(TokenType.PROJECT_ANALYSIS_TOKEN);

    assertThat(token).matches("sqp_.{40}");
  }

  @Test
  public void generated_globalAnalysisToken_should_have_sqa_prefix() {
    String token = underTest.generate(TokenType.GLOBAL_ANALYSIS_TOKEN);

    assertThat(token).matches("sqa_.{40}");
  }

  @Test
  public void generateProjectBadgeToken_nullToken_shouldNotHavePrefix() {
    String token = underTest.generate(TokenType.PROJECT_BADGE_TOKEN);

    assertThat(token).matches("sqb_.{40}");
  }

  @Test
  public void token_does_not_contain_colon() {
    assertThat(underTest.generate(TokenType.USER_TOKEN)).doesNotContain(":");
  }

  @Test
  public void hash_token() {
    String hash = underTest.hash("1234567890123456789012345678901234567890");

    assertThat(hash)
      .hasSize(96)
      .isEqualTo("b2501fc3833ae6feba7dc8a973a22d709b7c796ee97cbf66db2c22df873a9fa147b1b630878f771457b7769efd9ffa0d")
      .matches("[0-9a-f]+");
  }
}

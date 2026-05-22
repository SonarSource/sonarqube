/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class SetPrivatePortfoliosFromPrivateComponentsTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(SetPrivatePortfoliosFromPrivateComponents.class);

  private final SetPrivatePortfoliosFromPrivateComponents underTest = new SetPrivatePortfoliosFromPrivateComponents(db.database());

  @ParameterizedTest
  @CsvSource({
    "true, false, true, VW",
    "false, true, true, VW",
    "false, false, false, VW",
    "true, true, true, VW",
    "true, false, true, SVW",
    "false, true, true, SVW",
    "false, false, false, SVW",
    "true, true, true, SVW"
  })
  void execute_sets_portfolio_privacy_when_matching_component_exists(boolean componentPrivate, boolean portfolioPrivate,
    boolean expectedPortfolioPrivate, String qualifier) throws SQLException {
    insertComponent("portfolio-uuid", componentPrivate, qualifier, "PRJ");
    insertPortfolio("portfolio-uuid", portfolioPrivate);

    underTest.execute();

    assertThat(selectPortfolioPrivacy("portfolio-uuid")).isEqualTo(expectedPortfolioPrivate);
  }

  @ParameterizedTest
  @CsvSource({
    "true, false, true, VW",
    "false, true, true, VW",
    "false, false, false, VW",
    "true, true, true, VW",
    "true, false, true, SVW",
    "false, true, true, SVW",
    "false, false, false, SVW",
    "true, true, true, SVW"
  })
  void execute_sets_component_privacy_when_matching_portfolio_exists(boolean componentPrivate, boolean portfolioPrivate,
    boolean expectedComponentPrivate, String qualifier) throws SQLException {
    insertComponent("portfolio-uuid", componentPrivate, qualifier, "PRJ");
    insertPortfolio("portfolio-uuid", portfolioPrivate);

    underTest.execute();

    assertThat(selectComponentPrivacy("portfolio-uuid")).isEqualTo(expectedComponentPrivate);
  }

  @Test
  void execute_keeps_public_portfolio_public_when_no_matching_component_exists() throws SQLException {
    insertPortfolio("portfolio-uuid", false);

    underTest.execute();

    assertThat(selectPortfolioPrivacy("portfolio-uuid")).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
    "APP, PRJ",
    "TRK, PRJ",
    "VW, FIL",
    "SVW, FIL"
  })
  void execute_keeps_public_portfolio_public_when_matching_private_component_is_not_a_portfolio(String qualifier, String scope) throws SQLException {
    insertComponent("portfolio-uuid", true, qualifier, scope);
    insertPortfolio("portfolio-uuid", false);

    underTest.execute();

    assertThat(selectPortfolioPrivacy("portfolio-uuid")).isFalse();
  }

  @Test
  void execute_is_reentrant() throws SQLException {
    insertComponent("portfolio-uuid", true);
    insertPortfolio("portfolio-uuid", false);

    underTest.execute();
    underTest.execute();

    assertThat(selectPortfolioPrivacy("portfolio-uuid")).isTrue();
  }

  @Test
  void execute_only_updates_portfolio_with_matching_private_component() throws SQLException {
    insertComponent("private-component-uuid", true);
    insertPortfolio("private-component-uuid", false);

    insertComponent("public-component-uuid", false);
    insertPortfolio("public-component-uuid", false);

    underTest.execute();

    assertThat(selectPortfolioPrivacy("private-component-uuid")).isTrue();
    assertThat(selectPortfolioPrivacy("public-component-uuid")).isFalse();
  }

  private boolean selectPortfolioPrivacy(String uuid) {
    var rows = db.select("SELECT private FROM portfolios WHERE uuid = '" + uuid + "'");
    assertThat(rows).hasSize(1);
    return (boolean) rows.getFirst().get("PRIVATE");
  }

  private boolean selectComponentPrivacy(String uuid) {
    var rows = db.select("SELECT private FROM components WHERE uuid = '" + uuid + "'");
    assertThat(rows).hasSize(1);
    return (boolean) rows.getFirst().get("PRIVATE");
  }

  private void insertComponent(String uuid, boolean isPrivate) {
    insertComponent(uuid, isPrivate, "VW", "PRJ");
  }

  private void insertComponent(String uuid, boolean isPrivate, String qualifier, String scope) {
    db.executeInsert("components",
      "uuid", uuid,
      "kee", uuid + "-key",
      "enabled", true,
      "private", isPrivate,
      "qualifier", qualifier,
      "scope", scope,
      "uuid_path", uuid + ".",
      "branch_uuid", uuid);
  }

  private void insertPortfolio(String uuid, boolean isPrivate) {
    db.executeInsert("portfolios",
      "uuid", uuid,
      "kee", uuid + "-key",
      "name", uuid + "-name",
      "root_uuid", uuid,
      "private", isPrivate,
      "selection_mode", "MANUAL",
      "created_at", 1_000_000L,
      "updated_at", 1_000_000L);
  }
}

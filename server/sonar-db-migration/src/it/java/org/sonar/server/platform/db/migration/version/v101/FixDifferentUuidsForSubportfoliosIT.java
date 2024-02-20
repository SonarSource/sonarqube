/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.MigrationDbTester;

import static java.util.stream.Collectors.toSet;

class FixDifferentUuidsForSubportfoliosIT {
  private static final String OLD_UUID = "differentSubPfUuid";
  private static final String SUB_PF_KEY = "subPfKey";
  private static final String NEW_SUBPF_UUID = "subPfUuid";
  private static final String PF_UUID = "pfUuid";
  private static final String NEW_CHILD_SUBPF_UUID = "childsubpfUuid";
  private static final String OLD_CHILD_SUBPF_UUID = "old_child_subpf_uuid";
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(FixDifferentUuidsForSubportfolios.class);
  private final FixDifferentUuidsForSubportfolios underTest = new FixDifferentUuidsForSubportfolios(db.database());

  @Test
  void execute_shouldUpdatePortfoliosAndPortfolioProjectsAndPortfolioReferenceTable() throws SQLException {
    insertPortfolio("pfKey", PF_UUID);
    insertComponent(SUB_PF_KEY, NEW_SUBPF_UUID, PF_UUID, Qualifiers.SUBVIEW);
    insertSubPortfolio(SUB_PF_KEY, PF_UUID, PF_UUID, OLD_UUID);
    insertPortfolioProject("projUuid", OLD_UUID);
    insertPortfolioReference("refUuid", OLD_UUID);

    underTest.execute();

    Assertions.assertThat(findValueIn("portfolios", "uuid")).containsExactlyInAnyOrder(PF_UUID, NEW_SUBPF_UUID);
    Assertions.assertThat(findValueIn("portfolio_projects", "portfolio_uuid")).containsExactlyInAnyOrder(NEW_SUBPF_UUID);
    Assertions.assertThat(findValueIn("portfolio_references", "portfolio_uuid")).containsExactlyInAnyOrder(NEW_SUBPF_UUID);
  }

  @Test
  void execute_shouldBeRentrant() throws SQLException {
    insertPortfolio("pfKey", PF_UUID);
    insertComponent(SUB_PF_KEY, NEW_SUBPF_UUID, PF_UUID, Qualifiers.SUBVIEW);
    insertSubPortfolio(SUB_PF_KEY, PF_UUID, PF_UUID, OLD_UUID);
    insertPortfolioProject("projUuid", OLD_UUID);
    insertPortfolioReference("refUuid", OLD_UUID);

    underTest.execute();
    underTest.execute();

    Assertions.assertThat(findValueIn("portfolios", "uuid")).containsExactlyInAnyOrder(PF_UUID, NEW_SUBPF_UUID);
    Assertions.assertThat(findValueIn("portfolio_projects", "portfolio_uuid")).containsExactlyInAnyOrder(NEW_SUBPF_UUID);
    Assertions.assertThat(findValueIn("portfolio_references", "portfolio_uuid")).containsExactlyInAnyOrder(NEW_SUBPF_UUID);
  }

  @Test
  void execute_shouldFixUuidForSubPortfolioAtDifferentLevels() throws SQLException {
    insertPortfolio("pfKey", PF_UUID);

    insertComponent(SUB_PF_KEY, NEW_SUBPF_UUID, PF_UUID, Qualifiers.SUBVIEW);
    insertComponent("child_subpfkey", NEW_CHILD_SUBPF_UUID, PF_UUID, Qualifiers.SUBVIEW);

    insertSubPortfolio(SUB_PF_KEY, PF_UUID, PF_UUID, OLD_UUID);
    insertSubPortfolio("child_subpfkey", OLD_UUID, PF_UUID, OLD_CHILD_SUBPF_UUID);
    insertPortfolioProject("projUuid", OLD_CHILD_SUBPF_UUID);
    insertPortfolioReference("refUuid", OLD_CHILD_SUBPF_UUID);

    underTest.execute();

    Assertions.assertThat(findValueIn("portfolios", "uuid")).containsExactlyInAnyOrder(PF_UUID, NEW_SUBPF_UUID, NEW_CHILD_SUBPF_UUID);
    Assertions.assertThat(findValueIn("portfolios", "parent_uuid")).containsExactlyInAnyOrder(null, PF_UUID, NEW_SUBPF_UUID);
    Assertions.assertThat(findValueIn("portfolio_projects", "portfolio_uuid")).containsExactlyInAnyOrder(NEW_CHILD_SUBPF_UUID);
    Assertions.assertThat(findValueIn("portfolio_references", "portfolio_uuid")).containsExactlyInAnyOrder(NEW_CHILD_SUBPF_UUID);
  }

  private Set<String> findValueIn(String table, String field) {
    return db.select(String.format("select %s FROM %s", field, table))
      .stream()
      .map(row -> (String) row.get(field))
      .collect(toSet());
  }


  private String insertComponent(String key, String uuid, String branchUuid, String qualifier) {
    Map<String, Object> map = new HashMap<>();
    map.put("UUID", uuid);
    map.put("KEE", key);
    map.put("BRANCH_UUID", branchUuid);
    map.put("UUID_PATH", "." + uuid + ".");
    map.put("QUALIFIER", qualifier);
    map.put("ENABLED", true);
    map.put("PRIVATE", true);

    db.executeInsert("components", map);
    return uuid;
  }

  private String insertPortfolio(String kee, String uuid) {
    return insertSubPortfolio(kee, uuid, uuid);
  }

  private String insertSubPortfolio(String kee, String rootUuid, String uuid) {
    return insertSubPortfolio(kee, null, rootUuid, uuid);
  }

  private String insertSubPortfolio(String kee, @Nullable String parentUuid, String rootUuid, String uuid) {
    Map<String, Object> map = new HashMap<>();
    map.put("UUID", uuid);
    map.put("KEE", kee);
    map.put("NAME", uuid);
    map.put("ROOT_UUID", rootUuid);
    map.put("PRIVATE", false);
    map.put("SELECTION_MODE", "MANUAL");
    map.put("PARENT_UUID", parentUuid);
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("UPDATED_AT", System.currentTimeMillis());


    db.executeInsert("portfolios", map);
    return uuid;
  }

  private String insertPortfolioReference(String uuid, String portfolioUuid) {
    Map<String, Object> map = new HashMap<>();
    map.put("UUID", uuid);
    map.put("PORTFOLIO_UUID", portfolioUuid);
    map.put("REFERENCE_UUID", "reference");
    map.put("CREATED_AT", System.currentTimeMillis());

    db.executeInsert("portfolio_references", map);
    return uuid;
  }

  private String insertPortfolioProject(String uuid, String portfolioUuid) {
    Map<String, Object> map = new HashMap<>();
    map.put("UUID", uuid);
    map.put("PORTFOLIO_UUID", portfolioUuid);
    map.put("PROJECT_UUID", portfolioUuid);
    map.put("CREATED_AT", System.currentTimeMillis());

    db.executeInsert("portfolio_projects", map);
    return uuid;
  }
}

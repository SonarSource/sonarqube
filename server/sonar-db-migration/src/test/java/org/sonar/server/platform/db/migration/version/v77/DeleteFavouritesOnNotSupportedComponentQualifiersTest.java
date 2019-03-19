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
package org.sonar.server.platform.db.migration.version.v77;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteFavouritesOnNotSupportedComponentQualifiersTest {

  private static final String TABLE = "properties";
  private static final String FAVOURITE_PROPERTY = "favourite";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteFavouritesOnNotSupportedComponentQualifiersTest.class, "schema.sql");

  private DeleteFavouritesOnNotSupportedComponentQualifiers underTest = new DeleteFavouritesOnNotSupportedComponentQualifiers(db.database());

  @Test
  public void delete_favorites_on_none_supported_component_qualifiers() throws SQLException {
    int moduleId = insertComponent("BRC");
    insertProperty(FAVOURITE_PROPERTY, moduleId);
    insertProperty(FAVOURITE_PROPERTY, moduleId);
    insertProperty(FAVOURITE_PROPERTY, moduleId);
    int libId = insertComponent("LIB");
    insertProperty(FAVOURITE_PROPERTY, libId);

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE)).isZero();
  }

  @Test
  public void ignore_favorites_on_supported_component_qualifiers() throws SQLException {
    int projectId = insertComponent("TRK");
    int prop1 = insertProperty(FAVOURITE_PROPERTY, projectId);
    int fileId = insertComponent("FIL");
    int prop2 = insertProperty(FAVOURITE_PROPERTY, fileId);
    int portfolioId = insertComponent("VW");
    int prop3 = insertProperty(FAVOURITE_PROPERTY, portfolioId);
    int subPortfolioId = insertComponent("SVW");
    int prop4 = insertProperty(FAVOURITE_PROPERTY, subPortfolioId);
    int applicationId = insertComponent("APP");
    int prop5 = insertProperty(FAVOURITE_PROPERTY, applicationId);
    int unitTestId = insertComponent("UTS");
    int prop6 = insertProperty(FAVOURITE_PROPERTY, unitTestId);

    underTest.execute();

    assertProperties(prop1, prop2, prop3, prop4, prop5, prop6);
  }

  @Test
  public void ignore_other_properties() throws SQLException {
    int moduleId = insertComponent("BRC");
    int prop1 = insertProperty("other", moduleId);
    int prop2 = insertProperty("other", moduleId);
    int libId = insertComponent("LIB");
    int prop3 = insertProperty("other", libId);

    underTest.execute();

    assertProperties(prop1, prop2, prop3);
  }

  private void assertProperties(Integer... expectedIds) {
    assertThat(db.select("SELECT id FROM properties")
      .stream()
      .map(map -> ((Long) map.get("ID")).intValue())
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedIds);
  }

  private int insertProperty(String key, int componentId) {
    int id = nextInt();
    db.executeInsert(
      TABLE,
      "ID", id,
      "PROP_KEY", key,
      "USER_ID", nextInt(),
      "RESOURCE_ID", componentId,
      "IS_EMPTY", true,
      "CREATED_AT", 123456);
    return id;
  }

  private int insertComponent(String qualifier) {
    int id = nextInt();
    String uuid = "uuid_" + id;
    db.executeInsert(
      "projects",
      "ID", id,
      "UUID", uuid,
      "ORGANIZATION_UUID", "org_" + id,
      "UUID_PATH", "path_" + id,
      "ROOT_UUID", "root_" + id,
      "PROJECT_UUID", "project_" + id,
      "QUALIFIER", qualifier,
      "ENABLED", true,
      "PRIVATE", false);
    return id;
  }
}

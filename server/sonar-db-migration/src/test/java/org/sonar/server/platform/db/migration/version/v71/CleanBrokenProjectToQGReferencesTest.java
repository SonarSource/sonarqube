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
package org.sonar.server.platform.db.migration.version.v71;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class CleanBrokenProjectToQGReferencesTest {

  private static final String PROPERTY_SONAR_QUALITYGATE = "sonar.qualitygate";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(CleanBrokenProjectToQGReferencesTest.class, "properties_and_quality_gates.sql");

  private CleanBrokenProjectToQGReferences underTest = new CleanBrokenProjectToQGReferences(db.database());

  @Test
  public void do_nothing_when_no_data() throws SQLException {
    assertThat(db.countRowsOfTable("PROPERTIES")).isEqualTo(0);

    underTest.execute();

    assertThat(db.countRowsOfTable("PROPERTIES")).isEqualTo(0);
  }

  @Test
  public void execute_deletes_all_qualitygate_component_properties_when_there_is_no_qualitygate() throws SQLException {
    insertProperty(PROPERTY_SONAR_QUALITYGATE, 30, "12");
    insertProperty(PROPERTY_SONAR_QUALITYGATE, 42, "val1");
    insertProperty(PROPERTY_SONAR_QUALITYGATE, null, "val2");

    underTest.execute();

    assertThat(selectPropertyValues()).containsOnly("val2");
    assertThat(db.countRowsOfTable("PROPERTIES")).isEqualTo(1);
  }

  @Test
  @UseDataProvider("DP_execute_deletes_qualitygate_component_properties_for_non_existing_qualitygate")
  public void execute_deletes_qualitygate_component_properties_for_non_existing_qualitygate(int existingQualityGateCount, int missingQualityGateCount) throws SQLException {
    String[] qualityGateIds = IntStream.range(0, existingQualityGateCount)
      .mapToObj(i -> insertQualityGate())
      .map(s -> {
        int componentId = 2 + s;
        String qualityGateId = String.valueOf(s);
        insertProperty(PROPERTY_SONAR_QUALITYGATE, componentId, qualityGateId);
        return qualityGateId;
      })
      .toArray(String[]::new);
    IntStream.range(0, missingQualityGateCount)
      .forEach(i -> {
        int componentId = 3_000 + i;
        insertProperty(PROPERTY_SONAR_QUALITYGATE, componentId, "non_existing_" + i);
      });

    underTest.execute();

    assertThat(selectPropertyValues()).containsOnly(qualityGateIds);
    assertThat(db.countRowsOfTable("PROPERTIES")).isEqualTo(qualityGateIds.length);
  }

  @DataProvider
  public static Object[][] DP_execute_deletes_qualitygate_component_properties_for_non_existing_qualitygate() {
    Random random = new Random();
    return new Object[][] {
      {1, 1},
      {1, 2},
      {2, 1},
      {2 + random.nextInt(5), 1 + random.nextInt(5)},
    };
  }

  @Test
  public void execute_deletes_only_project_qualitygate_property() throws SQLException {
    String qualityGateId = String.valueOf(insertQualityGate());
    insertProperty(PROPERTY_SONAR_QUALITYGATE, 84651, qualityGateId);
    insertProperty(PROPERTY_SONAR_QUALITYGATE, 7_323, "does_not_exist");
    insertProperty(PROPERTY_SONAR_QUALITYGATE, null, "not a project property");
    insertProperty(PROPERTY_SONAR_QUALITYGATE, null, "not_a_qualitygate_id_either");

    underTest.execute();

    assertThat(selectPropertyValues()).containsExactly(qualityGateId, "not a project property", "not_a_qualitygate_id_either");
    assertThat(db.countRowsOfTable("PROPERTIES")).isEqualTo(3);
  }

  @Test
  public void execute_deletes_only_qualitygate_property_for_project() throws SQLException {
    String qualityGateId = String.valueOf(insertQualityGate());
    insertProperty(PROPERTY_SONAR_QUALITYGATE, 84651, qualityGateId);
    insertProperty("FOO", 84651, "does_not_exist");

    underTest.execute();

    assertThat(selectPropertyValues()).containsExactly(qualityGateId, "does_not_exist");
    assertThat(db.countRowsOfTable("PROPERTIES")).isEqualTo(2);
  }

  private Stream<String> selectPropertyValues() {
    return db.select("select text_value as \"value\" from properties").stream().map(s -> (String) s.get("value"));
  }

  private void insertProperty(String key, @Nullable Integer componentId, String value) {
    db.executeInsert(
      "PROPERTIES",
      "PROP_KEY", key,
      "RESOURCE_ID", componentId,
      "IS_EMPTY", value.isEmpty(),
      "TEXT_VALUE", value);
  }

  private static int qualityGateIdGenerator = 2_999_567 + new Random().nextInt(56);

  private int insertQualityGate() {
    int id = qualityGateIdGenerator++;
    db.executeInsert(
      "QUALITY_GATES",
      "ID", id,
      "UUID", "uuid_" + id,
      "NAME", "name_" + id,
      "IS_BUILT_IN", new Random().nextBoolean());
    return id;
  }
}

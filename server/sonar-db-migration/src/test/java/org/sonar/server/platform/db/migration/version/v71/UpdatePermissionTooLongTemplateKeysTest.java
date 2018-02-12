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

package org.sonar.server.platform.db.migration.version.v71;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class UpdatePermissionTooLongTemplateKeysTest {
  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(UpdatePermissionTooLongTemplateKeysTest.class, "templates.sql");

  private static final Random RANDOM = new Random();

  private final RecordingUuidFactory UUID_FACTORY = new RecordingUuidFactory();
  private final UpdatePermissionTooLongTemplateKeys underTest = new UpdatePermissionTooLongTemplateKeys(dbTester.database(), UUID_FACTORY);

  @Test
  public void keys_whose_length_is_greater_than_40_should_be_updated() throws SQLException {
    // Create 10 permission templates with keys' length greater or equals than 40
    List<String> tooLongKeys = IntStream.range(0, 10)
      .mapToObj(i -> insertTemplate(randomAlphanumeric(41 + RANDOM.nextInt(60))))
      .collect(toList());

    underTest.execute();

    assertThat(UUID_FACTORY.getRecordedUuids()).hasSize(tooLongKeys.size());
    List<String> kees = dbTester.select("select kee from permission_templates").stream()
      .map(r -> (String) r.get("KEE")).collect(toList());

    assertThat(kees).containsExactlyInAnyOrder(UUID_FACTORY.getRecordedUuids().toArray(new String[] {}));
  }

  @Test
  public void keys_whose_length_is_lower_than_40_should_not_be_updated() throws SQLException {
    // Create 10 permission templates with keys' length lower or equals than 40
    List<String> correctKeys = IntStream.range(0, 10)
      .mapToObj(i -> insertTemplate(randomAlphanumeric(RANDOM.nextInt(41))))
      .collect(toList());

    underTest.execute();

    assertThat(UUID_FACTORY.getRecordedUuids()).hasSize(0);
    List<String> kees = dbTester.select("select kee from permission_templates").stream()
      .map(r -> (String) r.get("KEE")).collect(toList());

    assertThat(kees).containsExactlyInAnyOrder(correctKeys.toArray(new String[] {}));
  }

  private String insertTemplate(String kee) {
    dbTester.executeInsert(
      "PERMISSION_TEMPLATES",
      "NAME", randomAlphanumeric(50),
      "ORGANIZATION_UUID", "uuid",
      "KEE", kee);
    return kee;
  }

  private static final class RecordingUuidFactory implements UuidFactory {
    private final List<String> generatedUuids = new ArrayList<>();
    private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

    @Override
    public String create() {
      String uuid = uuidFactory.create();
      generatedUuids.add(uuid);
      return uuid;
    }

    public void clear() {
      generatedUuids.clear();
    }

    public List<String> getRecordedUuids() {
      return ImmutableList.copyOf(generatedUuids);
    }
  }
}

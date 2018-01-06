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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProviderImpl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.assertj.core.api.Assertions.assertThat;

public class UpgradeQualityTemplateLoadedTemplatesTest {

  private static final String TABLE_ORGANIZATIONS = "organizations";
  private static final String DEFAULT_ORGANIZATION_UUID = "def-org";
  private static final String TABLE_LOADED_TEMPLATES = "loaded_templates";
  private static final String QUALITY_PROFILE_TYPE = "QUALITY_PROFILE";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpgradeQualityTemplateLoadedTemplatesTest.class, "organizations_internal_properties_and_loaded_templates.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UpgradeQualityTemplateLoadedTemplates underTest = new UpgradeQualityTemplateLoadedTemplates(db.database(), new DefaultOrganizationUuidProviderImpl());

  @Test
  public void fails_with_ISE_when_no_default_organization_is_set() throws SQLException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default organization uuid is missing");

    underTest.execute();
  }

  @Test
  public void fails_with_ISE_when_default_organization_does_not_exist_in_table_ORGANIZATIONS() throws SQLException {
    insertDefaultOrganizationUuid("blabla");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Default organization with uuid 'blabla' does not exist in table ORGANIZATIONS");

    underTest.execute();
  }

  @Test
  public void execute_has_no_effect_if_loaded_templates_table_is_empty() throws Exception {
    setupDefaultOrganization();

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_LOADED_TEMPLATES)).isEqualTo(0);
  }

  @Test
  public void execute_has_no_effect_if_loaded_templates_has_no_row_with_type_QUALITY_PROFILE() throws Exception {
    setupDefaultOrganization();
    insertLoadedTemplate("foo", "bar");

    underTest.execute();

    assertThat(loadedTemplateExists("foo", "bar")).isTrue();
    assertThat(db.countRowsOfTable(TABLE_LOADED_TEMPLATES)).isEqualTo(1);
  }

  @Test
  public void execute_updates_any_row_with_type_QUALITY_PROFILE_to_type_based_on_current_key_md5_and_default_organization_uuid_as_key() throws SQLException {
    setupDefaultOrganization();

    // put accents to ensure UTF-8 byte array of String is used
    String key1 = "fé@è:bar";
    String key2 = "bar";
    insertLoadedTemplate(QUALITY_PROFILE_TYPE, key1);
    // matching on type is case sensitive
    insertLoadedTemplate(QUALITY_PROFILE_TYPE.toLowerCase(), key1);
    insertLoadedTemplate(QUALITY_PROFILE_TYPE, key2);
    insertLoadedTemplate("other type", key2);

    underTest.execute();

    assertThat(loadedTemplateExists(QUALITY_PROFILE_TYPE + '.' + md5Hex(key1.getBytes(UTF_8)), DEFAULT_ORGANIZATION_UUID)).isTrue();
    assertThat(loadedTemplateExists(QUALITY_PROFILE_TYPE.toLowerCase(), key1)).isTrue();
    assertThat(loadedTemplateExists("other type", key2)).isTrue();
    assertThat(loadedTemplateExists(QUALITY_PROFILE_TYPE + '.' + md5Hex(key2.getBytes(UTF_8)), DEFAULT_ORGANIZATION_UUID)).isTrue();
    assertThat(db.countRowsOfTable(TABLE_LOADED_TEMPLATES)).isEqualTo(4);
  }

  @Test
  public void execute_is_reentrant() throws Exception {
    setupDefaultOrganization();
    String key = "blabla";
    insertLoadedTemplate(QUALITY_PROFILE_TYPE, key);

    underTest.execute();

    underTest.execute();

    assertThat(loadedTemplateExists(QUALITY_PROFILE_TYPE + '.' + md5Hex(key.getBytes(UTF_8)), DEFAULT_ORGANIZATION_UUID)).isTrue();
    assertThat(db.countRowsOfTable(TABLE_LOADED_TEMPLATES)).isEqualTo(1);
  }

  private void setupDefaultOrganization() {
    insertDefaultOrganizationUuid(DEFAULT_ORGANIZATION_UUID);
    insertOrganization(DEFAULT_ORGANIZATION_UUID);
  }

  private void insertOrganization(String uuid) {
    db.executeInsert(
      TABLE_ORGANIZATIONS,
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", String.valueOf(false),
      "CREATED_AT", "1000",
      "UPDATED_AT", "1000");
  }

  private void insertDefaultOrganizationUuid(String defaultOrganizationUuid) {
    db.executeInsert(
      "INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", defaultOrganizationUuid);
  }

  private void insertLoadedTemplate(String type, String key) {
    db.executeInsert(
      TABLE_LOADED_TEMPLATES,
      "TEMPLATE_TYPE", type,
      "KEE", key);
  }

  private boolean loadedTemplateExists(String type, String key) {
    return !db.selectFirst(String.format("select id from loaded_templates where template_type='%s' and kee='%s'", type, key)).isEmpty();
  }

}

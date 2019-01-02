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
package org.sonar.server.platform.db.migration.version.v63;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateDefaultPermTemplateColumnsOfOrganizationsTest {
  private static final String DEFAULT_TEMPLATE_PROPERTY = "sonar.permission.template.default";
  private static final String DEFAULT_PROJECT_TEMPLATE_PROPERTY = "sonar.permission.template.TRK.default";
  private static final String DEFAULT_VIEW_TEMPLATE_PROPERTY = "sonar.permission.template.VW.default";
  private static final String DEFAULT_DEV_TEMPLATE_PROPERTY = "sonar.permission.template.DEV.default";
  private static final String DEFAULT_ORGANIZATION_UUID = "def org uuid";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(PopulateDefaultPermTemplateColumnsOfOrganizationsTest.class, "properties_and_organizations.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private RecordingUuidFactory recordingUuidFactory = new RecordingUuidFactory();
  private PopulateDefaultPermTemplateColumnsOfOrganizations underTest = new PopulateDefaultPermTemplateColumnsOfOrganizations(dbTester.database(),
    new DefaultOrganizationUuidProviderImpl(), recordingUuidFactory);

  @After
  public void clearRecordingUuidFactory() {
    recordingUuidFactory.clear();
  }

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
  public void fails_with_ISE_when_more_than_one_organization_exist() throws SQLException {
    setupDefaultOrganization();

    insertOrganization("other orga uuid");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not migrate DB if more than one organization exists. " +
      "Remove all organizations but the default one which uuid is '" + DEFAULT_ORGANIZATION_UUID + "'");

    underTest.execute();
  }

  @Test
  public void do_nothing_if_global_default_template_property_does_not_exist() throws SQLException {
    setupDefaultOrganization();

    underTest.execute();

    verifyTemplateColumns(null, null);
    verifyPropertiesDoNotExist();
  }

  @Test
  public void execute_sets_project_perm_template_when_global_default_template_is_defined_in_property() throws SQLException {
    setupDefaultOrganization();
    insertProperty(DEFAULT_TEMPLATE_PROPERTY, "foo");

    underTest.execute();

    verifyTemplateColumns("foo", null);
    verifyPropertiesDoNotExist();
  }

  @Test
  public void execute_sets_project_perm_template_from_project_default_template_property_over_global_property() throws SQLException {
    setupDefaultOrganization();
    insertProperty(DEFAULT_TEMPLATE_PROPERTY, "foo");
    insertProperty(DEFAULT_PROJECT_TEMPLATE_PROPERTY, "bar");

    underTest.execute();

    verifyTemplateColumns("bar", null);
    verifyPropertiesDoNotExist();
  }

  @Test
  public void execute_sets_project_perm_template_from_global_property_and_view_perm_template_from_view_property() throws SQLException {
    setupDefaultOrganization();
    insertProperty(DEFAULT_TEMPLATE_PROPERTY, "foo");
    insertProperty(DEFAULT_VIEW_TEMPLATE_PROPERTY, "bar");

    underTest.execute();

    verifyTemplateColumns("foo", "bar");
    verifyPropertiesDoNotExist();
  }

  @Test
  public void execute_should_update_kee_when_old_kee_is_too_long() throws SQLException {
    setupDefaultOrganization();
    insertProperty(DEFAULT_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(100)));
    insertProperty(DEFAULT_VIEW_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(100)));
    insertProperty(DEFAULT_PROJECT_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(100)));
    insertProperty(DEFAULT_DEV_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(100)));

    underTest.execute();

    verifyTemplateColumns(recordingUuidFactory.getRecordedUuids().get(0), recordingUuidFactory.getRecordedUuids().get(1));
    verifyPropertiesDoNotExist();
    verifyExistenceOfPermissionTemplate(recordingUuidFactory.getRecordedUuids().get(0));
    verifyExistenceOfPermissionTemplate(recordingUuidFactory.getRecordedUuids().get(1));
  }

  @Test
  public void execute_should_update_kee_only_when_old_kee_length_is_41_or_more() throws SQLException {
    setupDefaultOrganization();
    insertProperty(DEFAULT_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(40)));
    insertProperty(DEFAULT_VIEW_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(40)));
    insertProperty(DEFAULT_PROJECT_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(40)));
    insertProperty(DEFAULT_DEV_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(40)));

    underTest.execute();

    assertThat(recordingUuidFactory.getRecordedUuids()).isEmpty();

    insertProperty(DEFAULT_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(41)));
    insertProperty(DEFAULT_VIEW_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(41)));
    insertProperty(DEFAULT_PROJECT_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(41)));
    insertProperty(DEFAULT_DEV_TEMPLATE_PROPERTY, insertPermissionTemplates(randomAlphanumeric(41)));

    underTest.execute();

    assertThat(recordingUuidFactory.getRecordedUuids()).hasSize(2);
  }

  @Test
  public void execute_sets_project_from_project_property_and_view_from_view_property_when_all_properties_are_defined() throws SQLException {
    setupDefaultOrganization();
    insertProperty(DEFAULT_TEMPLATE_PROPERTY, "foo");
    insertProperty(DEFAULT_PROJECT_TEMPLATE_PROPERTY, "bar");
    insertProperty(DEFAULT_VIEW_TEMPLATE_PROPERTY, "doh");

    underTest.execute();

    verifyTemplateColumns("bar", "doh");
    verifyPropertiesDoNotExist();
  }

  @Test
  public void execute_deletes_dev_property_when_it_is_defined() throws SQLException {
    setupDefaultOrganization();
    insertProperty(DEFAULT_TEMPLATE_PROPERTY, "foo");
    insertProperty(DEFAULT_DEV_TEMPLATE_PROPERTY, "bar");

    underTest.execute();

    verifyPropertiesDoNotExist();
  }

  private void verifyTemplateColumns(@Nullable String project, @Nullable String view) {
    Map<String, Object> row = dbTester.selectFirst("select " +
      " default_perm_template_project as \"projectDefaultPermTemplate\"," +
      " default_perm_template_view as \"viewDefaultPermTemplate\"" +
      " from organizations where uuid='" + DEFAULT_ORGANIZATION_UUID + "'");
    assertThat(row.get("projectDefaultPermTemplate")).isEqualTo(project);
    assertThat(row.get("viewDefaultPermTemplate")).isEqualTo(view);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    setupDefaultOrganization();
    insertProperty(DEFAULT_TEMPLATE_PROPERTY, "foo");

    underTest.execute();

    underTest.execute();
  }

  private void verifyPropertiesDoNotExist() {
    verifyPropertyDoesNotExist(DEFAULT_TEMPLATE_PROPERTY);
    verifyPropertyDoesNotExist(DEFAULT_PROJECT_TEMPLATE_PROPERTY);
    verifyPropertyDoesNotExist(DEFAULT_VIEW_TEMPLATE_PROPERTY);
    verifyPropertyDoesNotExist(DEFAULT_DEV_TEMPLATE_PROPERTY);
  }

  private void verifyPropertyDoesNotExist(String globalPermissionTemplateDefault) {
    assertThat(dbTester.countSql("select count(*) as \"cnt\" from PROPERTIES where prop_key='" + globalPermissionTemplateDefault + "'"))
      .isEqualTo(0);
  }

  private void verifyExistenceOfPermissionTemplate(String kee) {
    assertThat(dbTester.countSql("select count(kee) from permission_templates where kee='" + kee +"'")).isEqualTo(1);
  }

  private void setupDefaultOrganization() {
    insertDefaultOrganizationUuid(DEFAULT_ORGANIZATION_UUID);
    insertOrganization(DEFAULT_ORGANIZATION_UUID);
  }

  private void insertOrganization(String uuid) {
    dbTester.executeInsert(
      "ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "CREATED_AT", "1000",
      "UPDATED_AT", "1000");
  }

  private String insertPermissionTemplates(String kee) {
    dbTester.executeInsert(
      "PERMISSION_TEMPLATES",
      "ORGANIZATION_UUID", DEFAULT_ORGANIZATION_UUID,
      "NAME", randomAlphanumeric(50),
      "KEE", kee,
      "DESCRIPTION", randomAlphanumeric(500),
      "KEY_PATTERN", "",
      "CREATED_AT", new Timestamp(1000L),
      "UPDATED_AT", new Timestamp(1000L));

    return kee;
  }

  private void insertDefaultOrganizationUuid(String defaultOrganizationUuid) {
    dbTester.executeInsert(
      "INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", defaultOrganizationUuid);
  }

  private void insertProperty(String key, @Nullable String value) {
    dbTester.executeInsert(
      "PROPERTIES",
      "PROP_KEY", key,
      "IS_EMPTY", String.valueOf(value == null),
      "TEXT_VALUE", value);
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

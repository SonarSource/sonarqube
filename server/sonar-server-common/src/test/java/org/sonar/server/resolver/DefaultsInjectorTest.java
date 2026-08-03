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
package org.sonar.server.resolver;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sonarsource.enterprises.api.rest.EnterpriseId;
import org.sonarsource.enterprises.server.DefaultEnterpriseProvider;
import org.sonarsource.organizations.api.rest.OrganizationId;
import org.sonarsource.organizations.api.rest.OrganizationKey;
import org.sonarsource.organizations.api.rest.OrganizationLegacyId;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultsInjectorTest {

  @Test
  void injectDefaults_withRecordAndOrgAnnotations_injectsDefaults() throws Exception {
    TestRecordWithOrg input = new TestRecordWithOrg(null, null, null, "test-name", 42);

    Object result = DefaultsInjector.injectDefaults(input, TestRecordWithOrg.class);

    assertThat(result).isInstanceOf(TestRecordWithOrg.class);
    TestRecordWithOrg testRecord = (TestRecordWithOrg) result;
    assertThat(testRecord.orgId()).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(testRecord.orgKey()).isEqualTo(DefaultOrganizationProvider.KEY);
    assertThat(testRecord.orgLegacyId()).isEqualTo(DefaultOrganizationProvider.LEGACY_ID);
    assertThat(testRecord.name()).isEqualTo("test-name");
    assertThat(testRecord.count()).isEqualTo(42);
  }

  @Test
  void injectDefaults_withRecordAndUserProvidedOrgValues_overridesWithDefaults() throws Exception {
    TestRecordWithOrg input = new TestRecordWithOrg("user-id", "user-key", "user-legacy", "test-name", 42);

    Object result = DefaultsInjector.injectDefaults(input, TestRecordWithOrg.class);

    assertThat(result).isInstanceOf(TestRecordWithOrg.class);
    TestRecordWithOrg testRecord = (TestRecordWithOrg) result;
    assertThat(testRecord.orgId()).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(testRecord.orgKey()).isEqualTo(DefaultOrganizationProvider.KEY);
    assertThat(testRecord.orgLegacyId()).isEqualTo(DefaultOrganizationProvider.LEGACY_ID);
    assertThat(testRecord.name()).isEqualTo("test-name");
    assertThat(testRecord.count()).isEqualTo(42);
  }

  @Test
  void injectDefaults_withRecordAndOrgListAnnotation_injectsDefaultAsSingletonList() throws Exception {
    TestRecordWithOrgList input = new TestRecordWithOrgList(null, "test-name");

    Object result = DefaultsInjector.injectDefaults(input, TestRecordWithOrgList.class);

    assertThat(result).isInstanceOf(TestRecordWithOrgList.class);
    TestRecordWithOrgList testRecord = (TestRecordWithOrgList) result;
    assertThat(testRecord.orgIds()).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(testRecord.name()).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withRecordAndUserProvidedOrgListValue_overridesWithDefaultList() throws Exception {
    TestRecordWithOrgList input = new TestRecordWithOrgList(List.of(UUID.fromString("11111111-1111-4111-1111-111111111111")), "test-name");

    Object result = DefaultsInjector.injectDefaults(input, TestRecordWithOrgList.class);

    assertThat(result).isInstanceOf(TestRecordWithOrgList.class);
    TestRecordWithOrgList testRecord = (TestRecordWithOrgList) result;
    assertThat(testRecord.orgIds()).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(testRecord.name()).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withRecordAndEmptyOrgListValue_overridesWithDefaultList() throws Exception {
    TestRecordWithOrgList input = new TestRecordWithOrgList(List.of(), "test-name");

    Object result = DefaultsInjector.injectDefaults(input, TestRecordWithOrgList.class);

    assertThat(result).isInstanceOf(TestRecordWithOrgList.class);
    TestRecordWithOrgList testRecord = (TestRecordWithOrgList) result;
    assertThat(testRecord.orgIds()).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(testRecord.name()).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withRecordWithoutAnnotations_doesNotModify() throws Exception {
    TestRecordWithoutAnnotations input = new TestRecordWithoutAnnotations("test-name", 42);

    Object result = DefaultsInjector.injectDefaults(input, TestRecordWithoutAnnotations.class);

    assertThat(result).isInstanceOf(TestRecordWithoutAnnotations.class);
    TestRecordWithoutAnnotations testRecord = (TestRecordWithoutAnnotations) result;
    assertThat(testRecord.name()).isEqualTo("test-name");
    assertThat(testRecord.count()).isEqualTo(42);
  }

  @Test
  void injectDefaults_withRecordAndEnterpriseAnnotation_injectsDefault() throws Exception {
    TestRecordWithEnterprise input = new TestRecordWithEnterprise(null, "test-name", 42);

    Object result = DefaultsInjector.injectDefaults(input, TestRecordWithEnterprise.class);

    assertThat(result).isInstanceOf(TestRecordWithEnterprise.class);
    TestRecordWithEnterprise testRecord = (TestRecordWithEnterprise) result;
    assertThat(testRecord.enterpriseId()).isEqualTo(DefaultEnterpriseProvider.ENTERPRISE_ID.toString());
    assertThat(testRecord.name()).isEqualTo("test-name");
    assertThat(testRecord.count()).isEqualTo(42);
  }

  @Test
  void injectDefaults_withRecordAndUserProvidedEnterpriseValue_overridesWithDefault() throws Exception {
    TestRecordWithEnterprise input = new TestRecordWithEnterprise("user-enterprise-id", "test-name", 42);

    Object result = DefaultsInjector.injectDefaults(input, TestRecordWithEnterprise.class);

    assertThat(result).isInstanceOf(TestRecordWithEnterprise.class);
    TestRecordWithEnterprise testRecord = (TestRecordWithEnterprise) result;
    assertThat(testRecord.enterpriseId()).isEqualTo(DefaultEnterpriseProvider.ENTERPRISE_ID.toString());
    assertThat(testRecord.name()).isEqualTo("test-name");
    assertThat(testRecord.count()).isEqualTo(42);
  }

  @Test
  void injectDefaults_withClassAndOrgAnnotations_injectsDefaults() throws Exception {
    TestClassWithOrg input = new TestClassWithOrg();
    input.orgKey = null;
    input.orgId = null;
    input.name = "test-name";

    Object result = DefaultsInjector.injectDefaults(input, TestClassWithOrg.class);

    assertThat(result).isInstanceOf(TestClassWithOrg.class);
    TestClassWithOrg obj = (TestClassWithOrg) result;
    assertThat(obj.orgId).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(obj.orgKey).isEqualTo(DefaultOrganizationProvider.KEY);
    assertThat(obj.name).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withClassAndUserProvidedOrgValues_overridesWithDefaults() throws Exception {
    TestClassWithOrg input = new TestClassWithOrg();
    input.orgKey = "user-key";
    input.orgId = "user-id";
    input.name = "test-name";

    Object result = DefaultsInjector.injectDefaults(input, TestClassWithOrg.class);

    assertThat(result).isInstanceOf(TestClassWithOrg.class);
    TestClassWithOrg obj = (TestClassWithOrg) result;
    assertThat(obj.orgId).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(obj.orgKey).isEqualTo(DefaultOrganizationProvider.KEY);
    assertThat(obj.name).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withClassAndOrgListAnnotation_injectsDefaultAsSingletonList() throws Exception {
    TestClassWithOrgList input = new TestClassWithOrgList();
    input.orgIds = null;
    input.name = "test-name";

    Object result = DefaultsInjector.injectDefaults(input, TestClassWithOrgList.class);

    assertThat(result).isInstanceOf(TestClassWithOrgList.class);
    TestClassWithOrgList obj = (TestClassWithOrgList) result;
    assertThat(obj.orgIds).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(obj.name).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withClassAndUserProvidedOrgListValue_overridesWithDefaultList() throws Exception {
    TestClassWithOrgList input = new TestClassWithOrgList();
    input.orgIds = List.of(UUID.fromString("11111111-1111-4111-1111-111111111111"));
    input.name = "test-name";

    Object result = DefaultsInjector.injectDefaults(input, TestClassWithOrgList.class);

    assertThat(result).isInstanceOf(TestClassWithOrgList.class);
    TestClassWithOrgList obj = (TestClassWithOrgList) result;
    assertThat(obj.orgIds).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(obj.name).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withClassAndEmptyOrgListValue_overridesWithDefaultList() throws Exception {
    TestClassWithOrgList input = new TestClassWithOrgList();
    input.orgIds = List.of();
    input.name = "test-name";

    Object result = DefaultsInjector.injectDefaults(input, TestClassWithOrgList.class);

    assertThat(result).isInstanceOf(TestClassWithOrgList.class);
    TestClassWithOrgList obj = (TestClassWithOrgList) result;
    assertThat(obj.orgIds).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(obj.name).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withClassWithoutAnnotations_doesNotModify() throws Exception {
    TestClassWithoutAnnotations input = new TestClassWithoutAnnotations();
    input.name = "test-name";
    input.count = 42;

    Object result = DefaultsInjector.injectDefaults(input, TestClassWithoutAnnotations.class);

    assertThat(result).isInstanceOf(TestClassWithoutAnnotations.class);
    TestClassWithoutAnnotations obj = (TestClassWithoutAnnotations) result;
    assertThat(obj.name).isEqualTo("test-name");
    assertThat(obj.count).isEqualTo(42);
  }

  @Test
  void injectDefaults_withClassAndEnterpriseAnnotation_injectsDefault() throws Exception {
    TestClassWithEnterprise input = new TestClassWithEnterprise();
    input.enterpriseId = null;
    input.name = "test-name";

    Object result = DefaultsInjector.injectDefaults(input, TestClassWithEnterprise.class);

    assertThat(result).isInstanceOf(TestClassWithEnterprise.class);
    TestClassWithEnterprise obj = (TestClassWithEnterprise) result;
    assertThat(obj.enterpriseId).isEqualTo(DefaultEnterpriseProvider.ENTERPRISE_ID.toString());
    assertThat(obj.name).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withClassAndUserProvidedEnterpriseValue_overridesWithDefault() throws Exception {
    TestClassWithEnterprise input = new TestClassWithEnterprise();
    input.enterpriseId = "user-enterprise-id";
    input.name = "test-name";

    Object result = DefaultsInjector.injectDefaults(input, TestClassWithEnterprise.class);

    assertThat(result).isInstanceOf(TestClassWithEnterprise.class);
    TestClassWithEnterprise obj = (TestClassWithEnterprise) result;
    assertThat(obj.enterpriseId).isEqualTo(DefaultEnterpriseProvider.ENTERPRISE_ID.toString());
    assertThat(obj.name).isEqualTo("test-name");
  }

  record TestRecordWithOrg(
    @OrganizationId String orgId,
    @OrganizationKey String orgKey,
    @OrganizationLegacyId String orgLegacyId,
    String name,
    Integer count
  ) {}

  record TestRecordWithEnterprise(
    @EnterpriseId String enterpriseId,
    String name,
    Integer count
  ) {}

  record TestRecordWithoutAnnotations(String name, Integer count) {}

  record TestRecordWithOrgList(
    @OrganizationId List<UUID> orgIds,
    String name
  ) {}

  static class TestClassWithOrg {
    @OrganizationId
    public String orgId;
    @OrganizationKey
    public String orgKey;
    public String name;
  }

  static class TestClassWithEnterprise {
    @EnterpriseId
    public String enterpriseId;
    public String name;
  }

  static class TestClassWithoutAnnotations {
    public String name;
    public Integer count;
  }

  static class TestClassWithOrgList {
    @OrganizationId
    public List<UUID> orgIds;
    public String name;
  }
}

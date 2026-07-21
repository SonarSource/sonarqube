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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.web.method.HandlerMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonarsource.enterprises.api.rest.EnterpriseId;
import org.sonarsource.enterprises.server.DefaultEnterpriseProvider;
import org.sonarsource.organizations.api.rest.OrganizationId;
import org.sonarsource.organizations.api.rest.OrganizationKey;
import org.sonarsource.organizations.api.rest.OrganizationLegacyId;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestParam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultsArgumentResolverTest {

  private DefaultsArgumentResolver underTest;

  @BeforeEach
  void setUp() {
    underTest = new DefaultsArgumentResolver();
  }

  @Test
  void supportsParameter_withParameterObjectAnnotation_returnsTrue() {
    MethodParameter parameter = mock(MethodParameter.class);
    when(parameter.hasParameterAnnotation(ParameterObject.class)).thenReturn(true);

    assertThat(underTest.supportsParameter(parameter)).isTrue();
  }

  @Test
  void supportsParameter_withoutAnnotation_returnsFalse() {
    MethodParameter parameter = mock(MethodParameter.class);
    when(parameter.hasParameterAnnotation(ParameterObject.class)).thenReturn(false);
    Parameter javaParameter = mock(Parameter.class);
    when(parameter.getParameter()).thenReturn(javaParameter);
    when(javaParameter.isAnnotationPresent(OrganizationKey.class)).thenReturn(false);
    when(javaParameter.isAnnotationPresent(OrganizationId.class)).thenReturn(false);
    when(javaParameter.isAnnotationPresent(OrganizationLegacyId.class)).thenReturn(false);
    when(javaParameter.isAnnotationPresent(EnterpriseId.class)).thenReturn(false);

    assertThat(underTest.supportsParameter(parameter)).isFalse();
  }

  @ParameterizedTest
  @MethodSource("annotationMethods")
  void supportsParameter_withAnnotations_returnsTrue(String methodName, Class<?> paramType) throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod(methodName, paramType);
    MethodParameter parameter = new MethodParameter(method, 0);

    assertThat(underTest.supportsParameter(parameter)).isTrue();
  }

  private static Stream<Object[]> annotationMethods() {
    return Stream.of(
      new Object[]{"methodWithOrgKey", String.class},
      new Object[]{"methodWithOrgId", String.class},
      new Object[]{"methodWithOrgLegacyId", String.class},
      new Object[]{"methodWithEnterpriseId", String.class}
    );
  }

  @Test
  void resolveArgument_withOrganizationKeyAnnotation_returnsDefaultKey() throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod("methodWithOrgKey", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(DefaultOrganizationProvider.KEY);
  }

  @Test
  void resolveArgument_withOrganizationIdAnnotation_returnsDefaultId() throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod("methodWithOrgId", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(DefaultOrganizationProvider.ID.toString());
  }

  @Test
  void resolveArgument_withOrganizationLegacyIdAnnotation_returnsDefaultLegacyId() throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod("methodWithOrgLegacyId", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(DefaultOrganizationProvider.LEGACY_ID);
  }

  @Test
  void resolveArgument_withEnterpriseIdAnnotation_returnsDefaultEnterpriseId() throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod("methodWithEnterpriseId", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(DefaultEnterpriseProvider.ENTERPRISE_ID.toString());
  }

  @Test
  void resolveArgument_withOrganizationIdAnnotationOnListParameter_returnsSingletonDefaultList() throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod("methodWithOrgIdList", List.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(List.of(DefaultOrganizationProvider.ID));
  }


  @Test
  void resolveArgument_withOrganizationIdAnnotationOnArrayParameter_returnsSingletonDefaultArray() throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod("methodWithOrgIdArray", UUID[].class);
    MethodParameter parameter = new MethodParameter(method, 0);

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(new UUID[] {DefaultOrganizationProvider.ID});
  }

  @Test
  void resolveArgument_withOrganizationIdAnnotationOnSetParameter_returnsSingletonDefaultSet() throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod("methodWithOrgIdSet", Set.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(Set.of(DefaultOrganizationProvider.ID));
  }

  @Test
  void injectDefaults_withRecordAndOrgAnnotations_injectsDefaults() throws Exception {
    TestRecordWithOrg input = new TestRecordWithOrg(null, null, null, "test-name", 42);

    Object result = invokeInjectDefaults(input, TestRecordWithOrg.class);

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

    Object result = invokeInjectDefaults(input, TestRecordWithOrg.class);

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

    Object result = invokeInjectDefaults(input, TestRecordWithOrgList.class);

    assertThat(result).isInstanceOf(TestRecordWithOrgList.class);
    TestRecordWithOrgList testRecord = (TestRecordWithOrgList) result;
    assertThat(testRecord.orgIds()).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(testRecord.name()).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withRecordAndUserProvidedOrgListValue_overridesWithDefaultList() throws Exception {
    TestRecordWithOrgList input = new TestRecordWithOrgList(List.of(UUID.fromString("11111111-1111-4111-1111-111111111111")), "test-name");

    Object result = invokeInjectDefaults(input, TestRecordWithOrgList.class);

    assertThat(result).isInstanceOf(TestRecordWithOrgList.class);
    TestRecordWithOrgList testRecord = (TestRecordWithOrgList) result;
    assertThat(testRecord.orgIds()).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(testRecord.name()).isEqualTo("test-name");
  }


  @Test
  void injectDefaults_withRecordAndEmptyOrgListValue_overridesWithDefaultList() throws Exception {
    TestRecordWithOrgList input = new TestRecordWithOrgList(List.of(), "test-name");

    Object result = invokeInjectDefaults(input, TestRecordWithOrgList.class);

    assertThat(result).isInstanceOf(TestRecordWithOrgList.class);
    TestRecordWithOrgList testRecord = (TestRecordWithOrgList) result;
    assertThat(testRecord.orgIds()).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(testRecord.name()).isEqualTo("test-name");
  }

  @Test
  void injectDefaults_withRecordWithoutAnnotations_doesNotModify() throws Exception {
    TestRecordWithoutAnnotations input = new TestRecordWithoutAnnotations("test-name", 42);

    Object result = invokeInjectDefaults(input, TestRecordWithoutAnnotations.class);

    assertThat(result).isInstanceOf(TestRecordWithoutAnnotations.class);
    TestRecordWithoutAnnotations testRecord = (TestRecordWithoutAnnotations) result;
    assertThat(testRecord.name()).isEqualTo("test-name");
    assertThat(testRecord.count()).isEqualTo(42);
  }

  @Test
  void injectDefaults_withRecordAndEnterpriseAnnotation_injectsDefault() throws Exception {
    TestRecordWithEnterprise input = new TestRecordWithEnterprise(null, "test-name", 42);

    Object result = invokeInjectDefaults(input, TestRecordWithEnterprise.class);

    assertThat(result).isInstanceOf(TestRecordWithEnterprise.class);
    TestRecordWithEnterprise testRecord = (TestRecordWithEnterprise) result;
    assertThat(testRecord.enterpriseId()).isEqualTo(DefaultEnterpriseProvider.ENTERPRISE_ID.toString());
    assertThat(testRecord.name()).isEqualTo("test-name");
    assertThat(testRecord.count()).isEqualTo(42);
  }

  @Test
  void injectDefaults_withRecordAndUserProvidedEnterpriseValue_overridesWithDefault() throws Exception {
    TestRecordWithEnterprise input = new TestRecordWithEnterprise("user-enterprise-id", "test-name", 42);

    Object result = invokeInjectDefaults(input, TestRecordWithEnterprise.class);

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

    Object result = invokeInjectDefaults(input, TestClassWithOrg.class);

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

    Object result = invokeInjectDefaults(input, TestClassWithOrg.class);

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

    Object result = invokeInjectDefaults(input, TestClassWithOrgList.class);

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

    Object result = invokeInjectDefaults(input, TestClassWithOrgList.class);

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

    Object result = invokeInjectDefaults(input, TestClassWithOrgList.class);

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

    Object result = invokeInjectDefaults(input, TestClassWithoutAnnotations.class);

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

    Object result = invokeInjectDefaults(input, TestClassWithEnterprise.class);

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

    Object result = invokeInjectDefaults(input, TestClassWithEnterprise.class);

    assertThat(result).isInstanceOf(TestClassWithEnterprise.class);
    TestClassWithEnterprise obj = (TestClassWithEnterprise) result;
    assertThat(obj.enterpriseId).isEqualTo(DefaultEnterpriseProvider.ENTERPRISE_ID.toString());
    assertThat(obj.name).isEqualTo("test-name");
  }

  private Object invokeInjectDefaults(Object boundObject, Class<?> parameterType) throws Exception {
    var method = DefaultsArgumentResolver.class.getDeclaredMethod("injectDefaults", Object.class, Class.class);
    method.setAccessible(true);
    return method.invoke(underTest, boundObject, parameterType);
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

  static class TestControllerWithIndividualParams {
    public void methodWithOrgKey(@OrganizationKey @RequestParam String organizationKey) {
      //empty for tests
    }

    public void methodWithOrgId(@OrganizationId @RequestParam String organizationId) {
      //empty for tests
    }

    public void methodWithOrgLegacyId(@OrganizationLegacyId @RequestParam String organizationLegacyId) {
      //empty for tests
    }

    public void methodWithEnterpriseId(@EnterpriseId @RequestParam String enterpriseId) {
      //empty for tests
    }

    public void methodWithOrgIdList(@OrganizationId @RequestParam List<UUID> organizationIds) {
      //empty for tests
    }

    public void methodWithOrgIdArray(@OrganizationId @RequestParam UUID[] organizationIds) {
      //empty for tests
    }

    public void methodWithOrgIdSet(@OrganizationId @RequestParam Set<UUID> organizationIds) {
      //empty for tests
    }
  }

  // Interface with annotations on method parameters — the concrete implementation does NOT repeat them.
  interface TestApiInterface {
    void methodWithOrgId(@OrganizationId @RequestParam String organizationId);
    void methodWithOrgKey(@OrganizationKey @RequestParam String organizationKey);
  }

  static class TestControllerImplementingInterface implements TestApiInterface {
    @Override
    public void methodWithOrgId(String organizationId) {
      //empty for tests
    }

    @Override
    public void methodWithOrgKey(String organizationKey) {
      //empty for tests
    }
  }

  @Test
  void supportsParameter_withAnnotationOnInterfaceOnly_returnsTrue() throws Exception {
    Method method = TestControllerImplementingInterface.class.getMethod("methodWithOrgId", String.class);
    MethodParameter parameter = new HandlerMethod(new TestControllerImplementingInterface(), method).getMethodParameters()[0];

    assertThat(underTest.supportsParameter(parameter)).isTrue();
  }

  @Test
  void resolveArgument_withAnnotationOnInterfaceOnly_returnsDefault() throws Exception {
    Method method = TestControllerImplementingInterface.class.getMethod("methodWithOrgId", String.class);
    MethodParameter parameter = new HandlerMethod(new TestControllerImplementingInterface(), method).getMethodParameters()[0];

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(DefaultOrganizationProvider.ID.toString());
  }

  @Test
  void resolveArgument_withOrgKeyAnnotationOnInterfaceOnly_returnsDefault() throws Exception {
    Method method = TestControllerImplementingInterface.class.getMethod("methodWithOrgKey", String.class);
    MethodParameter parameter = new HandlerMethod(new TestControllerImplementingInterface(), method).getMethodParameters()[0];

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result).isEqualTo(DefaultOrganizationProvider.KEY);
  }
}

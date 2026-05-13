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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

class OrganizationDefaultsArgumentResolverTest {

  private OrganizationDefaultsArgumentResolver underTest;

  @BeforeEach
  void setUp() {
    underTest = new OrganizationDefaultsArgumentResolver();
  }

  @Test
  void supportsParameter_withParameterObjectAnnotation_returnsTrue() {
    MethodParameter parameter = mock(MethodParameter.class);
    when(parameter.hasParameterAnnotation(ParameterObject.class)).thenReturn(true);

    assertThat(underTest.supportsParameter(parameter)).isTrue();
  }

  @Test
  void supportsParameter_withoutParameterObjectAnnotation_returnsFalse() {
    MethodParameter parameter = mock(MethodParameter.class);
    when(parameter.hasParameterAnnotation(ParameterObject.class)).thenReturn(false);
    Parameter javaParameter = mock(Parameter.class);
    when(parameter.getParameter()).thenReturn(javaParameter);
    when(javaParameter.isAnnotationPresent(OrganizationKey.class)).thenReturn(false);
    when(javaParameter.isAnnotationPresent(OrganizationId.class)).thenReturn(false);
    when(javaParameter.isAnnotationPresent(OrganizationLegacyId.class)).thenReturn(false);

    assertThat(underTest.supportsParameter(parameter)).isFalse();
  }

  @ParameterizedTest
  @MethodSource("organizationAnnotationMethods")
  void supportsParameter_withOrganizationAnnotations_returnsTrue(String methodName) throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod(methodName, String.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    assertThat(underTest.supportsParameter(parameter)).isTrue();
  }

  private static Stream<String> organizationAnnotationMethods() {
    return Stream.of("methodWithOrgKey", "methodWithOrgId", "methodWithOrgLegacyId");
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
  void injectOrganizationDefaults_withRecordAndOrgAnnotations_injectsDefaults() throws Exception {
    TestRecordWithOrg input = new TestRecordWithOrg(null, null, null, "test-name", 42);

    Object result = invokeInjectOrganizationDefaults(input, TestRecordWithOrg.class);

    assertThat(result).isInstanceOf(TestRecordWithOrg.class);
    TestRecordWithOrg testRecordWithOrg = (TestRecordWithOrg) result;
    assertThat(testRecordWithOrg.orgId()).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(testRecordWithOrg.orgKey()).isEqualTo(DefaultOrganizationProvider.KEY);
    assertThat(testRecordWithOrg.orgLegacyId()).isEqualTo(DefaultOrganizationProvider.LEGACY_ID);
    assertThat(testRecordWithOrg.name()).isEqualTo("test-name");
    assertThat(testRecordWithOrg.count()).isEqualTo(42);
  }

  @Test
  void injectOrganizationDefaults_withRecordAndUserProvidedOrgValues_overridesWithDefaults() throws Exception {
    TestRecordWithOrg input = new TestRecordWithOrg("user-id", "user-key", "user-legacy", "test-name", 42);

    Object result = invokeInjectOrganizationDefaults(input, TestRecordWithOrg.class);

    assertThat(result).isInstanceOf(TestRecordWithOrg.class);
    TestRecordWithOrg testRecordWithOrg = (TestRecordWithOrg) result;
    assertThat(testRecordWithOrg.orgId()).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(testRecordWithOrg.orgKey()).isEqualTo(DefaultOrganizationProvider.KEY);
    assertThat(testRecordWithOrg.orgLegacyId()).isEqualTo(DefaultOrganizationProvider.LEGACY_ID);
    assertThat(testRecordWithOrg.name()).isEqualTo("test-name");
    assertThat(testRecordWithOrg.count()).isEqualTo(42);
  }

  @Test
  void injectOrganizationDefaults_withRecordWithoutOrgAnnotations_doesNotModify() throws Exception {
    TestRecordWithoutOrg input = new TestRecordWithoutOrg("test-name", 42);

    Object result = invokeInjectOrganizationDefaults(input, TestRecordWithoutOrg.class);

    assertThat(result).isInstanceOf(TestRecordWithoutOrg.class);
    TestRecordWithoutOrg testRecordWithoutOrg = (TestRecordWithoutOrg) result;
    assertThat(testRecordWithoutOrg.name()).isEqualTo("test-name");
    assertThat(testRecordWithoutOrg.count()).isEqualTo(42);
  }

  @Test
  void injectOrganizationDefaults_withClassAndOrgAnnotations_injectsDefaults() throws Exception {
    TestClassWithOrg input = new TestClassWithOrg();
    input.orgKey = null;
    input.orgId = null;
    input.name = "test-name";

    Object result = invokeInjectOrganizationDefaults(input, TestClassWithOrg.class);

    assertThat(result).isInstanceOf(TestClassWithOrg.class);
    TestClassWithOrg obj = (TestClassWithOrg) result;
    assertThat(obj.orgId).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(obj.orgKey).isEqualTo(DefaultOrganizationProvider.KEY);
    assertThat(obj.name).isEqualTo("test-name");
  }

  @Test
  void injectOrganizationDefaults_withClassAndUserProvidedOrgValues_overridesWithDefaults() throws Exception {
    TestClassWithOrg input = new TestClassWithOrg();
    input.orgKey = "user-key";
    input.orgId = "user-id";
    input.name = "test-name";

    Object result = invokeInjectOrganizationDefaults(input, TestClassWithOrg.class);

    assertThat(result).isInstanceOf(TestClassWithOrg.class);
    TestClassWithOrg obj = (TestClassWithOrg) result;
    assertThat(obj.orgId).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(obj.orgKey).isEqualTo(DefaultOrganizationProvider.KEY);
    assertThat(obj.name).isEqualTo("test-name");
  }

  @Test
  void injectOrganizationDefaults_withClassWithoutOrgAnnotations_doesNotModify() throws Exception {
    TestClassWithoutOrg input = new TestClassWithoutOrg();
    input.name = "test-name";
    input.count = 42;

    Object result = invokeInjectOrganizationDefaults(input, TestClassWithoutOrg.class);

    assertThat(result).isInstanceOf(TestClassWithoutOrg.class);
    TestClassWithoutOrg obj = (TestClassWithoutOrg) result;
    assertThat(obj.name).isEqualTo("test-name");
    assertThat(obj.count).isEqualTo(42);
  }

  private Object invokeInjectOrganizationDefaults(Object boundObject, Class<?> parameterType) throws Exception {
    var method = OrganizationDefaultsArgumentResolver.class.getDeclaredMethod("injectOrganizationDefaults", Object.class, Class.class);
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

  record TestRecordWithoutOrg(String name, Integer count) {}

  static class TestClassWithOrg {
    @OrganizationId
    public String orgId;
    @OrganizationKey
    public String orgKey;
    public String name;
  }

  static class TestClassWithoutOrg {
    public String name;
    public Integer count;
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
  }
}

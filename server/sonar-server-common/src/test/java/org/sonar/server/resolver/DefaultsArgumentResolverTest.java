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
import org.springframework.web.method.HandlerMethod;

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
  void resolveArgument_withOrganizationIdAnnotationOnUuidParameter_returnsDefaultUuid() throws Exception {
    Method method = TestControllerWithIndividualParams.class.getMethod("methodWithOrgIdUuid", UUID.class);
    MethodParameter parameter = new MethodParameter(method, 0);

    Object result = underTest.resolveArgument(parameter, null, null, null);

    assertThat(result)
      .isInstanceOf(UUID.class)
      .isEqualTo(DefaultOrganizationProvider.ID);
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

  static class TestControllerWithIndividualParams {
    public void methodWithOrgKey(@OrganizationKey @RequestParam String organizationKey) {
      //empty for tests
    }

    public void methodWithOrgId(@OrganizationId @RequestParam String organizationId) {
      //empty for tests
    }

    public void methodWithOrgIdUuid(@OrganizationId @RequestParam UUID organizationId) {
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

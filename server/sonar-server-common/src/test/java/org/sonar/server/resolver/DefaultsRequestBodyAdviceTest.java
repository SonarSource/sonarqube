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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonarsource.organizations.api.rest.OrganizationId;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.annotation.RequestBody;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultsRequestBodyAdviceTest {

  private DefaultsRequestBodyAdvice underTest;
  private final MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
  private final Class<? extends HttpMessageConverter<?>> converterType = JacksonJsonHttpMessageConverter.class;

  @BeforeEach
  void setUp() {
    underTest = new DefaultsRequestBodyAdvice();
  }

  @Test
  void supports_withAnnotatedMember_returnsTrue() throws Exception {
    MethodParameter parameter = parameterOf("methodWithOrgBody", TestRecordWithOrg.class);

    assertThat(underTest.supports(parameter, parameter.getGenericParameterType(), converterType)).isTrue();
  }

  @Test
  void supports_withoutAnnotatedMember_returnsFalse() throws Exception {
    MethodParameter parameter = parameterOf("methodWithoutAnnotationsBody", TestRecordWithoutAnnotations.class);

    assertThat(underTest.supports(parameter, parameter.getGenericParameterType(), converterType)).isFalse();
  }

  @Test
  void supports_withAnnotatedMember_memoizesResultPerType() throws Exception {
    MethodParameter parameter = parameterOf("methodWithOrgBody", TestRecordWithOrg.class);

    underTest.supports(parameter, parameter.getGenericParameterType(), converterType);
    underTest.supports(parameter, parameter.getGenericParameterType(), converterType);

    Field cacheField = DefaultsRequestBodyAdvice.class.getDeclaredField("annotatedMemberCache");
    cacheField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Class<?>, Boolean> cache = (Map<Class<?>, Boolean>) cacheField.get(underTest);

    assertThat(cache).containsEntry(TestRecordWithOrg.class, true);
  }

  @Test
  void afterBodyRead_withRecord_injectsDefault() throws Exception {
    MethodParameter parameter = parameterOf("methodWithOrgBody", TestRecordWithOrg.class);
    TestRecordWithOrg body = new TestRecordWithOrg(null, "test-name");

    Object result = underTest.afterBodyRead(body, inputMessage, parameter, parameter.getGenericParameterType(), converterType);

    assertThat(result).isInstanceOf(TestRecordWithOrg.class);
    TestRecordWithOrg testRecord = (TestRecordWithOrg) result;
    assertThat(testRecord.orgId()).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(testRecord.name()).isEqualTo("test-name");
  }

  @Test
  void afterBodyRead_withClassField_injectsDefault() throws Exception {
    MethodParameter parameter = parameterOf("methodWithOrgClassBody", TestClassWithOrg.class);
    TestClassWithOrg body = new TestClassWithOrg();
    body.name = "test-name";

    Object result = underTest.afterBodyRead(body, inputMessage, parameter, parameter.getGenericParameterType(), converterType);

    assertThat(result).isSameAs(body);
    assertThat(body.orgId).isEqualTo(DefaultOrganizationProvider.ID.toString());
    assertThat(body.name).isEqualTo("test-name");
  }

  @Test
  void afterBodyRead_withNullBody_returnsNull() throws Exception {
    // A non-empty request body (e.g. the literal JSON "null") can still deserialize to null;
    // Spring calls afterBodyRead with that null rather than routing it through handleEmptyBody.
    MethodParameter parameter = parameterOf("methodWithOrgBody", TestRecordWithOrg.class);

    Object result = underTest.afterBodyRead(null, inputMessage, parameter, parameter.getGenericParameterType(), converterType);

    assertThat(result).isNull();
  }

  @Test
  void afterBodyRead_withOrgIdListField_overridesWithDefaultSingletonList() throws Exception {
    MethodParameter parameter = parameterOf("methodWithOrgIdListBody", TestRecordWithOrgList.class);
    TestRecordWithOrgList body = new TestRecordWithOrgList(List.of(), "test-name");

    Object result = underTest.afterBodyRead(body, inputMessage, parameter, parameter.getGenericParameterType(), converterType);

    assertThat(result).isInstanceOf(TestRecordWithOrgList.class);
    TestRecordWithOrgList testRecord = (TestRecordWithOrgList) result;
    assertThat(testRecord.orgIds()).isEqualTo(List.of(DefaultOrganizationProvider.ID));
    assertThat(testRecord.name()).isEqualTo("test-name");
  }

  @Test
  void afterBodyRead_whenInjectionFails_wrapsInIllegalStateException() throws Exception {
    MethodParameter parameter = parameterOf("methodWithThrowingBody", ThrowingOnDefaultRecord.class);
    // Constructs fine with any non-default value — only throws once the canonical constructor is
    // re-invoked with the injected default, i.e. from inside DefaultsInjector.injectDefaults.
    ThrowingOnDefaultRecord body = new ThrowingOnDefaultRecord("not-the-default");
    Type targetType = parameter.getGenericParameterType();

    assertThatThrownBy(() -> underTest.afterBodyRead(body, inputMessage, parameter, targetType, converterType))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to inject default values into @RequestBody parameter")
      .cause()
      .isInstanceOf(InvocationTargetException.class);
  }

  private static MethodParameter parameterOf(String methodName, Class<?> paramType) throws NoSuchMethodException {
    Method method = TestController.class.getMethod(methodName, paramType);
    return new MethodParameter(method, 0);
  }

  record TestRecordWithOrg(@OrganizationId String orgId, String name) {}

  record TestRecordWithoutAnnotations(String name) {}

  record TestRecordWithOrgList(@OrganizationId List<UUID> orgIds, String name) {}

  record ThrowingOnDefaultRecord(@OrganizationId String orgId) {
    ThrowingOnDefaultRecord {
      if (DefaultOrganizationProvider.ID.toString().equals(orgId)) {
        throw new IllegalStateException("boom");
      }
    }
  }

  static class TestClassWithOrg {
    @OrganizationId
    public String orgId;
    public String name;
  }

  static class TestController {
    public void methodWithOrgBody(@RequestBody TestRecordWithOrg body) {
      //empty for tests
    }

    public void methodWithoutAnnotationsBody(@RequestBody TestRecordWithoutAnnotations body) {
      //empty for tests
    }

    public void methodWithOrgClassBody(@RequestBody TestClassWithOrg body) {
      //empty for tests
    }

    public void methodWithOrgIdListBody(@RequestBody TestRecordWithOrgList body) {
      //empty for tests
    }

    public void methodWithThrowingBody(@RequestBody ThrowingOnDefaultRecord body) {
      //empty for tests
    }
  }
}

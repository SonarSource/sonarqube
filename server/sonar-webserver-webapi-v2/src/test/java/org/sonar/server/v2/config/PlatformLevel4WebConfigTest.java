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
package org.sonar.server.v2.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.api.config.Configuration;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.process.ProcessProperties;
import org.sonar.server.monitoring.ServerMonitoringMetrics;
import org.sonar.server.resolver.DefaultsRequestBodyAdvice;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.UserSession;
import org.sonarsource.organizations.api.rest.OrganizationId;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wiring test for {@link PlatformLevel4WebConfig#addInterceptors}.
 *
 * <p>Unit tests on {@code WebApiV2MetricsInterceptor} and {@code DeprecatedHandler}
 * prove the interceptor code is correct, not that it is wired into Spring MVC.
 * SONAR-27755 originally shipped with the interceptor unreachable because the
 * registration mechanism (a hand-rolled {@code RequestMappingHandlerMapping} bean)
 * was silently broken; the same trap had hidden a {@code DeprecatedHandler}
 * regression since Dec 2023. This test fires real requests through Spring's
 * {@code DispatcherServlet} and asserts the observable side effects.
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = PlatformLevel4WebConfigTest.TestConfig.class)
public class PlatformLevel4WebConfigTest {

  @Autowired
  private WebApplicationContext webApplicationContext;
  @Autowired
  private ServerMonitoringMetrics metrics;
  @Autowired
  private RecordingRequestBodyAdvice recordingRequestBodyAdvice;

  private MockMvc mockMvc;

  @Rule
  public LogTester logTester = new LogTester().setLevel(Level.WARN);

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  public void metricsInterceptor_isInvokedOnV2Request() throws Exception {
    mockMvc.perform(get("/test-wiring/ok")).andExpect(status().isOk());

    verify(metrics).observeWebApiV2RequestDuration(anyDouble(), eq("/test-wiring/ok"), eq("GET"));
  }

  @Test
  public void deprecatedHandler_logsWarningOnDeprecatedV2Endpoint() throws Exception {
    mockMvc.perform(get("/test-wiring/deprecated")).andExpect(status().isOk());

    assertThat(logTester.logs(Level.WARN))
      .anyMatch(msg -> msg.contains("Web service is deprecated since test"));
  }

  @Test
  public void defaultsArgumentResolverPrepender_injectsDefaultOrganizationId_whenParamAbsentFromRequest() throws Exception {
    // organizationId is @RequestParam(required=true) on the interface only — the BPP must prepend
    // DefaultsArgumentResolver before RequestParamMethodArgumentResolver, otherwise the missing
    // param would cause a 400 instead of being filled with the server-mode default.
    mockMvc.perform(get("/test-wiring/org-id"))
      .andExpect(status().isOk())
      .andExpect(content().string(DefaultOrganizationProvider.ID.toString()));
  }

  @Test
  public void defaultsArgumentResolverPrepender_injectsDefaultOrganizationId_intoRequestBodyBeforeValidation() throws Exception {
    // organizationId is @NotEmpty on the request body — if the BPP injected the default AFTER
    // validation instead of before, a missing organizationId would fail validation with a 400.
    mockMvc.perform(post("/test-wiring/org-id-body")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
      .andExpect(status().isOk())
      .andExpect(content().string(DefaultOrganizationProvider.ID.toString()));
  }

  @Test
  public void defaultsRequestBodyAdvice_runsBeforeOtherRequestBodyAdviceInTheChain() throws Exception {
    // RecordingRequestBodyAdvice also targets OrgBodyRequest. If DefaultsRequestBodyAdvice's
    // chain were bypassed (bug) it wouldn't run at all; if it ran but without @Order it could
    // run after RecordingRequestBodyAdvice and this would observe a null organizationId instead.
    mockMvc.perform(post("/test-wiring/org-id-body")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
      .andExpect(status().isOk());

    assertThat(recordingRequestBodyAdvice.received).hasSize(1);
    assertThat(recordingRequestBodyAdvice.received.get(0).organizationId()).isEqualTo(DefaultOrganizationProvider.ID.toString());
  }

  // Interface with @OrganizationId on the parameter — the concrete implementation does NOT repeat it.
  interface OrgApi {
    @GetMapping("/test-wiring/org-id")
    String getOrgId(@OrganizationId @RequestParam(value = "organizationId", required = true) String organizationId);
  }

  @RestController
  static class OrgController implements OrgApi {
    @Override
    public String getOrgId(String organizationId) {
      return organizationId;
    }
  }

  record OrgBodyRequest(@OrganizationId @NotEmpty String organizationId) {}

  @RestController
  static class OrgBodyController {
    @PostMapping("/test-wiring/org-id-body")
    public String postOrgId(@Valid @RequestBody OrgBodyRequest request) {
      return request.organizationId();
    }
  }

  // A second RequestBodyAdvice in the same chain as DefaultsRequestBodyAdvice, standing in for
  // a real-world advice (e.g. an audit-logging or business-rule advice) that must see the
  // already-defaulted value — proving both that the chain isn't bypassed and that @Order works.
  @ControllerAdvice
  static class RecordingRequestBodyAdvice extends RequestBodyAdviceAdapter {
    private final List<OrgBodyRequest> received = new ArrayList<>();

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
      return methodParameter.getParameterType() == OrgBodyRequest.class;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
      Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
      received.add((OrgBodyRequest) body);
      return body;
    }
  }

  @RestController
  static class TestController {
    @GetMapping("/test-wiring/ok")
    public String ok() {
      return "ok";
    }

    @Deprecated(since = "test")
    @GetMapping("/test-wiring/deprecated")
    public String deprecated() {
      return "deprecated";
    }
  }

  @org.springframework.context.annotation.Configuration
  @EnableWebMvc
  static class TestConfig {

    @Bean
    public ServerMonitoringMetrics serverMonitoringMetrics() {
      return mock(ServerMonitoringMetrics.class);
    }

    @Bean
    @Primary
    public UserSession userSession() {
      return new MockUserSession("test").setSystemAdministrator(true);
    }

    @Bean
    public Configuration configuration() {
      Configuration config = mock(Configuration.class);
      when(config.getBoolean(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey()))
        .thenReturn(Optional.of(true));
      return config;
    }

    @Bean
    public PlatformLevel4WebConfig platformLevel4WebConfig(UserSession userSession,
      ServerMonitoringMetrics metrics, Configuration config) {
      return new PlatformLevel4WebConfig(userSession, metrics, config);
    }

    @Bean
    public TestController testController() {
      return new TestController();
    }

    @Bean
    public OrgController orgController() {
      return new OrgController();
    }

    @Bean
    public OrgBodyController orgBodyController() {
      return new OrgBodyController();
    }

    @Bean
    public static BeanPostProcessor defaultsArgumentResolverPrepender() {
      return PlatformLevel4WebConfig.defaultsArgumentResolverPrepender();
    }

    @Bean
    public DefaultsRequestBodyAdvice defaultsRequestBodyAdvice() {
      return new DefaultsRequestBodyAdvice();
    }

    @Bean
    public RecordingRequestBodyAdvice recordingRequestBodyAdvice() {
      return new RecordingRequestBodyAdvice();
    }
  }
}

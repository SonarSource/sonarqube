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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.server.v2.common.ServerRestResponseEntityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class CommonWebConfigTest {

  @Test
  public void customOpenAPI_shouldIncludeNonNullVersion() {
    Version expectedVersion = Version.parse("1.0.0");
    try (MockedStatic<MetadataLoader> metadataLoaderMock = mockStatic(MetadataLoader.class)) {
      metadataLoaderMock.when(() -> MetadataLoader.loadSQVersion(System2.INSTANCE)).thenReturn(expectedVersion);

      var serverWebConfig = new ServerWebConfig();
      var info = serverWebConfig.customOpenAPI().getInfo();

      assertThat(info.getVersion()).isNotNull();
      assertThat(info.getDescription()).isEqualTo("""
        The SonarQube API v2 is a REST API which enables you to interact with SonarQube programmatically.
        While not all endpoints of the former Web API are available yet, the ones available are stable and can be used in production environments.
        """);
      assertThat(info.getVersion()).isEqualTo(expectedVersion.toString());
    }
  }

  @Test
  public void nativeMethodValidation_shouldReturnBadRequestForInvalidParameter() throws Exception {
    try (var context = new AnnotationConfigWebApplicationContext()) {
      context.setServletContext(new MockServletContext());
      context.register(NativeValidationConfiguration.class, TestControllerConfiguration.class);
      context.refresh();

      MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

      mockMvc.perform(get("/test").param("pageSize", "5001"))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"pageSize: must be less than or equal to 5000\"}"));
    }
  }

  @Test
  public void nativeMethodValidation_shouldReturnAllInvalidParametersInDeterministicOrder() throws Exception {
    try (var context = new AnnotationConfigWebApplicationContext()) {
      context.setServletContext(new MockServletContext());
      context.register(NativeValidationConfiguration.class, TestControllerConfiguration.class);
      context.refresh();

      MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

      mockMvc.perform(get("/test")
          .param("pageIndex", "0")
          .param("pageSize", "5001"))
        .andExpect(status().isBadRequest())
        .andExpect(content().json("{\"message\":\"pageIndex: must be greater than or equal to 1, pageSize: must be less than or equal to 5000\"}"));
    }
  }

  @Configuration
  @EnableWebMvc
  static class NativeValidationConfiguration {
    @Bean
    CommonWebConfig commonWebConfig() {
      return new CommonWebConfig();
    }

    @Bean
    ServerRestResponseEntityExceptionHandler serverRestResponseEntityExceptionHandler() {
      return new ServerRestResponseEntityExceptionHandler();
    }
  }

  @Configuration
  static class TestControllerConfiguration {
    @Bean
    TestController testController() {
      return new TestController();
    }
  }

  @RequestMapping("/test")
  interface TestApi {
    @GetMapping
    ResponseEntity<Void> get(
      @RequestParam(value = "pageIndex", required = false) @Min(1) Integer pageIndex,
      @RequestParam("pageSize") @Max(5000) Integer pageSize);
  }

  @RequestMapping("/test")
  @RestController
  public static class TestController implements TestApi {
    @GetMapping
    @Override
    public ResponseEntity<Void> get(Integer pageIndex, Integer pageSize) {
      return ResponseEntity.ok().build();
    }
  }

}

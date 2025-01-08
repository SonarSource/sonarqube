/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.v2.api;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.sonar.server.v2.common.RestResponseEntityExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class ControllerTester {
  public static MockMvc getMockMvc(Object... controllers) {
    return getMockMvcWithHandlerInterceptors(null, controllers);
  }

  public static MockMvc getMockMvcWithHandlerInterceptors(List<HandlerInterceptor> handlerInterceptors, Object... controllers) {
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
      .configure()
      .messageInterpolator(new ParameterMessageInterpolator())
      .buildValidatorFactory();
    return MockMvcBuilders
      .standaloneSetup(controllers)
      .setValidator(new SpringValidatorAdapter(validatorFactory.getValidator()))
      .setCustomHandlerMapping(() -> resolveRequestMappingHandlerMapping(handlerInterceptors))
      .setControllerAdvice(new RestResponseEntityExceptionHandler())
      .setUseTrailingSlashPatternMatch(true)
      .build();
  }

  private static RequestMappingHandlerMapping resolveRequestMappingHandlerMapping(List<HandlerInterceptor> handlerInterceptors) {
    RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
    handlerMapping.setInterceptors(handlerInterceptors != null ? handlerInterceptors.toArray() : new Object[0]);
    return handlerMapping;
  }
}

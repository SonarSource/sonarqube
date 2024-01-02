/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.List;
import org.sonar.server.v2.common.RestResponseEntityExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class ControllerTester {
  public static MockMvc getMockMvc(Object... controllers) {
    return getMockMvcWithHandlerInterceptors(null, controllers);
  }

  public static MockMvc getMockMvcWithHandlerInterceptors(List<HandlerInterceptor> handlerInterceptors, Object... controllers) {
    return MockMvcBuilders
      .standaloneSetup(controllers)
      .setCustomHandlerMapping(() -> resolveRequestMappingHandlerMapping(handlerInterceptors))
      .setControllerAdvice(new RestResponseEntityExceptionHandler())
      .build();
  }

  private static RequestMappingHandlerMapping resolveRequestMappingHandlerMapping(List<HandlerInterceptor> handlerInterceptors) {
    RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
    handlerMapping.setInterceptors(handlerInterceptors != null ? handlerInterceptors.toArray() : new Object[0]);
    return handlerMapping;
  }
}

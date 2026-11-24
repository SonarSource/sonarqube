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
package org.sonar.server.v2.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerRestResponseEntityExceptionHandlerHeaderBindingTest {

  private final StrictHttpFirewall firewall = new StrictHttpFirewall();
  private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
    .setControllerAdvice(new ServerRestResponseEntityExceptionHandler())
    .addFilter((request, response, chain) -> chain.doFilter(firewall.getFirewalledRequest((HttpServletRequest) request), response))
    .build();

  @Test
  void requestSucceedsWhenHeaderValueIsRejectedByFirewall() throws Exception {
    // Tomcat decodes the UTF-8 bytes C4 81 sent by Cloudflare as ISO-8859-1, which yields a C1 control character
    mockMvc.perform(get("/test").param("name", "foo").header("Cf-Ipcity", "Naini TÄ\u0081l"))
      .andExpect(status().isOk())
      .andExpect(content().string("foo"));
  }

  public record Query(String name) {
  }

  @RestController
  @RequestMapping("/test")
  public static class TestController {
    @GetMapping
    public String get(Query query) {
      return query.name();
    }
  }
}

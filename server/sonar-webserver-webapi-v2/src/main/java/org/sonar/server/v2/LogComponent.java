/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2;

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
public class LogComponent implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = Loggers.get(LogComponent.class);
  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    ApplicationContext applicationContext = event.getApplicationContext();
    List<Integer> isUseless = applicationContext.getBeansOfType(RequestMappingHandlerMapping.class).values().stream()
      .map(AbstractHandlerMethodMapping::getHandlerMethods)
      .map(d->{
        d.forEach((e, c)-> LOGGER.info("Registered endpoint: "+e.getName()+" "+e.getDirectPaths()+" "+e));
        return 1;
      })
      .collect(Collectors.toList());
  }
}

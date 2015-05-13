/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.web;


/**
 * Interface to create a web service by implementing a Ruby On Rails controller.
 * The method getTemplate() return the ROR controller code, the name of the controller class defined in the template
 * MUST match the following name scheme : Api::$Webservice.getId()Controller I.E : Webservice.getId() = TestWS > Api::TestWSController.
 * The plugin will be deployed with the following URL scheme: http://sonarhost/api/plugins/$Webservice.getId()/:action/:id
 * :action is the name of the controller method to call, :id is a param that will be passed to the controller, these 2 params are not mandatory
 * and will call the index controller method if not specified.
 *
 * @since 1.11
 * @deprecated in 4.2. Replaced by {@link org.sonar.api.server.ws.WebService}
 */
@Deprecated
public interface RubyRailsWebservice extends Webservice {

  /**
   * @return Content of the Ruby on Rails web service controller class
   */
  String getTemplate();
}

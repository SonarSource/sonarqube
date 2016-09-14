/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.server.ws;

/**
 * Used to declare web services that are still implemented in Ruby on Rails.
 *
 * @since 4.4
 */
public class RailsHandler implements RequestHandler {

  public static final RequestHandler INSTANCE = new RailsHandler();

  private RailsHandler() {
    // Nothing
  }

  @Override
  public void handle(Request request, Response response) {
    throw new UnsupportedOperationException("This web service is implemented in rails");
  }

  public static WebService.NewParam addFormatParam(WebService.NewAction action) {
    return action.createParam("format")
      .setDescription("Response format can be set through:" +
        "<ul>" +
        "<li>Parameter format: xml | json</li>" +
        "<li>Or the 'Accept' property in the HTTP header:" +
        "<ul>" +
        "<li>Accept:text/xml</li>" +
        "<li>Accept:application/json</li>" +
        "</ul></li></ul>" +
        "If nothing is set, json is used.<br/>" +
        "Since 6.1, XML format is deprecated, only JSON format should be used.")
      .setPossibleValues("json", "xml")
      .setDeprecatedSince("6.1");
  }

  public static WebService.NewParam addJsonOnlyFormatParam(WebService.NewAction action) {
    return action.createParam("format")
      .setDescription("Only json response format is available")
      .setPossibleValues("json");
  }

}

/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.organization.ws;

import javax.annotation.CheckForNull;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.organization.OrganizationValidation;
import org.sonarqube.ws.Organizations.Organization;

import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.organization.OrganizationValidation.DESCRIPTION_MAX_LENGTH;
import static org.sonar.server.organization.OrganizationValidation.NAME_MAX_LENGTH;
import static org.sonar.server.organization.OrganizationValidation.URL_MAX_LENGTH;

/**
 * Factorizes code and constants between Organization WS's actions.
 */
public class OrganizationsWsSupport {
  static final String PARAM_ORGANIZATION = "organization";
  static final String PARAM_KEY = "key";
  static final String PARAM_NAME = "name";
  static final String PARAM_DESCRIPTION = "description";
  static final String PARAM_URL = "url";
  static final String PARAM_AVATAR_URL = "avatar";

  static final String PARAM_LOGIN = "login";

  private final OrganizationValidation organizationValidation;

  public OrganizationsWsSupport(OrganizationValidation organizationValidation) {
    this.organizationValidation = organizationValidation;
  }

  String getAndCheckMandatoryName(Request request) {
    String name = request.mandatoryParam(PARAM_NAME);
    organizationValidation.checkName(name);
    return name;
  }

  @CheckForNull
  String getAndCheckName(Request request) {
    String name = request.param(PARAM_NAME);
    if (name != null) {
      organizationValidation.checkName(name);
    }
    return name;
  }

  @CheckForNull
  String getAndCheckAvatar(Request request) {
    return organizationValidation.checkAvatar(request.param(PARAM_AVATAR_URL));
  }

  @CheckForNull
  String getAndCheckUrl(Request request) {
    return organizationValidation.checkUrl(request.param(PARAM_URL));
  }

  @CheckForNull
  String getAndCheckDescription(Request request) {
    return organizationValidation.checkDescription(request.param(PARAM_DESCRIPTION));
  }

  void addOrganizationDetailsParams(WebService.NewAction action, boolean isNameRequired) {
    action.createParam(PARAM_NAME)
      .setRequired(isNameRequired)
      .setMaximumLength(NAME_MAX_LENGTH)
      .setDescription("Name of the organization. <br />" +
        "It must be between 2 and 64 chars longs.")
      .setExampleValue("Foo Company");

    action.createParam(PARAM_DESCRIPTION)
      .setRequired(false)
      .setMaximumLength(DESCRIPTION_MAX_LENGTH)
      .setDescription("Description of the organization.<br/> It must be less than 256 chars long.")
      .setExampleValue("The Foo company produces quality software for Bar.");

    action.createParam(PARAM_URL)
      .setRequired(false)
      .setMaximumLength(URL_MAX_LENGTH)
      .setDescription("URL of the organization.<br/> It must be less than 256 chars long.")
      .setExampleValue("https://www.foo.com");

    action.createParam(PARAM_AVATAR_URL)
      .setRequired(false)
      .setMaximumLength(URL_MAX_LENGTH)
      .setDescription("URL of the organization avatar.<br/> It must be less than 256 chars long.")
      .setExampleValue("https://www.foo.com/foo.png");
  }

  Organization.Builder toOrganization(OrganizationDto dto) {
    return toOrganization(Organization.newBuilder(), dto);
  }

  Organization.Builder toOrganization(Organization.Builder builder, OrganizationDto dto) {
    builder
      .setName(dto.getName())
      .setKey(dto.getKey())
      .setGuarded(dto.isGuarded());
    setNullable(dto.getDescription(), builder::setDescription);
    setNullable(dto.getUrl(), builder::setUrl);
    setNullable(dto.getAvatarUrl(), builder::setAvatar);
    return builder;
  }
}

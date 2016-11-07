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
package org.sonar.server.organization.ws;

import javax.annotation.CheckForNull;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.organization.OrganizationDto;
import org.sonarqube.ws.Organizations;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Factorizes code and constants between Organization WS's actions.
 */
public class OrganizationsWsSupport {
  static final String PARAM_KEY = "key";
  static final String PARAM_NAME = "name";
  static final String PARAM_DESCRIPTION = "description";
  static final String PARAM_URL = "url";
  static final String PARAM_AVATAR_URL = "avatar";
  static final int KEY_MIN_LENGTH = 2;
  static final int KEY_MAX_LENGTH = 32;
  static final int NAME_MIN_LENGTH = 2;
  static final int NAME_MAX_LENGTH = 64;
  static final int DESCRIPTION_MAX_LENGTH = 256;
  static final int URL_MAX_LENGTH = 256;

  String getAndCheckMandatoryName(Request request) {
    String name = request.mandatoryParam(PARAM_NAME);
    checkName(name);
    return name;
  }

  @CheckForNull
  String getAndCheckName(Request request) {
    String name = request.param(PARAM_NAME);
    if (name != null) {
      checkName(name);
    }
    return name;
  }

  private static void checkName(String name) {
    checkArgument(name.length() >= NAME_MIN_LENGTH, "Name '%s' must be at least %s chars long", name, NAME_MIN_LENGTH);
    checkArgument(name.length() <= NAME_MAX_LENGTH, "Name '%s' must be at most %s chars long", name, NAME_MAX_LENGTH);
  }

  @CheckForNull
  String getAndCheckAvatar(Request request) {
    return getAndCheckParamMaxLength(request, PARAM_AVATAR_URL, URL_MAX_LENGTH);
  }

  @CheckForNull
  String getAndCheckUrl(Request request) {
    return getAndCheckParamMaxLength(request, PARAM_URL, URL_MAX_LENGTH);
  }

  @CheckForNull
  String getAndCheckDescription(Request request) {
    return getAndCheckParamMaxLength(request, PARAM_DESCRIPTION, DESCRIPTION_MAX_LENGTH);
  }

  @CheckForNull
  private static String getAndCheckParamMaxLength(Request request, String key, int maxLength) {
    String value = request.param(key);
    if (value != null) {
      checkArgument(value.length() <= maxLength, "%s '%s' must be at most %s chars long", key, value, maxLength);
    }
    return value;
  }

  void addOrganizationDetailsParams(WebService.NewAction action, boolean isNameRequired) {
    action.createParam(PARAM_NAME)
      .setRequired(isNameRequired)
      .setDescription("Name of the organization. <br />" +
        "It must be between 2 and 64 chars longs.")
      .setExampleValue("Foo Company");

    action.createParam(PARAM_DESCRIPTION)
      .setRequired(false)
      .setDescription("Description of the organization.<br/> It must be less than 256 chars long.")
      .setExampleValue("The Foo company produces quality software for Bar.");

    action.createParam(PARAM_URL)
      .setRequired(false)
      .setDescription("URL of the organization.<br/> It must be less than 256 chars long.")
      .setExampleValue("https://www.foo.com");

    action.createParam(PARAM_AVATAR_URL)
      .setRequired(false)
      .setDescription("URL of the organization avatar.<br/> It must be less than 256 chars long.")
      .setExampleValue("https://www.foo.com/foo.png");
  }

  Organizations.Organization toOrganization(OrganizationDto dto) {
    return toOrganization(Organizations.Organization.newBuilder(), dto);
  }

  Organizations.Organization toOrganization(Organizations.Organization.Builder builder, OrganizationDto dto) {
    builder
      .clear()
      .setName(dto.getName())
      .setKey(dto.getKey());
    if (dto.getDescription() != null) {
      builder.setDescription(dto.getDescription());
    }
    if (dto.getUrl() != null) {
      builder.setUrl(dto.getUrl());
    }
    if (dto.getAvatarUrl() != null) {
      builder.setAvatar(dto.getAvatarUrl());
    }
    return builder.build();
  }
}

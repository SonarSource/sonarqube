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
package org.sonar.server.organization;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.Slug.slugify;

public class OrganizationValidationImpl implements OrganizationValidation {

  @Override
  public String checkKey(String keyCandidate) {
    requireNonNull(keyCandidate, "key can't be null");
    checkArgument(keyCandidate.length() >= KEY_MIN_LENGTH, "Key '%s' must be at least %s chars long", keyCandidate, KEY_MIN_LENGTH);
    checkArgument(keyCandidate.length() <= KEY_MAX_LENGTH, "Key '%s' must be at most %s chars long", keyCandidate, KEY_MAX_LENGTH);
    checkArgument(slugify(keyCandidate).equals(keyCandidate), "Key '%s' contains at least one invalid char", keyCandidate);

    return keyCandidate;
  }

  @Override
  public String checkName(String nameCandidate) {
    requireNonNull(nameCandidate, "name can't be null");

    checkArgument(nameCandidate.length() >= NAME_MIN_LENGTH, "Name '%s' must be at least %s chars long", nameCandidate, NAME_MIN_LENGTH);
    checkArgument(nameCandidate.length() <= NAME_MAX_LENGTH, "Name '%s' must be at most %s chars long", nameCandidate, NAME_MAX_LENGTH);

    return nameCandidate;
  }

  @Override
  public String checkDescription(@Nullable String descriptionCandidate) {
    checkParamMaxLength(descriptionCandidate, "Description", DESCRIPTION_MAX_LENGTH);

    return descriptionCandidate;
  }

  @Override
  public String checkUrl(@Nullable String urlCandidate) {
    checkParamMaxLength(urlCandidate, "Url", URL_MAX_LENGTH);

    return urlCandidate;
  }

  @Override
  public String checkAvatar(@Nullable String avatarCandidate) {
    checkParamMaxLength(avatarCandidate, "Avatar", URL_MAX_LENGTH);

    return avatarCandidate;
  }

  @CheckForNull
  private static void checkParamMaxLength(@Nullable String value, String label, int maxLength) {
    if (value != null) {
      checkArgument(value.length() <= maxLength, "%s '%s' must be at most %s chars long", label, value, maxLength);
    }
  }

  @Override
  public String generateKeyFrom(String source) {
    return slugify(source.substring(0, min(source.length(), KEY_MAX_LENGTH)));
  }
}

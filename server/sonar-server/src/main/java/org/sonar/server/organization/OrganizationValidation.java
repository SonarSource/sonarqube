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

public interface OrganizationValidation {
  int KEY_MIN_LENGTH = 2;
  int KEY_MAX_LENGTH = 32;
  int NAME_MIN_LENGTH = 2;
  int NAME_MAX_LENGTH = 64;
  int DESCRIPTION_MAX_LENGTH = 256;
  int URL_MAX_LENGTH = 256;

  /**
   * Ensures the specified argument is a valid key by failing with an exception if it is not so.
   * <p>
   * A valid key is non null and its length is between {@link #KEY_MIN_LENGTH 2} and {@link #KEY_MAX_LENGTH 32}.
   * </p>
   *
   * @return the argument
   *
   * @throws NullPointerException if argument is {@code null}.
   * @throws IllegalArgumentException if argument is not a valid key.
   */
  String checkKey(String keyCandidate);

  /**
   * Ensures the specified argument is a valid name by failing with an exception if it is not so.
   * <p>
   * A valid name is non null and its length is between {@link #NAME_MIN_LENGTH 2} and {@link #NAME_MAX_LENGTH 64}.
   * </p>
   *
   * @return the argument
   *
   * @throws NullPointerException if argument is {@code null}.
   * @throws IllegalArgumentException if argument is not a valid name.
   */
  String checkName(String nameCandidate);

  /**
   * Ensures the specified argument is either {@code null}, empty or a valid description by failing with an exception
   * if it is not so.
   * <p>
   * The length of a valid url can't be more than {@link #DESCRIPTION_MAX_LENGTH 256}.
   * </p>
   *
   * @return the argument
   *
   * @throws IllegalArgumentException if argument is not a valid description.
   */
  @CheckForNull
  String checkDescription(@Nullable String descriptionCandidate);

  /**
   * Ensures the specified argument is either {@code null}, empty or a valid URL by failing with an exception if it is
   * not so.
   * <p>
   * The length of a valid URL can't be more than {@link #URL_MAX_LENGTH 256}.
   * </p>
   *
   * @return the argument
   *
   * @throws IllegalArgumentException if argument is not a valid url.
   */
  @CheckForNull
  String checkUrl(@Nullable String urlCandidate);

  /**
   * Ensures the specified argument is either {@code null}, empty or a valid avatar URL by failing with an exception if
   * it is not so.
   * <p>
   * The length of a valid avatar URL can't be more than {@link #URL_MAX_LENGTH 256}.
   * </p>
   *
   * @return the argument
   *
   * @throws IllegalArgumentException if argument is not a valid avatar url.
   */
  @CheckForNull
  String checkAvatar(@Nullable String avatarCandidate);

  /**
   * Transforms the specified string into a valid key.
   *
   * @see #checkKey(String)
   */
  String generateKeyFrom(String source);
}

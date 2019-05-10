/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.issue;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * @since 3.6
 */
public interface DefaultTransitions {

  String CONFIRM = "confirm";
  String UNCONFIRM = "unconfirm";
  String REOPEN = "reopen";
  String RESOLVE = "resolve";
  String FALSE_POSITIVE = "falsepositive";
  String CLOSE = "close";

  /**
   * @since 5.1
   */
  String WONT_FIX = "wontfix";

  /**
   * @since 7.8
   */
  String SET_AS_IN_REVIEW = "setinreview";

  /**
   * @since 7.8
   */
  String RESOLVE_AS_REVIEWED = "resolveasreviewed";

  /**
   * @since 7.8
   */
  String OPEN_AS_VULNERABILITY = "openasvulnerability";

  /**
   * @since 7.8
   */
  String RESET_AS_TO_REVIEW = "resetastoreview";

  /**
   * @since 4.4
   */
  List<String> ALL = unmodifiableList(asList(CONFIRM, UNCONFIRM, REOPEN, RESOLVE, FALSE_POSITIVE, WONT_FIX, CLOSE,
    SET_AS_IN_REVIEW, RESOLVE_AS_REVIEWED, OPEN_AS_VULNERABILITY,RESET_AS_TO_REVIEW));
}

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
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class RulesWsParameters {
  public static final String PARAM_REPOSITORIES = "repositories";
  public static final String PARAM_RULE_KEY = "rule_key";
  public static final String PARAM_ACTIVATION = "activation";
  public static final String PARAM_QPROFILE = "qprofile";
  public static final String PARAM_SEVERITIES = "severities";
  public static final String PARAM_AVAILABLE_SINCE = "available_since";
  public static final String PARAM_STATUSES = "statuses";
  public static final String PARAM_LANGUAGES = "languages";
  public static final String PARAM_TAGS = "tags";
  public static final String PARAM_TYPES = "types";
  public static final String PARAM_CWE = "cwe";
  public static final String PARAM_OWASP_TOP_10 = "owaspTop10";
  public static final String PARAM_SANS_TOP_25 = "sansTop25";
  public static final String PARAM_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  public static final String PARAM_INHERITANCE = "inheritance";
  public static final String PARAM_ACTIVE_SEVERITIES = "active_severities";
  public static final String PARAM_IS_TEMPLATE = "is_template";
  public static final String PARAM_INCLUDE_EXTERNAL = "include_external";
  public static final String PARAM_TEMPLATE_KEY = "template_key";
  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_COMPARE_TO_PROFILE = "compareToProfile";

  public static final String FIELD_REPO = "repo";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_UPDATED_AT = "updatedAt";
  public static final String FIELD_SEVERITY = "severity";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_INTERNAL_KEY = "internalKey";
  public static final String FIELD_IS_EXTERNAL = "isExternal";
  public static final String FIELD_IS_TEMPLATE = "isTemplate";
  public static final String FIELD_TEMPLATE_KEY = "templateKey";
  public static final String FIELD_TAGS = "tags";
  public static final String FIELD_SYSTEM_TAGS = "sysTags";
  public static final String FIELD_LANGUAGE = "lang";
  public static final String FIELD_LANGUAGE_NAME = "langName";
  public static final String FIELD_HTML_DESCRIPTION = "htmlDesc";
  public static final String FIELD_MARKDOWN_DESCRIPTION = "mdDesc";
  public static final String FIELD_NOTE_LOGIN = "noteLogin";
  public static final String FIELD_MARKDOWN_NOTE = "mdNote";
  public static final String FIELD_HTML_NOTE = "htmlNote";

  /**
   * @deprecated since 5.5, replaced by {@link #FIELD_DEFAULT_REM_FUNCTION}
   */
  @Deprecated
  public static final String FIELD_DEFAULT_DEBT_REM_FUNCTION = "defaultDebtRemFn";
  public static final String FIELD_DEFAULT_REM_FUNCTION = "defaultRemFn";

  /**
   * @deprecated since 5.5, replaced by {@link #FIELD_REM_FUNCTION}
   */
  @Deprecated
  public static final String FIELD_DEBT_REM_FUNCTION = "debtRemFn";
  public static final String FIELD_REM_FUNCTION = "remFn";

  /**
   * @deprecated since 5.5, replaced by {@link #FIELD_GAP_DESCRIPTION}
   */
  @Deprecated
  public static final String FIELD_EFFORT_TO_FIX_DESCRIPTION = "effortToFixDescription";
  public static final String FIELD_GAP_DESCRIPTION = "gapDescription";

  /**
   * @deprecated since 5.5, replaced by {@link #FIELD_REM_FUNCTION_OVERLOADED}
   */
  @Deprecated
  public static final String FIELD_DEBT_OVERLOADED = "debtOverloaded";
  public static final String FIELD_REM_FUNCTION_OVERLOADED = "remFnOverloaded";

  /**
   * @since 7.1
   */
  public static final String FIELD_SCOPE = "scope";

  public static final String FIELD_PARAMS = "params";
  public static final String FIELD_ACTIVES = "actives";

  public static final Set<String> OPTIONAL_FIELDS = ImmutableSet.of(FIELD_REPO, FIELD_NAME, FIELD_CREATED_AT, FIELD_UPDATED_AT, FIELD_SEVERITY, FIELD_STATUS, FIELD_INTERNAL_KEY,
    FIELD_IS_EXTERNAL, FIELD_IS_TEMPLATE, FIELD_TEMPLATE_KEY, FIELD_TAGS, FIELD_SYSTEM_TAGS, FIELD_LANGUAGE, FIELD_LANGUAGE_NAME, FIELD_HTML_DESCRIPTION,
    FIELD_MARKDOWN_DESCRIPTION, FIELD_NOTE_LOGIN, FIELD_MARKDOWN_NOTE, FIELD_HTML_NOTE,
    FIELD_DEFAULT_DEBT_REM_FUNCTION, FIELD_EFFORT_TO_FIX_DESCRIPTION, FIELD_DEBT_OVERLOADED, FIELD_DEBT_REM_FUNCTION,
    FIELD_DEFAULT_REM_FUNCTION, FIELD_GAP_DESCRIPTION, FIELD_REM_FUNCTION_OVERLOADED, FIELD_REM_FUNCTION,
    FIELD_PARAMS, FIELD_ACTIVES, FIELD_SCOPE);

  private RulesWsParameters() {
    // prevent instantiation
  }
}

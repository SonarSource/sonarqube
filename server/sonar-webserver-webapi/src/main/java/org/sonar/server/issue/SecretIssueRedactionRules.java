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
package org.sonar.server.issue;

import java.util.Set;
import org.sonar.db.issue.IssueDto;

/**
 * Defines secret-rule handling policy: {@code secrets:*} uses trusted ranges, {@code S2068} and {@code S6418} redact whole source responses,
 * and {@code S6437} redacts its primary and flow locations when available.
 */
public final class SecretIssueRedactionRules {
  private static final String SECRETS_REPOSITORY = "secrets";
  private static final String S2068_RULE_KEY = "S2068";
  private static final String S6418_RULE_KEY = "S6418";
  private static final String S6437_RULE_KEY = "S6437";

  private static final Set<String> FLOW_BASED_SOURCE_RULE_KEYS = Set.of(S6437_RULE_KEY);
  private static final Set<String> SOURCE_REDACTION_RULE_KEYS = Set.of(S2068_RULE_KEY, S6418_RULE_KEY, S6437_RULE_KEY);

  private SecretIssueRedactionRules() {
  }

  public static Set<String> sourceRedactionRuleKeys() {
    return SOURCE_REDACTION_RULE_KEYS;
  }

  static boolean isFlowBasedSourceRule(IssueDto issue) {
    return FLOW_BASED_SOURCE_RULE_KEYS.contains(issue.getRuleKey().rule());
  }

  static boolean isSecretIssue(IssueDto issue) {
    return SECRETS_REPOSITORY.equals(issue.getRuleRepo()) || SOURCE_REDACTION_RULE_KEYS.contains(issue.getRuleKey().rule());
  }

  static boolean hasTrustedSourceRange(IssueDto issue) {
    return SECRETS_REPOSITORY.equals(issue.getRuleRepo());
  }

}

/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.report;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;

@ScannerSide
@Immutable
public class RuleNameProvider {
  private Rules rules;

  public RuleNameProvider(Rules rules) {
    this.rules = rules;
  }

  @CheckForNull
  private String nameFromDB(RuleKey ruleKey) {
    Rule r = rules.find(ruleKey);
    return r != null ? r.name() : null;
  }

  public String nameForHTML(RuleKey ruleKey) {
    String name = nameFromDB(ruleKey);
    return StringEscapeUtils.escapeHtml(name != null ? name : ruleKey.toString());
  }

  public String nameForJS(String ruleKey) {
    String name = nameFromDB(RuleKey.parse(ruleKey));
    return StringEscapeUtils.escapeJavaScript(name != null ? name : ruleKey);
  }

  public String nameForHTML(Rule rule) {
    return StringEscapeUtils.escapeHtml(rule.name());
  }

}

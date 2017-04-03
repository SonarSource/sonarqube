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
package org.sonar.server.rule.index;

import com.google.common.collect.Maps;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.RuleTesting;

public class RuleDocTesting {

  public static RuleDoc newDoc() {
    return newDoc(RuleTesting.XOO_X1);
  }

  public static RuleDoc newDoc(RuleKey ruleKey) {
    return new RuleDoc(Maps.<String, Object>newHashMap())
      .setKey(ruleKey.toString())
      .setRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setName("Name " + ruleKey.toString())
      .setHtmlDescription("Description " + ruleKey.rule())
      .setSeverity(Severity.CRITICAL)
      .setStatus(RuleStatus.READY.name())
      .setLanguage("xoo")
      .setIsTemplate(false)
      .setType(RuleType.CODE_SMELL)
      .setCreatedAt(1_500_000_000L)
      .setUpdatedAt(1_600_000_000L);
  }
}

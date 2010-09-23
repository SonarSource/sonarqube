/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.checks.checkers;

import com.google.common.collect.Maps;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.profiles.Check;
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.check.Message;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public class MessageDispatcher {

  private Map<Check, Object> checkersByCheck;
  private Map<Object, Check> checksByChecker;
  private SensorContext context;

  public MessageDispatcher(SensorContext context) {
    this.context = context;
    checkersByCheck = Maps.newIdentityHashMap();
    checksByChecker = Maps.newIdentityHashMap();
  }

  public void registerChecker(Check check, Object checker) {
    checkersByCheck.put(check, checker);
    checksByChecker.put(checker, check);
  }

  public void registerCheckers(CheckerFactory factory) {
    Map<Check, Object> map = factory.create();
    for (Map.Entry<Check, Object> entry : map.entrySet()) {
      registerChecker(entry.getKey(), entry.getValue());
    }
  }

  public void registerCheckers(CheckProfile profile) {
    for (Check check : profile.getChecks()) {
      registerChecker(check, check);
    }
  }

  public Object getChecker(Check check) {
    return checkersByCheck.get(check);
  }

  public Check getCheck(Object checker) {
    return checksByChecker.get(checker);
  }

  public Collection getCheckers() {
    return checkersByCheck.values();
  }

  public void unregisterCheck(Check check) {
    Object checker = checkersByCheck.remove(check);
    if (checker != null) {
      checksByChecker.remove(checker);
    }
  }

  public void unregisterChecks(CheckProfile profile) {
    for (Check check : profile.getChecks()) {
      unregisterCheck(check);
    }
  }

  public void log(Resource resource, Message message) {
    Object checker = message.getChecker();
    Check check = getCheck(checker);
    Violation violation = Violation.create(new Rule(check.getRepositoryKey(), check.getTemplateKey()), resource);
    violation.setLineId(message.getLine());
    violation.setMessage(message.getText(Locale.ENGLISH));
    violation.setPriority(RulePriority.fromCheckPriority(check.getPriority()));
    context.saveViolation(violation);
  }

  public void clear() {
    checkersByCheck.clear();
    checksByChecker.clear();
  }

}
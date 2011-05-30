/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.squid.bridges;

import java.util.Locale;
import java.util.Set;

import org.sonar.api.resources.Resource;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

public class ChecksBridge extends Bridge {

  protected ChecksBridge() {
    super(false);
  }

  @Override
  public void onFile(SourceFile squidFile, Resource sonarFile) {
    Set<CheckMessage> messages = squidFile.getCheckMessages();
    if (messages != null) {
      for (CheckMessage checkMessage : messages) {
        ActiveRule rule = checkFactory.getActiveRule(checkMessage.getChecker());
        Violation violation = Violation.create(rule, sonarFile);
        violation.setLineId(checkMessage.getLine());
        violation.setMessage(checkMessage.getText(Locale.ENGLISH));
        violation.setCost(checkMessage.getCost());
        context.saveViolation(violation, checkMessage.isBypassExclusion());
      }
    }
  }
}

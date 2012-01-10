/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.findbugs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

public final class FindbugsRuleRepository extends RuleRepository {

  private XMLRuleParser xmlRuleParser;

  private ServerFileSystem fileSystem;

  public FindbugsRuleRepository(ServerFileSystem fileSystem, XMLRuleParser xmlRuleParser) {
    super(FindbugsConstants.REPOSITORY_KEY, Java.KEY);
    setName(FindbugsConstants.REPOSITORY_NAME);
    this.xmlRuleParser = xmlRuleParser;
    this.fileSystem = fileSystem;
  }

  @Override
  public List<Rule> createRules() {
    List<Rule> rules = new ArrayList<Rule>();
    rules.addAll(xmlRuleParser.parse(getClass().getResourceAsStream("/org/sonar/plugins/findbugs/rules.xml")));
    for (File userExtensionXml : fileSystem.getExtensions(FindbugsConstants.REPOSITORY_KEY, "xml")) {
      rules.addAll(xmlRuleParser.parse(userExtensionXml));
    }
    return rules;
  }
}

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
package org.sonar.api.rules;

import org.apache.commons.io.IOUtils;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.SonarException;

import java.io.InputStream;
import java.util.List;

@Deprecated
public abstract class AbstractRulesRepository<LANG extends Language, MAPPER extends RulePriorityMapper<?, ?>> implements RulesRepository<LANG> {

  private MAPPER priorityMapper;
  private LANG language;

  public AbstractRulesRepository(LANG language, MAPPER priorityMapper) {
    super();
    this.priorityMapper = priorityMapper;
    this.language = language;
  }

  public LANG getLanguage() {
    return language;
  }

  public abstract String getRepositoryResourcesBase();

  public final List<Rule> getInitialReferential() {
    String baseCP = getCheckResourcesBase();
    InputStream input = getClass().getResourceAsStream(baseCP + "rules.xml");
    if (input == null) {
      throw new SonarException("Resource not found : " + baseCP + "rules.xml");
    }
    try {
      return new StandardRulesXmlParser().parse(input);
    }
    finally {
      IOUtils.closeQuietly(input);
    }
  }

  public List<Rule> parseReferential(String fileContent) {
    return new StandardRulesXmlParser().parse(fileContent);
  }

  public MAPPER getRulePriorityMapper() {
    return priorityMapper;
  }

  protected String getCheckResourcesBase() {
    String base = getRepositoryResourcesBase();
    base = base.startsWith("/") ? base : "/" + base;
    base = base.endsWith("/") ? base : base + "/";
    return base;
  }

}

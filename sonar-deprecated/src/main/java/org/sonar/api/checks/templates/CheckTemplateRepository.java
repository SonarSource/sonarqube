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
package org.sonar.api.checks.templates;

import org.apache.commons.io.IOUtils;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.rules.*;
import org.sonar.check.IsoCategory;

import java.io.InputStream;
import java.util.*;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public class CheckTemplateRepository implements RulesRepository {

  private String key;
  private Language language;
  private List<CheckTemplate> templates;
  private Map<String, CheckTemplate> templatesByKey;


  public CheckTemplateRepository() {
  }

  public CheckTemplateRepository(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Key can not be null");
    }
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public CheckTemplateRepository setKey(String key) {
    this.key = key;
    return this;
  }

  public Language getLanguage() {
    return language;
  }

  public CheckTemplateRepository setLanguage(Language l) {
    this.language = l;
    return this;
  }

  public List<CheckTemplate> getTemplates() {
    if (templates == null) {
      return Collections.emptyList();
    }
    return templates;
  }

  public CheckTemplateRepository setTemplates(List<CheckTemplate> c) {
    this.templates = c;
    return this;
  }

  public CheckTemplate getTemplate(String key) {
    if (templatesByKey == null || templatesByKey.isEmpty()) {
      templatesByKey = new HashMap<String, CheckTemplate>();
      for (CheckTemplate template : templates) {
        templatesByKey.put(template.getKey(), template);
      }
    }
    return templatesByKey.get(key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CheckTemplateRepository that = (CheckTemplateRepository) o;
    return key.equals(that.key);

  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }


  public static CheckTemplateRepository createFromXml(String repositoryKey, Language language, String pathToXml) {
    InputStream input = CheckTemplateRepository.class.getResourceAsStream(pathToXml);
    try {
      List<CheckTemplate> templates = new XmlCheckTemplateFactory().parse(input);
      CheckTemplateRepository repository = new CheckTemplateRepository(repositoryKey);
      repository.setTemplates(templates);
      repository.setLanguage(language);
      return repository;

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public static CheckTemplateRepository createFromAnnotatedClasses(String repositoryKey, Language language, Collection<Class> classes) {
    AnnotationCheckTemplateFactory factory = new AnnotationCheckTemplateFactory(classes);
    CheckTemplateRepository repository = new CheckTemplateRepository(repositoryKey);
    repository.setTemplates(factory.create());
    repository.setLanguage(language);
    return repository;
  }










  /*

    CODE FOR BACKWARD COMPATIBLITY
    This class should not extend RulesRepository in next versions

   */


  public List<Rule> getInitialReferential() {
    List<Rule> rules = new ArrayList<Rule>();
    for (CheckTemplate checkTemplate : getTemplates()) {
      rules.add(toRule(checkTemplate));
    }
    return rules;
  }

  private Rule toRule(CheckTemplate checkTemplate) {
    Rule rule = new Rule(getKey(), checkTemplate.getKey());
    rule.setDescription(checkTemplate.getDescription(Locale.ENGLISH));
    rule.setName(checkTemplate.getTitle(Locale.ENGLISH));
    rule.setPriority(RulePriority.fromCheckPriority(checkTemplate.getPriority()));
    rule.setRulesCategory(toRuleCategory(checkTemplate.getIsoCategory()));
    for (CheckTemplateProperty checkTemplateProperty : checkTemplate.getProperties()) {
      RuleParam param = rule.createParameter(checkTemplateProperty.getKey());
      param.setDescription(checkTemplateProperty.getDescription(Locale.ENGLISH));
      param.setType("s");
    }

    return rule;
  }

  private RulesCategory toRuleCategory(IsoCategory isoCategory) {
    if (isoCategory == IsoCategory.Reliability) {
      return Iso9126RulesCategories.RELIABILITY;
    }
    if (isoCategory == IsoCategory.Efficiency) {
      return Iso9126RulesCategories.EFFICIENCY;
    }
    if (isoCategory == IsoCategory.Maintainability) {
      return Iso9126RulesCategories.MAINTAINABILITY;
    }
    if (isoCategory == IsoCategory.Portability) {
      return Iso9126RulesCategories.PORTABILITY;
    }
    if (isoCategory == IsoCategory.Usability) {
      return Iso9126RulesCategories.USABILITY;
    }
    return null;
  }


  public List<Rule> parseReferential(String fileContent) {
    return Collections.emptyList();
  }

  public List<RulesProfile> getProvidedProfiles() {
    return Collections.emptyList();
  }
}

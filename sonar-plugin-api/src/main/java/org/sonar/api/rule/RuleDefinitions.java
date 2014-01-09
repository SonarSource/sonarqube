/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.rule;

import com.google.common.collect.*;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerExtension;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the coding rules. For example the Java Findbugs plugin provides an implementation of
 * this extension point in order to define the rules that it supports.
 * <p/>
 * This interface replaces the deprecated class {@link org.sonar.api.rules.RuleRepository}.
 *
 * @since 4.2
 */
public interface RuleDefinitions extends ServerExtension {

  /**
   * Instantiated by core but not by plugins.
   */
  public static class Context {
    private final Map<String, NewRepository> newRepositories = Maps.newHashMap();
    private final ListMultimap<String, ExtendedRepository> extendedRepositories = ArrayListMultimap.create();

    public NewRepository newRepository(String key, String language) {
      if (newRepositories.containsKey(key)) {
        throw new IllegalArgumentException("The rule repository '" + key + "' is defined several times");
      }
      NewRepository repo = new NewRepository(key, language);
      newRepositories.put(key, repo);
      return repo;
    }

    /**
     * Add rules to a repository defined by another plugin. For example the Java FB-Contrib plugin
     * provides new rules for the Findbugs engine.
     *
     * @param key the key of the repository to extend, "findbugs" in the example.
     */
    public ExtendedRepository extendRepository(String key) {
      ExtendedRepository repo = new NewRepository(key);
      extendedRepositories.put(key, repo);
      return repo;
    }

    @CheckForNull
    public NewRepository getRepository(String key) {
      return newRepositories.get(key);
    }

    public List<NewRepository> getRepositories() {
      return ImmutableList.copyOf(newRepositories.values());
    }

    @CheckForNull
    public List<ExtendedRepository> getExtendedRepositories(String key) {
      return extendedRepositories.get(key);
    }

    public List<ExtendedRepository> getExtendedRepositories() {
      return ImmutableList.copyOf(extendedRepositories.values());
    }
  }

  public static interface ExtendedRepository {
    String key();

    NewRule newRule(String ruleKey);

    @CheckForNull
    NewRule getRule(String ruleKey);

    List<NewRule> getRules();
  }

  public static class NewRepository implements ExtendedRepository {
    private final String key;
    private String language;
    private String name;
    private final Map<String, NewRule> newRules = Maps.newHashMap();

    private NewRepository(String key, String language) {
      this.key = this.name = key;
      this.language = language;
    }

    // Used to expose ExtendedRepository
    private NewRepository(String key) {
      this.key = key;
    }

    public NewRepository setName(String s) {
      this.name = s;
      return this;
    }

    @Override
    public String key() {
      return key;
    }

    public String language() {
      return language;
    }

    public String name() {
      return name;
    }

    @Override
    public NewRule newRule(String ruleKey) {
      if (newRules.containsKey(ruleKey)) {
        throw new IllegalArgumentException("The rule '" + ruleKey + "' of repository '" + key + "' is declared several times");
      }
      NewRule newRule = new NewRule(key, ruleKey);
      newRules.put(ruleKey, newRule);
      return newRule;
    }

    @Override
    @CheckForNull
    public NewRule getRule(String ruleKey) {
      return newRules.get(ruleKey);
    }

    @Override
    public List<NewRule> getRules() {
      return ImmutableList.copyOf(newRules.values());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      NewRepository that = (NewRepository) o;
      return key.equals(that.key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }
  }

  public static class NewRule {
    private final String repoKey, key;
    private String name, htmlDescription, metadata, defaultSeverity = Severity.MAJOR;
    private final Set<String> tags = Sets.newHashSet();
    private final Map<String, NewParam> params = Maps.newHashMap();
    // TODO cardinality ? or template boolean ?

    public NewRule(String repoKey, String key) {
      this.repoKey = repoKey;
      this.key = this.name = key;
    }

    public String key() {
      return key;
    }

    public String name() {
      return name;
    }

    public NewRule setName(String s) {
      if (StringUtils.isBlank(s)) {
        throw new IllegalArgumentException("Name of rule " + this + " is blank");
      }
      this.name = s;
      return this;
    }

    public String defaultSeverity() {
      return defaultSeverity;
    }

    public NewRule setDefaultSeverity(String s) {
      if (!Severity.ALL.contains(s)) {
        throw new IllegalArgumentException("Default severity of rule " + this + " is not correct: " + s);
      }
      this.defaultSeverity = s;
      return this;
    }

    @CheckForNull
    public String htmlDescription() {
      return htmlDescription;
    }

    public NewRule setHtmlDescription(String s) {
      if (StringUtils.isBlank(s)) {
        throw new IllegalArgumentException("HTML description of rule " + this + " is blank");
      }
      this.htmlDescription = s;
      return this;
    }

    public NewParam newParam(String paramKey) {
      if (params.containsKey(paramKey)) {
        throw new IllegalArgumentException("The parameter '" + key + "' is declared several times on the rule " + this);
      }
      NewParam param = new NewParam(this, paramKey);
      params.put(paramKey, param);
      return param;
    }

    @CheckForNull
    public NewParam getParam(String key) {
      return params.get(key);
    }

    public List<NewParam> getParams() {
      return ImmutableList.copyOf(params.values());
    }

    public Set<String> tags() {
      return ImmutableSet.copyOf(tags);
    }

    /**
     * @see org.sonar.api.rule.RuleTagFormat
     */
    public NewRule addTag(String s) {
      RuleTagFormat.validate(s);
      tags.add(s);
      return this;
    }

    /**
     * @see org.sonar.api.rule.RuleTagFormat
     */
    public NewRule setTags(String... list) {
      tags.clear();
      for (String tag : list) {
        addTag(tag);
      }
      return this;
    }

    /**
     * @see org.sonar.api.rule.RuleDefinitions.NewRule#setMetadata(String)
     */
    @CheckForNull
    public String metadata() {
      return metadata;
    }

    /**
     * Optional metadata that can be used by the rule engine. Not displayed
     * in webapp. For example the Java Checkstyle plugin feeds this field
     * with the internal path ("Checker/TreeWalker/AnnotationUseStyle").
     */
    public NewRule setMetadata(@Nullable String s) {
      this.metadata = s;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      NewRule newRule = (NewRule) o;
      return key.equals(newRule.key) && repoKey.equals(newRule.repoKey);
    }

    @Override
    public int hashCode() {
      int result = repoKey.hashCode();
      result = 31 * result + key.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return String.format("[repository=%s, key=%s]", repoKey, key);
    }
  }

  public static class NewParam {
    private final NewRule rule;
    private final String key;
    private String name, description, defaultValue;
    // TODO type

    private NewParam(NewRule rule, String key) {
      this.rule = rule;
      this.key = this.name = key;
    }

    public String key() {
      return key;
    }

    public String name() {
      return name;
    }

    public NewParam setName(@Nullable String s) {
      // name must never be null.
      this.name = StringUtils.defaultIfBlank(s, key);
      return this;
    }

    /**
     * @see org.sonar.api.rule.RuleDefinitions.NewParam#setDescription(String)
     */
    @Nullable
    public String description() {
      return description;
    }

    /**
     * Plain-text description. Can be null.
     */
    public NewParam setDescription(@Nullable String s) {
      this.description = StringUtils.defaultIfBlank(s, null);
      return this;
    }

    @Nullable
    public String defaultValue() {
      return defaultValue;
    }

    public NewParam setDefaultValue(@Nullable String s) {
      this.defaultValue = s;
      return this;
    }

    /**
     * Helpful for method chaining.
     */
    public NewRule rule() {
      return rule;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      NewParam that = (NewParam) o;
      return key.equals(that.key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }
  }

  /**
   * This method is executed when server is started.
   */
  void defineRules(Context context);

}

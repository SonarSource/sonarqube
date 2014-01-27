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
package org.sonar.api.server.rule;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the coding rules. For example the Java Findbugs plugin provides an implementation of
 * this extension point in order to define the rules that it supports.
 * <p/>
 * This interface replaces the deprecated class org.sonar.api.rules.RuleRepository.
 *
 * @since 4.2
 */
public interface RuleDefinitions extends ServerExtension {

  /**
   * Instantiated by core but not by plugins
   */
  class Context {
    private final Map<String, Repository> repositoriesByKey = Maps.newHashMap();
    private final ListMultimap<String, ExtendedRepository> extendedRepositoriesByKey = ArrayListMultimap.create();

    public NewRepository newRepository(String key, String language) {
      return new NewRepositoryImpl(this, key, language, false);
    }

    public NewExtendedRepository extendRepository(String key, String language) {
      return new NewRepositoryImpl(this, key, language, true);
    }

    @CheckForNull
    public Repository repository(String key) {
      return repositoriesByKey.get(key);
    }

    public List<Repository> repositories() {
      return ImmutableList.copyOf(repositoriesByKey.values());
    }

    public List<ExtendedRepository> extendedRepositories(String repositoryKey) {
      return ImmutableList.copyOf(extendedRepositoriesByKey.get(repositoryKey));
    }

    public List<ExtendedRepository> extendedRepositories() {
      return ImmutableList.copyOf(extendedRepositoriesByKey.values());
    }

    private void registerRepository(NewRepositoryImpl newRepository) {
      if (repositoriesByKey.containsKey(newRepository.key)) {
        throw new IllegalStateException(String.format("The rule repository '%s' is defined several times", newRepository.key));
      }
      repositoriesByKey.put(newRepository.key, new RepositoryImpl(newRepository));
    }

    private void registerExtendedRepository(NewRepositoryImpl newRepository) {
      extendedRepositoriesByKey.put(newRepository.key, new RepositoryImpl(newRepository));
    }
  }

  interface NewExtendedRepository {
    NewRule newRule(String ruleKey);

    /**
     * Reads definition of rule from the annotations provided by the library sonar-check-api.
     */
    NewRule loadAnnotatedClass(Class clazz);

    /**
     * Reads definitions of rules from the annotations provided by the library sonar-check-api.
     */
    NewExtendedRepository loadAnnotatedClasses(Class... classes);

    /**
     * Reads definitions of rules from a XML file. Format is :
     * <pre>
     * &lt;rules&gt;
     * &lt;rule&gt;
     * &lt;!-- required fields --&gt;
     * &lt;key&gt;the-rule-key&lt;/key&gt;
     * &lt;name&gt;The purpose of the rule&lt;/name&gt;
     * &lt;description&gt;
     * &lt;![CDATA[The description]]&gt;
     * &lt;/description&gt;
     *
     * &lt;!-- optional fields --&gt;
     * &lt;configKey&gt;Checker/TreeWalker/LocalVariableName&lt;/configKey&gt;
     * &lt;severity&gt;BLOCKER&lt;/severity&gt;
     * &lt;cardinality&gt;MULTIPLE&lt;/cardinality&gt;
     * &lt;status&gt;BETA&lt;/status&gt;
     * &lt;param&gt;
     * &lt;key&gt;the-param-key&lt;/key&gt;
     * &lt;description&gt;
     * &lt;![CDATA[
     * the param-description
     * ]]&gt;
     * &lt;/description&gt;
     * &lt;defaultValue&gt;42&lt;/defaultValue&gt;
     * &lt;/param&gt;
     * &lt;param&gt;
     * &lt;key&gt;another-param&lt;/key&gt;
     * &lt;/param&gt;
     *
     * &lt;!-- deprecated fields --&gt;
     * &lt;priority&gt;BLOCKER&lt;/priority&gt;
     * &lt;/rule&gt;
     * &lt;/rules&gt;
     *
     * </pre>
     */
    NewExtendedRepository loadXml(InputStream xmlInput, String encoding);

    void done();
  }

  interface NewRepository extends NewExtendedRepository {
    NewRepository setName(String s);

    @CheckForNull
    NewRule rule(String ruleKey);
  }

  class NewRepositoryImpl implements NewRepository {
    private final Context context;
    private final boolean extended;
    private final String key;
    private String language;
    private String name;
    private final Map<String, NewRule> newRules = Maps.newHashMap();

    private NewRepositoryImpl(Context context, String key, String language, boolean extended) {
      this.extended = extended;
      this.context = context;
      this.key = this.name = key;
      this.language = language;
    }

    @Override
    public NewRepositoryImpl setName(@Nullable String s) {
      if (StringUtils.isNotEmpty(s)) {
        this.name = s;
      }
      return this;
    }

    @Override
    public NewRule newRule(String ruleKey) {
      if (newRules.containsKey(ruleKey)) {
        // Should fail in a perfect world, but at the time being the Findbugs plugin
        // defines several times the rule EC_INCOMPATIBLE_ARRAY_COMPARE
        // See http://jira.codehaus.org/browse/SONARJAVA-428
        LoggerFactory.getLogger(getClass()).warn(String.format("The rule '%s' of repository '%s' is declared several times", ruleKey, key));
      }
      NewRule newRule = new NewRule(key, ruleKey);
      newRules.put(ruleKey, newRule);
      return newRule;
    }

    @CheckForNull
    @Override
    public NewRule rule(String ruleKey) {
      return newRules.get(ruleKey);
    }

    @Override
    public NewRepositoryImpl loadAnnotatedClasses(Class... classes) {
      new RuleDefinitionsFromAnnotations().loadRules(this, classes);
      return this;
    }

    @Override
    public RuleDefinitions.NewRule loadAnnotatedClass(Class clazz) {
      return new RuleDefinitionsFromAnnotations().loadRule(this, clazz);
    }

    @Override
    public NewRepositoryImpl loadXml(InputStream xmlInput, String encoding) {
      new RuleDefinitionsFromXml().loadRules(this, xmlInput, encoding);
      return this;
    }

    @Override
    public void done() {
      // note that some validations can be done here, for example for
      // verifying that at least one rule is declared

      if (extended) {
        context.registerExtendedRepository(this);
      } else {
        context.registerRepository(this);
      }
    }
  }

  interface ExtendedRepository {
    String key();

    String language();

    @CheckForNull
    Rule rule(String ruleKey);

    List<Rule> rules();
  }

  interface Repository extends ExtendedRepository {
    String name();
  }

  @Immutable
  class RepositoryImpl implements Repository {
    private final String key, language, name;
    private final Map<String, Rule> rulesByKey;

    private RepositoryImpl(NewRepositoryImpl newRepository) {
      this.key = newRepository.key;
      this.language = newRepository.language;
      this.name = newRepository.name;
      ImmutableMap.Builder<String, Rule> ruleBuilder = ImmutableMap.builder();
      for (NewRule newRule : newRepository.newRules.values()) {
        newRule.validate();
        ruleBuilder.put(newRule.key, new Rule(this, newRule));
      }
      this.rulesByKey = ruleBuilder.build();
    }

    @Override
    public String key() {
      return key;
    }

    @Override
    public String language() {
      return language;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    @CheckForNull
    public Rule rule(String ruleKey) {
      return rulesByKey.get(ruleKey);
    }

    @Override
    public List<Rule> rules() {
      return ImmutableList.copyOf(rulesByKey.values());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RepositoryImpl that = (RepositoryImpl) o;
      return key.equals(that.key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }
  }

  class NewRule {
    private final String repoKey, key;
    private String name, htmlDescription, engineKey, severity = Severity.MAJOR;
    private boolean template;
    private RuleStatus status = RuleStatus.defaultStatus();
    private final Set<String> tags = Sets.newTreeSet();
    private final Map<String, NewParam> paramsByKey = Maps.newHashMap();

    private NewRule(String repoKey, String key) {
      this.repoKey = repoKey;
      this.key = key;
    }

    public String key() {
      return this.key;
    }

    public NewRule setName(String s) {
      // TODO remove newlines
      this.name = s;
      return this;
    }

    public NewRule setTemplate(boolean template) {
      this.template = template;
      return this;
    }

    public NewRule setSeverity(String s) {
      if (!Severity.ALL.contains(s)) {
        throw new IllegalArgumentException(String.format("Severity of rule %s is not correct: %s", this, s));
      }
      this.severity = s;
      return this;
    }

    public NewRule setHtmlDescription(String s) {
      this.htmlDescription = s;
      return this;
    }

    public NewRule setStatus(RuleStatus status) {
      if (status.equals(RuleStatus.REMOVED)) {
        throw new IllegalArgumentException(String.format("Status 'REMOVED' is not accepted on rule '%s'", this));
      }
      this.status = status;
      return this;
    }

    public NewParam newParam(String paramKey) {
      if (paramsByKey.containsKey(paramKey)) {
        throw new IllegalArgumentException(String.format("The parameter '%s' is declared several times on the rule %s", paramKey, this));
      }
      NewParam param = new NewParam(paramKey);
      paramsByKey.put(paramKey, param);
      return param;
    }

    @CheckForNull
    public NewParam param(String paramKey) {
      return paramsByKey.get(paramKey);
    }

    /**
     * @see RuleTagFormat
     */
    public NewRule addTags(String... list) {
      for (String tag : list) {
        RuleTagFormat.validate(tag);
        tags.add(tag);
      }
      return this;
    }

    /**
     * @see RuleTagFormat
     */
    public NewRule setTags(String... list) {
      tags.clear();
      addTags(list);
      return this;
    }

    /**
     * Optional key that can be used by the rule engine. Not displayed
     * in webapp. For example the Java Checkstyle plugin feeds this field
     * with the internal path ("Checker/TreeWalker/AnnotationUseStyle").
     */
    public NewRule setEngineKey(@Nullable String s) {
      this.engineKey = s;
      return this;
    }

    private void validate() {
      if (StringUtils.isBlank(name)) {
        throw new IllegalStateException(String.format("Name of rule %s is empty", this));
      }
      if (StringUtils.isBlank(htmlDescription)) {
        throw new IllegalStateException(String.format("HTML description of rule %s is empty", this));
      }
    }

    @Override
    public String toString() {
      return String.format("[repository=%s, key=%s]", repoKey, key);
    }
  }

  @Immutable
  class Rule {
    private final Repository repository;
    private final String repoKey, key, name, htmlDescription, engineKey, severity;
    private final boolean template;
    private final Set<String> tags;
    private final Map<String, Param> params;
    private final RuleStatus status;

    private Rule(Repository repository, NewRule newRule) {
      this.repository = repository;
      this.repoKey = newRule.repoKey;
      this.key = newRule.key;
      this.name = newRule.name;
      this.htmlDescription = newRule.htmlDescription;
      this.engineKey = newRule.engineKey;
      this.severity = newRule.severity;
      this.template = newRule.template;
      this.status = newRule.status;
      this.tags = ImmutableSortedSet.copyOf(newRule.tags);
      ImmutableMap.Builder<String, Param> paramsBuilder = ImmutableMap.builder();
      for (NewParam newParam : newRule.paramsByKey.values()) {
        paramsBuilder.put(newParam.key, new Param(newParam));
      }
      this.params = paramsBuilder.build();
    }

    public Repository repository() {
      return repository;
    }

    public String key() {
      return key;
    }

    public String name() {
      return name;
    }

    public String severity() {
      return severity;
    }

    @CheckForNull
    public String htmlDescription() {
      return htmlDescription;
    }

    public boolean template() {
      return template;
    }

    public RuleStatus status() {
      return status;
    }

    @CheckForNull
    public Param param(String key) {
      return params.get(key);
    }

    public List<Param> params() {
      return ImmutableList.copyOf(params.values());
    }

    public Set<String> tags() {
      return tags;
    }

    /**
     * @see RuleDefinitions.NewRule#setEngineKey(String)
     */
    @CheckForNull
    public String engineKey() {
      return engineKey;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Rule other = (Rule) o;
      return key.equals(other.key) && repoKey.equals(other.repoKey);
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

  class NewParam {
    private final String key;
    private String name, description, defaultValue;
    private RuleParamType type = RuleParamType.STRING;

    private NewParam(String key) {
      this.key = this.name = key;
    }

    public NewParam setName(@Nullable String s) {
      // name must never be null.
      this.name = StringUtils.defaultIfBlank(s, key);
      return this;
    }

    public NewParam setType(RuleParamType t) {
      this.type = t;
      return this;
    }

    /**
     * Plain-text description. Can be null.
     */
    public NewParam setDescription(@Nullable String s) {
      this.description = StringUtils.defaultIfBlank(s, null);
      return this;
    }

    public NewParam setDefaultValue(@Nullable String s) {
      this.defaultValue = s;
      return this;
    }
  }

  @Immutable
  class Param {
    private final String key, name, description, defaultValue;
    private final RuleParamType type;

    private Param(NewParam newParam) {
      this.key = newParam.key;
      this.name = newParam.name;
      this.description = newParam.description;
      this.defaultValue = newParam.defaultValue;
      this.type = newParam.type;
    }

    public String key() {
      return key;
    }

    public String name() {
      return name;
    }

    @Nullable
    public String description() {
      return description;
    }

    @Nullable
    public String defaultValue() {
      return defaultValue;
    }

    public RuleParamType type() {
      return type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Param that = (Param) o;
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
  void define(Context context);

}

/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines some coding rules of the same repository. For example the Java Findbugs plugin provides an implementation of
 * this extension point in order to define the rules that it supports.
 * <p/>
 * This interface replaces the deprecated class org.sonar.api.rules.RuleRepository.
 * <p/>
 * <h3>How to use</h3>
 * <pre>
 * public class MyJsRulesDefinition implements RulesDefinition {
 *
 *   {@literal @}Override
 *   public void define(Context context) {
 *     NewRepository repository = context.createRepository("my_js", "js").setName("My Javascript Analyzer");
 *
 *     // define a rule programmatically. Note that rules
 *     // could be loaded from files (JSON, XML, ...)
 *     NewRule x1Rule = repository.createRule("x1")
 *      .setName("No empty line")
 *      .setHtmlDescription("Generate an issue on empty lines")
 *
 *      // optional tags
 *      .setTags("style", "stupid")
 *
 *     // optional status. Default value is READY.
 *     .setStatus(RuleStatus.BETA)
 *
 *     // default severity when the rule is activated on a Quality profile. Default value is MAJOR.
 *     .setSeverity(Severity.MINOR);
 *
 *     x1Rule
 *       .setDebtSubCharacteristic("INTEGRATION_TESTABILITY")
 *       .setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));
 *
 *     x1Rule.createParam("acceptWhitespace")
 *       .setDefaultValue("false")
 *       .setType(RuleParamType.BOOLEAN)
 *       .setDescription("Accept whitespaces on the line");
 *
 *     // don't forget to call done() to finalize the definition
 *     repository.done();
 *   }
 * }
 * </pre>
 * <p/>
 * If rules are declared in a XML file with the standard SonarQube format (see
 * {@link org.sonar.api.server.rule.RulesDefinitionXmlLoader}), then it can be loaded by using :
 * <p/>
 * <pre>
 * public class MyJsRulesDefinition implements RulesDefinition {
 *
 *   private final RulesDefinitionXmlLoader xmlLoader;
 *
 *   public MyJsRulesDefinition(RulesDefinitionXmlLoader xmlLoader) {
 *     this.xmlLoader = xmlLoader;
 *   }
 *
 *   {@literal @}Override
 *   public void define(Context context) {
 *     NewRepository repository = context.createRepository("my_js", "js").setName("My Javascript Analyzer");
 *     // see javadoc of RulesDefinitionXmlLoader for the format
 *     xmlLoader.load(repository, getClass().getResourceAsStream("/path/to/rules.xml"));
 *     repository.done();
 *   }
 * }
 * </pre>
 * <p/>
 * In the above example, XML file must contain name and description of each rule. If it's not the case, then the
 * (deprecated) English bundles can be used :
 * <p/>
 * <pre>
 * public class MyJsRulesDefinition implements RulesDefinition {
 *
 *   private final RulesDefinitionXmlLoader xmlLoader;
 *   private final RulesDefinitionI18nLoader i18nLoader;
 *
 *   public MyJsRulesDefinition(RulesDefinitionXmlLoader xmlLoader, RulesDefinitionI18nLoader i18nLoader) {
 *     this.xmlLoader = xmlLoader;
 *     this.i18nLoader = i18nLoader;
 *   }
 *
 *   {@literal @}Override
 *   public void define(Context context) {
 *     NewRepository repository = context.createRepository("my_js", "js").setName("My Javascript Analyzer");
 *     xmlLoader.load(repository, getClass().getResourceAsStream("/path/to/rules.xml"), "UTF-8");
 *     i18nLoader.load(repository);
 *     repository.done();
 *   }
 * }
 * </pre>
 *
 * @since 4.3
 */
@ServerSide
@ExtensionPoint
public interface RulesDefinition {

  /**
   * Default sub-characteristics of technical debt model. See http://www.sqale.org
   */
  final class SubCharacteristics {
    /**
     * Related to characteristic REUSABILITY
     */
    public static final String MODULARITY = "MODULARITY";

    /**
     * Related to characteristic REUSABILITY
     */
    public static final String TRANSPORTABILITY = "TRANSPORTABILITY";

    /**
     * Related to characteristic PORTABILITY
     */
    public static final String COMPILER_RELATED_PORTABILITY = "COMPILER_RELATED_PORTABILITY";

    /**
     * Related to characteristic PORTABILITY
     */
    public static final String HARDWARE_RELATED_PORTABILITY = "HARDWARE_RELATED_PORTABILITY";

    /**
     * Related to characteristic PORTABILITY
     */
    public static final String LANGUAGE_RELATED_PORTABILITY = "LANGUAGE_RELATED_PORTABILITY";

    /**
     * Related to characteristic PORTABILITY
     */
    public static final String OS_RELATED_PORTABILITY = "OS_RELATED_PORTABILITY";

    /**
     * Related to characteristic PORTABILITY
     */
    public static final String SOFTWARE_RELATED_PORTABILITY = "SOFTWARE_RELATED_PORTABILITY";

    /**
     * Related to characteristic PORTABILITY
     */
    public static final String TIME_ZONE_RELATED_PORTABILITY = "TIME_ZONE_RELATED_PORTABILITY";

    /**
     * Related to characteristic MAINTAINABILITY
     */
    public static final String READABILITY = "READABILITY";

    /**
     * Related to characteristic MAINTAINABILITY
     */
    public static final String UNDERSTANDABILITY = "UNDERSTANDABILITY";

    /**
     * Related to characteristic SECURITY
     */
    public static final String API_ABUSE = "API_ABUSE";

    /**
     * Related to characteristic SECURITY
     */
    public static final String ERRORS = "ERRORS";

    /**
     * Related to characteristic SECURITY
     */
    public static final String INPUT_VALIDATION_AND_REPRESENTATION = "INPUT_VALIDATION_AND_REPRESENTATION";

    /**
     * Related to characteristic SECURITY
     */
    public static final String SECURITY_FEATURES = "SECURITY_FEATURES";

    /**
     * Related to characteristic EFFICIENCY
     */
    public static final String CPU_EFFICIENCY = "CPU_EFFICIENCY";

    /**
     * Related to characteristic EFFICIENCY
     */
    public static final String MEMORY_EFFICIENCY = "MEMORY_EFFICIENCY";

    /**
     * Related to characteristic EFFICIENCY
     */
    public static final String NETWORK_USE = "NETWORK_USE";

    /**
     * Related to characteristic CHANGEABILITY
     */
    public static final String ARCHITECTURE_CHANGEABILITY = "ARCHITECTURE_CHANGEABILITY";

    /**
     * Related to characteristic CHANGEABILITY
     */
    public static final String DATA_CHANGEABILITY = "DATA_CHANGEABILITY";

    /**
     * Related to characteristic CHANGEABILITY
     */
    public static final String LOGIC_CHANGEABILITY = "LOGIC_CHANGEABILITY";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String ARCHITECTURE_RELIABILITY = "ARCHITECTURE_RELIABILITY";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String DATA_RELIABILITY = "DATA_RELIABILITY";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String EXCEPTION_HANDLING = "EXCEPTION_HANDLING";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String FAULT_TOLERANCE = "FAULT_TOLERANCE";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String INSTRUCTION_RELIABILITY = "INSTRUCTION_RELIABILITY";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String LOGIC_RELIABILITY = "LOGIC_RELIABILITY";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String RESOURCE_RELIABILITY = "RESOURCE_RELIABILITY";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String SYNCHRONIZATION_RELIABILITY = "SYNCHRONIZATION_RELIABILITY";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String UNIT_TESTS = "UNIT_TESTS";

    /**
     * Related to characteristic TESTABILITY
     */
    public static final String INTEGRATION_TESTABILITY = "INTEGRATION_TESTABILITY";

    /**
     * Related to characteristic TESTABILITY
     */
    public static final String UNIT_TESTABILITY = "UNIT_TESTABILITY";

    /**
     * Related to characteristic ACCESSIBILITY
     */
    public static final String USABILITY_ACCESSIBILITY = "USABILITY_ACCESSIBILITY";

    /**
     * Related to characteristic ACCESSIBILITY
     */
    public static final String USABILITY_COMPLIANCE = "USABILITY_COMPLIANCE";

    /**
     * Related to characteristic ACCESSIBILITY
     */
    public static final String USABILITY_EASE_OF_USE = "USABILITY_EASE_OF_USE";

    /**
     * Related to characteristic REUSABILITY
     */
    public static final String REUSABILITY_COMPLIANCE = "REUSABILITY_COMPLIANCE";

    /**
     * Related to characteristic PORTABILITY
     */
    public static final String PORTABILITY_COMPLIANCE = "PORTABILITY_COMPLIANCE";

    /**
     * Related to characteristic MAINTAINABILITY
     */
    public static final String MAINTAINABILITY_COMPLIANCE = "MAINTAINABILITY_COMPLIANCE";

    /**
     * Related to characteristic SECURITY
     */
    public static final String SECURITY_COMPLIANCE = "SECURITY_COMPLIANCE";

    /**
     * Related to characteristic EFFICIENCY
     */
    public static final String EFFICIENCY_COMPLIANCE = "EFFICIENCY_COMPLIANCE";

    /**
     * Related to characteristic CHANGEABILITY
     */
    public static final String CHANGEABILITY_COMPLIANCE = "CHANGEABILITY_COMPLIANCE";

    /**
     * Related to characteristic RELIABILITY
     */
    public static final String RELIABILITY_COMPLIANCE = "RELIABILITY_COMPLIANCE";

    /**
     * Related to characteristic TESTABILITY
     */
    public static final String TESTABILITY_COMPLIANCE = "TESTABILITY_COMPLIANCE";

    private SubCharacteristics() {
      // only constants
    }
  }

  /**
   * Instantiated by core but not by plugins
   */
  public class Context {
    private final Map<String, Repository> repositoriesByKey = Maps.newHashMap();
    private final ListMultimap<String, ExtendedRepository> extendedRepositoriesByKey = ArrayListMultimap.create();

    public NewRepository createRepository(String key, String language) {
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
    NewRule createRule(String ruleKey);

    @CheckForNull
    NewRule rule(String ruleKey);

    Collection<NewRule> rules();

    String key();

    void done();
  }

  interface NewRepository extends NewExtendedRepository {
    NewRepository setName(String s);
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
    public String key() {
      return key;
    }

    @Override
    public NewRepositoryImpl setName(@Nullable String s) {
      if (StringUtils.isNotEmpty(s)) {
        this.name = s;
      }
      return this;
    }

    @Override
    public NewRule createRule(String ruleKey) {
      if (newRules.containsKey(ruleKey)) {
        // Should fail in a perfect world, but at the time being the Findbugs plugin
        // defines several times the rule EC_INCOMPATIBLE_ARRAY_COMPARE
        // See http://jira.codehaus.org/browse/SONARJAVA-428
        Loggers.get(getClass()).warn(String.format("The rule '%s' of repository '%s' is declared several times", ruleKey, key));
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
    public Collection<NewRule> rules() {
      return newRules.values();
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

  /**
   * Factory of {@link org.sonar.api.server.debt.DebtRemediationFunction}.
   */
  interface DebtRemediationFunctions {
    DebtRemediationFunction linear(String coefficient);

    DebtRemediationFunction linearWithOffset(String coefficient, String offset);

    DebtRemediationFunction constantPerIssue(String offset);
  }

  class NewRule {
    private final String repoKey, key;
    private String name, htmlDescription, markdownDescription, internalKey, severity = Severity.MAJOR;
    private boolean template;
    private RuleStatus status = RuleStatus.defaultStatus();
    private String debtSubCharacteristic;
    private DebtRemediationFunction debtRemediationFunction;
    private String effortToFixDescription;
    private final Set<String> tags = Sets.newTreeSet();
    private final Map<String, NewParam> paramsByKey = Maps.newHashMap();
    private final DebtRemediationFunctions functions;

    private NewRule(String repoKey, String key) {
      this.repoKey = repoKey;
      this.key = key;
      this.functions = new DefaultDebtRemediationFunctions(repoKey, key);
    }

    public String key() {
      return this.key;
    }

    /**
     * Required rule name
     */
    public NewRule setName(@Nullable String s) {
      this.name = StringUtils.trimToNull(s);
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

    public NewRule setHtmlDescription(@Nullable String s) {
      if (markdownDescription != null) {
        throw new IllegalStateException(String.format("Rule '%s' already has a Markdown description", this));
      }
      this.htmlDescription = StringUtils.trimToNull(s);
      return this;
    }

    /**
     * Load description from a file available in classpath. Example : <code>setHtmlDescription(getClass().getResource("/myrepo/Rule1234.html")</code>
     */
    public NewRule setHtmlDescription(@Nullable URL classpathUrl) {
      if (classpathUrl != null) {
        try {
          setHtmlDescription(IOUtils.toString(classpathUrl));
        } catch (IOException e) {
          throw new IllegalStateException("Fail to read: " + classpathUrl, e);
        }
      } else {
        this.htmlDescription = null;
      }
      return this;
    }

    public NewRule setMarkdownDescription(@Nullable String s) {
      if (htmlDescription != null) {
        throw new IllegalStateException(String.format("Rule '%s' already has an HTML description", this));
      }
      this.markdownDescription = StringUtils.trimToNull(s);
      return this;
    }

    /**
     * Load description from a file available in classpath. Example : <code>setMarkdownDescription(getClass().getResource("/myrepo/Rule1234.md")</code>
     */
    public NewRule setMarkdownDescription(@Nullable URL classpathUrl) {
      if (classpathUrl != null) {
        try {
          setMarkdownDescription(IOUtils.toString(classpathUrl));
        } catch (IOException e) {
          throw new IllegalStateException("Fail to read: " + classpathUrl, e);
        }
      } else {
        this.markdownDescription = null;
      }
      return this;
    }

    /**
     * Default value is {@link org.sonar.api.rule.RuleStatus#READY}. The value
     * {@link org.sonar.api.rule.RuleStatus#REMOVED} is not accepted and raises an
     * {@link java.lang.IllegalArgumentException}.
     */
    public NewRule setStatus(RuleStatus status) {
      if (status.equals(RuleStatus.REMOVED)) {
        throw new IllegalArgumentException(String.format("Status 'REMOVED' is not accepted on rule '%s'", this));
      }
      this.status = status;
      return this;
    }

    /**
     * SQALE sub-characteristic. See http://www.sqale.org
     *
     * @see org.sonar.api.server.rule.RulesDefinition.SubCharacteristics for constant values
     */
    public NewRule setDebtSubCharacteristic(@Nullable String s) {
      this.debtSubCharacteristic = s;
      return this;
    }

    /**
     * Factory of {@link org.sonar.api.server.debt.DebtRemediationFunction}
     */
    public DebtRemediationFunctions debtRemediationFunctions() {
      return functions;
    }

    /**
     * @see #debtRemediationFunctions()
     */
    public NewRule setDebtRemediationFunction(@Nullable DebtRemediationFunction fn) {
      this.debtRemediationFunction = fn;
      return this;
    }

    /**
     * For rules that use "Linear"/"Linear with offset" remediation functions, the meaning
     * of the function parameter (= "effort to fix") must be set. This description
     * explains what 1 point of "effort to fix" represents for the rule.
     * <p/>
     * Example : : for the "Insufficient condition coverage", this description for the
     * remediation function coefficient/offset would be something like
     * "Effort to test one uncovered condition".
     */
    public NewRule setEffortToFixDescription(@Nullable String s) {
      this.effortToFixDescription = s;
      return this;
    }

    public NewParam createParam(String paramKey) {
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

    public Collection<NewParam> params() {
      return paramsByKey.values();
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
    public NewRule setInternalKey(@Nullable String s) {
      this.internalKey = s;
      return this;
    }

    private void validate() {
      if (Strings.isNullOrEmpty(name)) {
        throw new IllegalStateException(String.format("Name of rule %s is empty", this));
      }
      if (Strings.isNullOrEmpty(htmlDescription) && Strings.isNullOrEmpty(markdownDescription)) {
        throw new IllegalStateException(String.format("One of HTML description or Markdown description must be defined for rule %s", this));
      }
      if ((Strings.isNullOrEmpty(debtSubCharacteristic) && debtRemediationFunction != null) || (!Strings.isNullOrEmpty(debtSubCharacteristic) && debtRemediationFunction == null)) {
        throw new IllegalStateException(String.format("Both debt sub-characteristic and debt remediation function should be defined on rule '%s'", this));
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
    private final String repoKey, key, name, htmlDescription, markdownDescription, internalKey, severity;
    private final boolean template;
    private final String debtSubCharacteristic;
    private final DebtRemediationFunction debtRemediationFunction;
    private final String effortToFixDescription;
    private final Set<String> tags;
    private final Map<String, Param> params;
    private final RuleStatus status;

    private Rule(Repository repository, NewRule newRule) {
      this.repository = repository;
      this.repoKey = newRule.repoKey;
      this.key = newRule.key;
      this.name = newRule.name;
      this.htmlDescription = newRule.htmlDescription;
      this.markdownDescription = newRule.markdownDescription;
      this.internalKey = newRule.internalKey;
      this.severity = newRule.severity;
      this.template = newRule.template;
      this.status = newRule.status;
      this.debtSubCharacteristic = newRule.debtSubCharacteristic;
      this.debtRemediationFunction = newRule.debtRemediationFunction;
      this.effortToFixDescription = newRule.effortToFixDescription;
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

    @CheckForNull
    public String markdownDescription() {
      return markdownDescription;
    }

    public boolean template() {
      return template;
    }

    public RuleStatus status() {
      return status;
    }

    @CheckForNull
    public String debtSubCharacteristic() {
      return debtSubCharacteristic;
    }

    @CheckForNull
    public DebtRemediationFunction debtRemediationFunction() {
      return debtRemediationFunction;
    }

    @CheckForNull
    public String effortToFixDescription() {
      return effortToFixDescription;
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
     * @see RulesDefinition.NewRule#setInternalKey(String)
     */
    @CheckForNull
    public String internalKey() {
      return internalKey;
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

    public String key() {
      return key;
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

    /**
     * Empty default value will be converted to null.
     */
    public NewParam setDefaultValue(@Nullable String s) {
      this.defaultValue = Strings.emptyToNull(s);
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

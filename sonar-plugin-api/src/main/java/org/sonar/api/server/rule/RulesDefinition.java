/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.server.rule;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.api.sonarlint.SonarLintSide;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * Defines some coding rules of the same repository. For example the Java Findbugs plugin provides an implementation of
 * this extension point in order to define the rules that it supports.
 * <br>
 * This interface replaces the deprecated class org.sonar.api.rules.RuleRepository.
 * <br>
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
 *     // optional type for SonarQube Quality Model. Default is RulesDefinition.Type.CODE_SMELL.
 *     .setType(RulesDefinition.Type.BUG)
 *
 *     x1Rule
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
 * <br>
 * If rules are declared in a XML file with the standard SonarQube format (see
 * {@link org.sonar.api.server.rule.RulesDefinitionXmlLoader}), then it can be loaded by using :
 * <br>
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
 * <br>
 * In the above example, XML file must contain name and description of each rule. If it's not the case, then the
 * (deprecated) English bundles can be used :
 * <br>
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
@ComputeEngineSide
@SonarLintSide
@ExtensionPoint
public interface RulesDefinition {

  /**
   * Default sub-characteristics of technical debt model. See http://www.sqale.org
   *
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   * See https://jira.sonarsource.com/browse/MMF-184
   */
  @Deprecated
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
   * Instantiated by core but not by plugins, except for their tests.
   */
  class Context {
    private final Map<String, Repository> repositoriesByKey = new HashMap<>();
    private String currentPluginKey;

    /**
     * New builder for {@link org.sonar.api.server.rule.RulesDefinition.Repository}.
     * <br>
     * A plugin can add rules to a repository that is defined then executed by another plugin. For instance
     * the FbContrib plugin contributes to the Findbugs plugin rules. In this case no need
     * to execute {@link org.sonar.api.server.rule.RulesDefinition.NewRepository#setName(String)}
     */
    public NewRepository createRepository(String key, String language) {
      return new NewRepositoryImpl(this, key, language);
    }

    /**
     * @deprecated since 5.2. Simply use {@link #createRepository(String, String)}
     */
    @Deprecated
    public NewRepository extendRepository(String key, String language) {
      return createRepository(key, language);
    }

    @CheckForNull
    public Repository repository(String key) {
      return repositoriesByKey.get(key);
    }

    public List<Repository> repositories() {
      return unmodifiableList(new ArrayList<>(repositoriesByKey.values()));
    }

    /**
     * @deprecated returns empty list since 5.2. Concept of "extended repository" was misleading and not valuable. Simply declare
     * repositories and use {@link #repositories()}. See http://jira.sonarsource.com/browse/SONAR-6709
     */
    @Deprecated
    public List<ExtendedRepository> extendedRepositories(String repositoryKey) {
      return emptyList();
    }

    /**
     * @deprecated returns empty list since 5.2. Concept of "extended repository" was misleading and not valuable. Simply declare
     * repositories and use {@link #repositories()}. See http://jira.sonarsource.com/browse/SONAR-6709
     */
    @Deprecated
    public List<ExtendedRepository> extendedRepositories() {
      return emptyList();
    }

    private void registerRepository(NewRepositoryImpl newRepository) {
      Repository existing = repositoriesByKey.get(newRepository.key());
      if (existing != null) {
        String existingLanguage = existing.language();
        checkState(existingLanguage.equals(newRepository.language),
          "The rule repository '%s' must not be defined for two different languages: %s and %s",
          newRepository.key, existingLanguage, newRepository.language);
      }
      repositoriesByKey.put(newRepository.key, new RepositoryImpl(newRepository, existing));
    }

    public void setCurrentPluginKey(@Nullable String pluginKey) {
      this.currentPluginKey = pluginKey;
    }
  }

  interface NewExtendedRepository {
    /**
     * Create a rule with specified key. Max length of key is 200 characters. Key must be unique
     * among the repository
     *
     * @throws IllegalArgumentException is key is not unique. Note a warning was logged up to version 5.4 (included)
     */
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
    private final String key;
    private String language;
    private String name;
    private final Map<String, NewRule> newRules = new HashMap<>();

    private NewRepositoryImpl(Context context, String key, String language) {
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
      checkArgument(!newRules.containsKey(ruleKey), "The rule '%s' of repository '%s' is declared several times", ruleKey, key);
      NewRule newRule = new NewRule(context.currentPluginKey, key, ruleKey);
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

      context.registerRepository(this);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("NewRepository{");
      sb.append("key='").append(key).append('\'');
      sb.append(", language='").append(language).append('\'');
      sb.append('}');
      return sb.toString();
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
    private final String key;
    private final String language;
    private final String name;
    private final Map<String, Rule> rulesByKey;

    private RepositoryImpl(NewRepositoryImpl newRepository, @Nullable Repository mergeInto) {
      this.key = newRepository.key;
      this.language = newRepository.language;

      Map<String, Rule> ruleBuilder = new HashMap<>();
      if (mergeInto != null) {
        if (!StringUtils.equals(newRepository.language, mergeInto.language()) || !StringUtils.equals(newRepository.key, mergeInto.key())) {
          throw new IllegalArgumentException(format("Bug - language and key of the repositories to be merged should be the sames: %s and %s", newRepository, mergeInto));
        }
        this.name = StringUtils.defaultIfBlank(mergeInto.name(), newRepository.name);
        for (Rule rule : mergeInto.rules()) {
          if (!newRepository.key().startsWith("common-") && ruleBuilder.containsKey(rule.key())) {
            Loggers.get(getClass()).warn("The rule '{}' of repository '{}' is declared several times", rule.key(), mergeInto.key());
          }
          ruleBuilder.put(rule.key(), rule);
        }
      } else {
        this.name = newRepository.name;
      }
      for (NewRule newRule : newRepository.newRules.values()) {
        newRule.validate();
        ruleBuilder.put(newRule.key, new Rule(this, newRule));
      }
      this.rulesByKey = unmodifiableMap(ruleBuilder);
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
      return unmodifiableList(new ArrayList<>(rulesByKey.values()));
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

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("Repository{");
      sb.append("key='").append(key).append('\'');
      sb.append(", language='").append(language).append('\'');
      sb.append('}');
      return sb.toString();
    }
  }

  /**
   * Factory of {@link org.sonar.api.server.debt.DebtRemediationFunction}.
   */
  interface DebtRemediationFunctions {

    /**
     * Shortcut for {@code create(Type.LINEAR, gap multiplier, null)}.
     *
     * @param gapMultiplier the duration to fix one issue. See {@link DebtRemediationFunction} for details about format.
     * @see org.sonar.api.server.debt.DebtRemediationFunction.Type#LINEAR
     */
    DebtRemediationFunction linear(String gapMultiplier);

    /**
     * Shortcut for {@code create(Type.LINEAR_OFFSET, gap multiplier, base effort)}.
     *
     * @param gapMultiplier duration to fix one point of complexity. See {@link DebtRemediationFunction} for details and format.
     * @param baseEffort    duration to make basic analysis. See {@link DebtRemediationFunction} for details and format.
     * @see org.sonar.api.server.debt.DebtRemediationFunction.Type#LINEAR_OFFSET
     */
    DebtRemediationFunction linearWithOffset(String gapMultiplier, String baseEffort);

    /**
     * Shortcut for {@code create(Type.CONSTANT_ISSUE, null, base effort)}.
     *
     * @param baseEffort cost per issue. See {@link DebtRemediationFunction} for details and format.
     * @see org.sonar.api.server.debt.DebtRemediationFunction.Type#CONSTANT_ISSUE
     */
    DebtRemediationFunction constantPerIssue(String baseEffort);

    /**
     * Flexible way to create a {@link DebtRemediationFunction}. An unchecked exception is thrown if
     * coefficient and/or offset are not valid according to the given @{code type}.
     *
     * @since 5.3
     */
    DebtRemediationFunction create(DebtRemediationFunction.Type type, @Nullable String gapMultiplier, @Nullable String baseEffort);
  }

  class NewRule {
    private final String pluginKey;
    private final String repoKey;
    private final String key;
    private RuleType type;
    private String name;
    private String htmlDescription;
    private String markdownDescription;
    private String internalKey;
    private String severity = Severity.MAJOR;
    private boolean template;
    private RuleStatus status = RuleStatus.defaultStatus();
    private DebtRemediationFunction debtRemediationFunction;
    private String gapDescription;
    private final Set<String> tags = new TreeSet<>();
    private final Map<String, NewParam> paramsByKey = new HashMap<>();
    private final DebtRemediationFunctions functions;
    private boolean activatedByDefault;
    private RuleScope scope;
    private final Set<RuleKey> deprecatedRuleKeys = new TreeSet<>();

    private NewRule(@Nullable String pluginKey, String repoKey, String key) {
      this.pluginKey = pluginKey;
      this.repoKey = repoKey;
      this.key = key;
      this.functions = new DefaultDebtRemediationFunctions(repoKey, key);
    }

    public String key() {
      return this.key;
    }

    /**
     * @since 7.1
     */
    @CheckForNull
    public RuleScope scope() {
      return this.scope;
    }

    /**
     * @since 7.1
     */
    public NewRule setScope(RuleScope scope) {
      this.scope = scope;
      return this;
    }

    /**
     * Required rule name
     */
    public NewRule setName(String s) {
      this.name = trimToNull(s);
      return this;
    }

    public NewRule setTemplate(boolean template) {
      this.template = template;
      return this;
    }

    /**
     * Should this rule be enabled by default. For example in SonarLint standalone.
     *
     * @since 6.0
     */
    public NewRule setActivatedByDefault(boolean activatedByDefault) {
      this.activatedByDefault = activatedByDefault;
      return this;
    }

    public NewRule setSeverity(String s) {
      checkArgument(Severity.ALL.contains(s), "Severity of rule %s is not correct: %s", this, s);
      this.severity = s;
      return this;
    }

    /**
     * The type as defined by the SonarQube Quality Model.
     * <br>
     * When a plugin does not define rule type, then it is deduced from
     * tags:
     * <ul>
     * <li>if the rule has the "bug" tag then type is {@link RuleType#BUG}</li>
     * <li>if the rule has the "security" tag then type is {@link RuleType#VULNERABILITY}</li>
     * <li>if the rule has both tags "bug" and "security", then type is {@link RuleType#BUG}</li>
     * <li>default type is {@link RuleType#CODE_SMELL}</li>
     * </ul>
     * Finally the "bug" and "security" tags are considered as reserved. They
     * are silently removed from the final state of definition.
     *
     * @since 5.5
     */
    public NewRule setType(RuleType t) {
      this.type = t;
      return this;
    }

    /**
     * The optional description, in HTML format, has no max length. It's exclusive with markdown description
     * (see {@link #setMarkdownDescription(String)})
     */
    public NewRule setHtmlDescription(@Nullable String s) {
      checkState(markdownDescription == null, "Rule '%s' already has a Markdown description", this);
      this.htmlDescription = trimToNull(s);
      return this;
    }

    /**
     * Load description from a file available in classpath. Example : <code>setHtmlDescription(getClass().getResource("/myrepo/Rule1234.html")</code>
     */
    public NewRule setHtmlDescription(@Nullable URL classpathUrl) {
      if (classpathUrl != null) {
        try {
          setHtmlDescription(IOUtils.toString(classpathUrl, UTF_8));
        } catch (IOException e) {
          throw new IllegalStateException("Fail to read: " + classpathUrl, e);
        }
      } else {
        this.htmlDescription = null;
      }
      return this;
    }

    /**
     * The optional description, in a restricted Markdown format, has no max length. It's exclusive with HTML description
     * (see {@link #setHtmlDescription(String)})
     */
    public NewRule setMarkdownDescription(@Nullable String s) {
      checkState(htmlDescription == null, "Rule '%s' already has an HTML description", this);
      this.markdownDescription = trimToNull(s);
      return this;
    }

    /**
     * Load description from a file available in classpath. Example : {@code setMarkdownDescription(getClass().getResource("/myrepo/Rule1234.md")}
     */
    public NewRule setMarkdownDescription(@Nullable URL classpathUrl) {
      if (classpathUrl != null) {
        try {
          setMarkdownDescription(IOUtils.toString(classpathUrl, UTF_8));
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
      checkArgument(RuleStatus.REMOVED != status, "Status 'REMOVED' is not accepted on rule '%s'", this);
      this.status = status;
      return this;
    }

    /**
     * SQALE sub-characteristic. See http://www.sqale.org
     *
     * @see org.sonar.api.server.rule.RulesDefinition.SubCharacteristics for constant values
     * @see #setType(RuleType)
     * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model. This method does nothing.
     * See https://jira.sonarsource.com/browse/MMF-184
     */
    public NewRule setDebtSubCharacteristic(@Nullable String s) {
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
     * @deprecated since 5.5, replaced by {@link #setGapDescription(String)}
     */
    @Deprecated
    public NewRule setEffortToFixDescription(@Nullable String s) {
      return setGapDescription(s);
    }

    /**
     * For rules that use LINEAR or LINEAR_OFFSET remediation functions, the meaning
     * of the function parameter (= "gap") must be set. This description
     * explains what 1 point of "gap" represents for the rule.
     * <br>
     * Example: for the "Insufficient condition coverage", this description for the
     * remediation function gap multiplier/base effort would be something like
     * "Effort to test one uncovered condition".
     */
    public NewRule setGapDescription(@Nullable String s) {
      this.gapDescription = s;
      return this;
    }

    /**
     * Create a parameter with given unique key. Max length of key is 128 characters.
     */
    public NewParam createParam(String paramKey) {
      checkArgument(!paramsByKey.containsKey(paramKey), "The parameter '%s' is declared several times on the rule %s", paramKey, this);
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
      if (isEmpty(name)) {
        throw new IllegalStateException(format("Name of rule %s is empty", this));
      }
      if (isEmpty(htmlDescription) && isEmpty(markdownDescription)) {
        throw new IllegalStateException(format("One of HTML description or Markdown description must be defined for rule %s", this));
      }
    }

    /**
     * Register a repository and key under which this rule used to be known
     * (see {@link Rule#deprecatedRuleKeys} for details).
     * <p>
     * Deprecated keys should be added with this method in order, oldest first, for documentation purpose.
     *
     * @since 7.1
     * @throws IllegalArgumentException if {@code repository} or {@code key} is {@code null} or empty.
     * @see Rule#deprecatedRuleKeys
     */
    public NewRule addDeprecatedRuleKey(String repository, String key) {
      deprecatedRuleKeys.add(RuleKey.of(repository, key));
      return this;
    }

    @Override
    public String toString() {
      return format("[repository=%s, key=%s]", repoKey, key);
    }
  }

  @Immutable
  class Rule {
    private final String pluginKey;
    private final Repository repository;
    private final String repoKey;
    private final String key;
    private final String name;
    private final RuleType type;
    private final String htmlDescription;
    private final String markdownDescription;
    private final String internalKey;
    private final String severity;
    private final boolean template;
    private final DebtRemediationFunction debtRemediationFunction;
    private final String gapDescription;
    private final Set<String> tags;
    private final Map<String, Param> params;
    private final RuleStatus status;
    private final boolean activatedByDefault;
    private final RuleScope scope;
    private final Set<RuleKey> deprecatedRuleKeys;

    private Rule(Repository repository, NewRule newRule) {
      this.pluginKey = newRule.pluginKey;
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
      this.debtRemediationFunction = newRule.debtRemediationFunction;
      this.gapDescription = newRule.gapDescription;
      this.scope = newRule.scope == null ? RuleScope.MAIN : newRule.scope;
      this.type = newRule.type == null ? RuleTagsToTypeConverter.convert(newRule.tags) : newRule.type;
      this.tags = ImmutableSortedSet.copyOf(Sets.difference(newRule.tags, RuleTagsToTypeConverter.RESERVED_TAGS));
      Map<String, Param> paramsBuilder = new HashMap<>();
      for (NewParam newParam : newRule.paramsByKey.values()) {
        paramsBuilder.put(newParam.key, new Param(newParam));
      }
      this.params = Collections.unmodifiableMap(paramsBuilder);
      this.activatedByDefault = newRule.activatedByDefault;
      this.deprecatedRuleKeys = ImmutableSortedSet.copyOf(newRule.deprecatedRuleKeys);
    }

    public Repository repository() {
      return repository;
    }

    /**
     * @since 6.6 the plugin the rule was declared in
     */
    @CheckForNull
    public String pluginKey() {
      return pluginKey;
    }

    public String key() {
      return key;
    }

    public String name() {
      return name;
    }

    /**
     * @since 7.1
     * @return
     */
    public RuleScope scope() {
      return scope;
    }

    /**
     * @see NewRule#setType(RuleType)
     * @since 5.5
     */
    public RuleType type() {
      return type;
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

    /**
     * Should this rule be enabled by default. For example in SonarLint standalone.
     *
     * @since 6.0
     */
    public boolean activatedByDefault() {
      return activatedByDefault;
    }

    public RuleStatus status() {
      return status;
    }

    /**
     * @see #type()
     * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model. {@code null} is
     * always returned. See https://jira.sonarsource.com/browse/MMF-184
     */
    @CheckForNull
    @Deprecated
    public String debtSubCharacteristic() {
      return null;
    }

    @CheckForNull
    public DebtRemediationFunction debtRemediationFunction() {
      return debtRemediationFunction;
    }

    /**
     * @deprecated since 5.5, replaced by {@link #gapDescription()}
     */
    @Deprecated
    @CheckForNull
    public String effortToFixDescription() {
      return gapDescription();
    }

    @CheckForNull
    public String gapDescription() {
      return gapDescription;
    }

    @CheckForNull
    public Param param(String key) {
      return params.get(key);
    }

    public List<Param> params() {
      return unmodifiableList(new ArrayList<>(params.values()));
    }

    public Set<String> tags() {
      return tags;
    }

    /**
     * Deprecated rules keys for this rule.
     * <p>
     * If you want to rename the key of a rule or change its repository or both, register the rule's previous repository
     * and key (see {@link NewRule#addDeprecatedRuleKey(String, String) addDeprecatedRuleKey}). This will allow
     * SonarQube to support "issue re-keying" for this rule.
     * <p>
     * If the repository and/or key of an existing rule is changed without declaring deprecated keys, existing issues
     * for this rule, created under the rule's previous repository and/or key, will be closed and new ones will be
     * created under the issue's new repository and/or key.
     * <p>
     * Several deprecated keys can be provided to allow SonarQube to support several key (and/or repository) changes
     * across multiple versions of a plugin.
     * <br>
     * Consider the following use case scenario:
     * <ul>
     *   <li>Rule {@code Foo:A} is defined in version 1 of the plugin
     * <pre>
     * NewRepository newRepository = context.createRepository("Foo", "my_language");
     * NewRule r = newRepository.createRule("A");
     * </pre>
     *   </li>
     *   <li>Rule's key is renamed to B in version 2 of the plugin
     * <pre>
     * NewRepository newRepository = context.createRepository("Foo", "my_language");
     * NewRule r = newRepository.createRule("B")
     *   .addDeprecatedRuleKey("Foo", "A");
     * </pre>
     *   </li>
     *   <li>All rules, including {@code Foo:B}, are moved to a new repository Bar in version 3 of the plugin
     * <pre>
     * NewRepository newRepository = context.createRepository("Bar", "my_language");
     * NewRule r = newRepository.createRule("B")
     *   .addDeprecatedRuleKey("Foo", "A")
     *   .addDeprecatedRuleKey("Bar", "B");
     * </pre>
     *   </li>
     * </ul>
     *
     * With all deprecated keys defined in version 3 of the plugin, SonarQube will be able to support "issue re-keying"
     * for this rule in all cases:
     * <ul>
     *   <li>plugin upgrade from v1 to v2,</li>
     *   <li>plugin upgrade from v2 to v3</li>
     *   <li>AND plugin upgrade from v1 to v3</li>
     * </ul>
     * <p>
     * Finally, repository/key pairs must be unique across all rules and their deprecated keys.
     * <br>
     * This implies that no rule can use the same repository and key as the deprecated key of another rule. This
     * uniqueness applies across plugins.
     * <p>
     * Note that, even though this method returns a {@code Set}, its elements are ordered according to calls to
     * {@link NewRule#addDeprecatedRuleKey(String, String) addDeprecatedRuleKey}. This allows to describe the history
     * of a rule's repositories and keys over time. Oldest repository and key must be specified first.
     *
     * @since 7.1
     * @see NewRule#addDeprecatedRuleKey(String, String)
     */
    public Set<RuleKey> deprecatedRuleKeys() {
      return deprecatedRuleKeys;
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
      return format("[repository=%s, key=%s]", repoKey, key);
    }
  }

  class NewParam {
    private final String key;
    private String name;
    private String description;
    private String defaultValue;
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
     * Plain-text description. Can be null. Max length is 4000 characters.
     */
    public NewParam setDescription(@Nullable String s) {
      this.description = StringUtils.defaultIfBlank(s, null);
      return this;
    }

    /**
     * Empty default value will be converted to null. Max length is 4000 characters.
     */
    public NewParam setDefaultValue(@Nullable String s) {
      this.defaultValue = defaultIfEmpty(s, null);
      return this;
    }
  }

  @Immutable
  class Param {
    private final String key;
    private final String name;
    private final String description;
    private final String defaultValue;
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

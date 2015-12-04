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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.check.Cardinality;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.trim;

/**
 * Loads definitions of rules from a XML file.
 *
 * <h3>Usage</h3>
 * <pre>
 * public class MyJsRulesDefinition implements RulesDefinition {
 *
 *   private static final String PATH = "my-js-rules.xml";
 *   private final RulesDefinitionXmlLoader xmlLoader;
 *
 *   public MyJsRulesDefinition(RulesDefinitionXmlLoader xmlLoader) {
 *     this.xmlLoader = xmlLoader;
 *   }
 *
 *   {@literal @}Override
 *   public void define(Context context) {
 *     try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(PATH), StandardCharsets.UTF_8)) {
 *       NewRepository repository = context.createRepository("my_js", "js").setName("My Javascript Analyzer");
 *       xmlLoader.load(repository, reader);
 *       repository.done();
 *     } catch (IOException e) {
 *       throw new IllegalStateException(String.format("Fail to read file %s", PATH), e);
 *     }
 *   }
 * }
 * </pre>
 *
 * <h3>XML Format</h3>
 * <pre>
 * &lt;rules&gt;
 *   &lt;rule&gt;
 *     &lt;!-- Required key. Max length is 200 characters. --&gt;
 *     &lt;key&gt;the-rule-key&lt;/key&gt;
 *
 *     &lt;!-- Required name. Max length is 200 characters. --&gt;
 *     &lt;name&gt;The purpose of the rule&lt;/name&gt;
 *
 *     &lt;!-- Required description. No max length. --&gt;
 *     &lt;description&gt;
 *       &lt;![CDATA[The description]]&gt;
 *     &lt;/description&gt;
 *     &lt;!-- Optional format of description. Supported values are HTML (default) and MARKDOWN. --&gt;
 *     &lt;descriptionFormat&gt;HTML&lt;/descriptionFormat&gt;
 *
 *     &lt;!-- Optional key for configuration of some rule engines --&gt;
 *     &lt;internalKey&gt;Checker/TreeWalker/LocalVariableName&lt;/internalKey&gt;
 *
 *     &lt;!-- Default severity when enabling the rule in a Quality profile.  --&gt;
 *     &lt;!-- Possible values are INFO, MINOR, MAJOR (default), CRITICAL, BLOCKER. --&gt;
 *     &lt;severity&gt;BLOCKER&lt;/severity&gt;
 *
 *     &lt;!-- Possible values are SINGLE (default) and MULTIPLE for template rules --&gt;
 *     &lt;cardinality&gt;SINGLE&lt;/cardinality&gt;
 *
 *     &lt;!-- Status displayed in rules console. Possible values are BETA, READY (default), DEPRECATED. --&gt;
 *     &lt;status&gt;BETA&lt;/status&gt;
 *
 *     &lt;!-- Optional tags. See org.sonar.api.server.rule.RuleTagFormat. The maximal length of all tags is 4000 characters. --&gt;
 *     &lt;tag&gt;style&lt;/tag&gt;
 *     &lt;tag&gt;security&lt;/tag&gt;
 *
 *     &lt;!-- Optional parameters --&gt;
 *     &lt;param&gt;
 *       &lt;!-- Required key. Max length is 128 characters. --&gt;
 *       &lt;key&gt;the-param-key&lt;/key&gt;
 *       &lt;description&gt;
 *         &lt;![CDATA[the optional description, in HTML format. Max length is 4000 characters.]]&gt;
 *       &lt;/description&gt;
 *       &lt;!-- Optional default value, used when enabling the rule in a Quality profile. Max length is 4000 characters. --&gt;
 *       &lt;defaultValue&gt;42&lt;/defaultValue&gt;
 *     &lt;/param&gt;
 *     &lt;param&gt;
 *       &lt;key&gt;another-param&lt;/key&gt;
 *     &lt;/param&gt;
 *
 *     &lt;!-- SQALE debt - key of sub-characteristic --&gt;
 *     &lt;!-- See {@link org.sonar.api.server.rule.RulesDefinition.SubCharacteristics} for core supported values.
 *     Any other values can be used. If sub-characteristic does not exist at runtime in the SQALE model,
 *     then the rule is created without any sub-characteristic. --&gt;
 *     &lt;!-- Since 5.3 --&gt;
 *     &lt;debtSubCharacteristic&gt;MODULARITY&lt;/debtSubCharacteristic&gt;
 *
 *     &lt;!-- SQALE debt - type of debt remediation function --&gt;
 *     &lt;!-- See enum {@link org.sonar.api.server.debt.DebtRemediationFunction.Type} for supported values --&gt;
 *     &lt;!-- Since 5.3 --&gt;
 *     &lt;debtRemediationFunction&gt;LINEAR_OFFSET&lt;/debtRemediationFunction&gt;
 *
 *     &lt;!-- SQALE debt - raw description of the "effort to fix", used for some types of remediation functions. --&gt;
 *     &lt;!-- See {@link org.sonar.api.server.rule.RulesDefinition.NewRule#setEffortToFixDescription(String)} --&gt;
 *     &lt;!-- Since 5.3 --&gt;
 *     &lt;effortToFixDescription&gt;Effort to test one uncovered condition&lt;/effortToFixDescription&gt;
 *
 *     &lt;!-- SQALE debt - coefficient of debt remediation function. Must be defined only for some function types. --&gt;
 *     &lt;!-- See {@link org.sonar.api.server.rule.RulesDefinition.DebtRemediationFunctions} --&gt;
 *     &lt;!-- Since 5.3 --&gt;
 *     &lt;debtRemediationFunctionCoefficient&gt;10min&lt;/debtRemediationFunctionCoefficient&gt;
 *
 *     &lt;!-- SQALE debt - offset of debt remediation function. Must be defined only for some function types. --&gt;
 *     &lt;!-- See {@link org.sonar.api.server.rule.RulesDefinition.DebtRemediationFunctions} --&gt;
 *     &lt;!-- Since 5.3 --&gt;
 *     &lt;debtRemediationFunctionOffset&gt;2min&lt;/debtRemediationFunctionOffset&gt;
 *
 *     &lt;!-- Deprecated field, replaced by "internalKey" --&gt;
 *     &lt;configKey&gt;Checker/TreeWalker/LocalVariableName&lt;/configKey&gt;
 *
 *     &lt;!-- Deprecated field, replaced by "severity" --&gt;
 *     &lt;priority&gt;BLOCKER&lt;/priority&gt;
 *   &lt;/rule&gt;
 * &lt;/rules&gt;
 * </pre>
 *
 * <h3>XML Example</h3>
 * <pre>
 * &lt;rules&gt;
 *   &lt;rule&gt;
 *     &lt;key&gt;S1442&lt;/key&gt;
 *     &lt;name&gt;"alert(...)" should not be used&lt;/name&gt;
 *     &lt;description&gt;alert(...) can be useful for debugging during development, but ...&lt;/description&gt;
 *     &lt;tag&gt;cwe&lt;/tag&gt;
 *     &lt;tag&gt;security&lt;/tag&gt;
 *     &lt;tag&gt;user-experience&lt;/tag&gt;
 *     &lt;debtSubCharacteristic&gt;SECURITY_FEATURES&lt;/debtSubCharacteristic&gt;
 *     &lt;debtRemediationFunction&gt;CONSTANT_ISSUE&lt;/debtRemediationFunction&gt;
 *     &lt;debtRemediationFunctionOffset&gt;10min&lt;/debtRemediationFunctionOffset&gt;
 *   &lt;/rule&gt;
 *
 *   &lt;!-- another rules... --&gt;
 * &lt;/rules&gt;
 * </pre>
 *
 * @see org.sonar.api.server.rule.RulesDefinition
 * @since 4.3
 */
@ServerSide
public class RulesDefinitionXmlLoader {

  private enum DescriptionFormat {
    HTML, MARKDOWN
  }

  /**
   * Loads rules by reading the XML input stream. The input stream is not always closed by the method, so it
   * should be handled by the caller.
   * @since 4.3
   */
  public void load(RulesDefinition.NewRepository repo, InputStream input, String encoding) {
    load(repo, input, Charset.forName(encoding));
  }

  /**
   * @since 5.1
   */
  public void load(RulesDefinition.NewRepository repo, InputStream input, Charset charset) {
    try (Reader reader = new InputStreamReader(input, charset)) {
      load(repo, reader);
    } catch (IOException e) {
      throw new IllegalStateException("Error while reading XML rules definition for repository " + repo.key(), e);
    }
  }

  /**
   * Loads rules by reading the XML input stream. The reader is not closed by the method, so it
   * should be handled by the caller.
   * @since 4.3
   */
  public void load(RulesDefinition.NewRepository repo, Reader reader) {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    SMInputFactory inputFactory = new SMInputFactory(xmlFactory);
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <rules>

      SMInputCursor rulesC = rootC.childElementCursor("rule");
      while (rulesC.getNext() != null) {
        // <rule>
        processRule(repo, rulesC);
      }

    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML is not valid", e);
    }
  }

  private void processRule(RulesDefinition.NewRepository repo, SMInputCursor ruleC) throws XMLStreamException {
    String key = null;
    String name = null;
    String description = null;
    // enum is not used as variable type as we want to raise an exception with the rule key when format is not supported
    String descriptionFormat = DescriptionFormat.HTML.name();
    String internalKey = null;
    String severity = Severity.defaultSeverity();
    RuleStatus status = RuleStatus.defaultStatus();
    boolean template = false;
    String effortToFixDescription = null;
    String debtSubCharacteristic = null;
    String debtRemediationFunction = null;
    String debtRemediationFunctionOffset = null;
    String debtRemediationFunctionCoeff = null;
    List<ParamStruct> params = new ArrayList<>();
    List<String> tags = new ArrayList<>();

    /* BACKWARD COMPATIBILITY WITH VERY OLD FORMAT */
    String keyAttribute = ruleC.getAttrValue("key");
    if (isNotBlank(keyAttribute)) {
      key = trim(keyAttribute);
    }
    String priorityAttribute = ruleC.getAttrValue("priority");
    if (isNotBlank(priorityAttribute)) {
      severity = trim(priorityAttribute);
    }

    SMInputCursor cursor = ruleC.childElementCursor();
    while (cursor.getNext() != null) {
      String nodeName = cursor.getLocalName();

      if (equalsIgnoreCase("name", nodeName)) {
        name = nodeValue(cursor);

      } else if (equalsIgnoreCase("description", nodeName)) {
        description = nodeValue(cursor);

      } else if (equalsIgnoreCase("descriptionFormat", nodeName)) {
        descriptionFormat = nodeValue(cursor);

      } else if (equalsIgnoreCase("key", nodeName)) {
        key = nodeValue(cursor);

      } else if (equalsIgnoreCase("configKey", nodeName)) {
        // deprecated field, replaced by internalKey
        internalKey = nodeValue(cursor);

      } else if (equalsIgnoreCase("internalKey", nodeName)) {
        internalKey = nodeValue(cursor);

      } else if (equalsIgnoreCase("priority", nodeName)) {
        // deprecated field, replaced by severity
        severity = nodeValue(cursor);

      } else if (equalsIgnoreCase("severity", nodeName)) {
        severity = nodeValue(cursor);

      } else if (equalsIgnoreCase("cardinality", nodeName)) {
        template = Cardinality.MULTIPLE == Cardinality.valueOf(nodeValue(cursor));

      } else if (equalsIgnoreCase("effortToFixDescription", nodeName)) {
        effortToFixDescription = nodeValue(cursor);

      } else if (equalsIgnoreCase("debtRemediationFunction", nodeName)) {
        debtRemediationFunction = nodeValue(cursor);

      } else if (equalsIgnoreCase("debtRemediationFunctionOffset", nodeName)) {
        debtRemediationFunctionOffset = nodeValue(cursor);

      } else if (equalsIgnoreCase("debtRemediationFunctionCoefficient", nodeName)) {
        debtRemediationFunctionCoeff = nodeValue(cursor);

      } else if (equalsIgnoreCase("debtSubCharacteristic", nodeName)) {
        debtSubCharacteristic = nodeValue(cursor);

      } else if (equalsIgnoreCase("status", nodeName)) {
        String s = nodeValue(cursor);
        if (s != null) {
          status = RuleStatus.valueOf(s);
        }

      } else if (equalsIgnoreCase("param", nodeName)) {
        params.add(processParameter(cursor));

      } else if (equalsIgnoreCase("tag", nodeName)) {
        tags.add(nodeValue(cursor));
      }
    }

    try {
      RulesDefinition.NewRule rule = repo.createRule(key)
        .setSeverity(severity)
        .setName(name)
        .setInternalKey(internalKey)
        .setTags(tags.toArray(new String[tags.size()]))
        .setTemplate(template)
        .setStatus(status)
        .setEffortToFixDescription(effortToFixDescription)
        .setDebtSubCharacteristic(debtSubCharacteristic);
      fillDescription(rule, descriptionFormat, description);
      fillRemediationFunction(rule, debtRemediationFunction, debtRemediationFunctionOffset, debtRemediationFunctionCoeff);
      fillParams(rule, params);
    } catch (Exception e) {
      throw new IllegalArgumentException(format("Fail to load the rule with key [%s:%s]", repo.key(), key), e);
    }
  }

  private static void fillDescription(RulesDefinition.NewRule rule, String descriptionFormat, @Nullable String description) {
    if (isNotBlank(description)) {
      switch (DescriptionFormat.valueOf(descriptionFormat)) {
        case HTML:
          rule.setHtmlDescription(description);
          break;
        case MARKDOWN:
          rule.setMarkdownDescription(description);
          break;
        default:
          throw new IllegalArgumentException("Value of descriptionFormat is not supported: " + descriptionFormat);
      }
    }
  }

  private static void fillRemediationFunction(RulesDefinition.NewRule rule, @Nullable String debtRemediationFunction,
    @Nullable String functionOffset, @Nullable String functionCoeff) {
    if (isNotBlank(debtRemediationFunction)) {
      DebtRemediationFunction.Type functionType = DebtRemediationFunction.Type.valueOf(debtRemediationFunction);
      rule.setDebtRemediationFunction(rule.debtRemediationFunctions().create(functionType, functionCoeff, functionOffset));
    }
  }

  private static void fillParams(RulesDefinition.NewRule rule, List<ParamStruct> params) {
    for (ParamStruct param : params) {
      rule.createParam(param.key)
        .setDefaultValue(param.defaultValue)
        .setType(param.type)
        .setDescription(param.description);
    }
  }

  private static String nodeValue(SMInputCursor cursor) throws XMLStreamException {
    return trim(cursor.collectDescendantText(false));
  }

  private static class ParamStruct {

    String key;
    String description;
    String defaultValue;
    RuleParamType type = RuleParamType.STRING;
  }

  private ParamStruct processParameter(SMInputCursor ruleC) throws XMLStreamException {
    ParamStruct param = new ParamStruct();

    // BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT
    String keyAttribute = ruleC.getAttrValue("key");
    if (isNotBlank(keyAttribute)) {
      param.key = trim(keyAttribute);
    }

    // BACKWARD COMPATIBILITY WITH DEPRECATED FORMAT
    String typeAttribute = ruleC.getAttrValue("type");
    if (isNotBlank(typeAttribute)) {
      param.type = RuleParamType.parse(typeAttribute);
    }

    SMInputCursor paramC = ruleC.childElementCursor();
    while (paramC.getNext() != null) {
      String propNodeName = paramC.getLocalName();
      String propText = nodeValue(paramC);
      if (equalsIgnoreCase("key", propNodeName)) {
        param.key = propText;

      } else if (equalsIgnoreCase("description", propNodeName)) {
        param.description = propText;

      } else if (equalsIgnoreCase("type", propNodeName)) {
        param.type = RuleParamType.parse(propText);

      } else if (equalsIgnoreCase("defaultValue", propNodeName)) {
        param.defaultValue = propText;
      }
    }
    return param;
  }
}

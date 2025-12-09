/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.rule;

import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.markdown.Markdown;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;

public class LegacyHotspotRuleDescriptionSectionsGeneratorTest {

  /*
   * Bunch of static constant to create rule description.
   */
  private static final String DESCRIPTION = """
    <p>The use of operators pairs ( <code>=+</code>, <code>=-</code> or <code>=!</code> ) where the reversed, single operator was meant (<code>+=</code>,
    <code>-=</code> or <code>!=</code>) will compile and run, but not produce the expected results.</p>
    <p>This rule raises an issue when <code>=+</code>, <code>=-</code>, or <code>=!</code> is used without any spacing between the two operators and when
    there is at least one whitespace character after.</p>
    """;
  private static final String NONCOMPLIANTCODE = """
    <h2>Noncompliant Code Example</h2>
    <pre>Integer target = -5;
    Integer num = 3;

    target =- num;  // Noncompliant; target = -3. Is that really what's meant?
    target =+ num; // Noncompliant; target = 3
    </pre>
    """;

  private static final String COMPLIANTCODE = """
    <h2>Compliant Solution</h2>
    <pre>Integer target = -5;
    Integer num = 3;

    target = -num;  // Compliant; intent to assign inverse value of num is clear
    target += num;
    </pre>
    """;

  private static final String SEE = """
    <h2>See</h2>
    <ul>
      <li> <a href="https://cwe.mitre.org/data/definitions/352.html">MITRE, CWE-352</a> - Cross-Site Request Forgery (CSRF) </li>
      <li> <a href="https://www.owasp.org/index.php/Top_10-2017_A6-Security_Misconfiguration">OWASP Top 10 2017 Category A6</a> - Security
      Misconfiguration </li>
      <li> <a href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29">OWASP: Cross-Site Request Forgery</a> </li>
      <li> <a href="https://www.sans.org/top25-software-errors/#cat1">SANS Top 25</a> - Insecure Interaction Between Components </li>
      <li> Derived from FindSecBugs rule <a href="https://find-sec-bugs.github.io/bugs.htm#SPRING_CSRF_PROTECTION_DISABLED">SPRING_CSRF_PROTECTION_DISABLED</a> </li>
      <li> <a href="https://docs.spring.io/spring-security/site/docs/current/reference/html/csrf.html#when-to-use-csrf-protection">Spring Security
      Official Documentation: When to use CSRF protection</a> </li>
    </ul>
    """;

  private static final String RECOMMENTEDCODINGPRACTICE = """
    <h2>Recommended Secure Coding Practices</h2>
    <ul>
      <li> activate Spring Security's CSRF protection. </li>
    </ul>
    """;

  private static final String ASKATRISK = """
    <h2>Ask Yourself Whether</h2>
    <ul>
      <li> Any URLs responding with <code>Access-Control-Allow-Origin: *</code> include sensitive content. </li>
      <li> Any domains specified in <code>Access-Control-Allow-Origin</code> headers are checked against a whitelist. </li>
    </ul>
    """;

  private static final String SENSITIVECODE = """
    <h2>Sensitive Code Example</h2>
    <pre>
    // === Java Servlet ===
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      resp.setHeader("Content-Type", "text/plain; charset=utf-8");
      resp.setHeader("Access-Control-Allow-Origin", "http://localhost:8080"); // Questionable
      resp.setHeader("Access-Control-Allow-Credentials", "true"); // Questionable
      resp.setHeader("Access-Control-Allow-Methods", "GET"); // Questionable
      resp.getWriter().write("response");
    }
    </pre>
    <pre>
    // === Spring MVC Controller annotation ===
    @CrossOrigin(origins = "http://domain1.com") // Questionable
    @RequestMapping("")
    public class TestController {
        public String home(ModelMap model) {
            model.addAttribute("message", "ok ");
            return "view";
        }

        @CrossOrigin(origins = "http://domain2.com") // Questionable
        @RequestMapping(value = "/test1")
        public ResponseEntity&lt;String&gt; test1() {
            return ResponseEntity.ok().body("ok");
        }
    }
    </pre>
    """;

  private static final String DEFAULT_SECTION_KEY = "default";

  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final RulesDefinition.Rule rule = mock(RulesDefinition.Rule.class);

  private final LegacyHotspotRuleDescriptionSectionsGenerator generator = new LegacyHotspotRuleDescriptionSectionsGenerator(uuidFactory);

  @Before
  public void setUp() {
    when(rule.htmlDescription()).thenReturn(null);
    when(rule.markdownDescription()).thenReturn(null);
  }

  @Test
  public void parse_returns_all_empty_fields_when_no_description() {
    when(rule.htmlDescription()).thenReturn(null);

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    assertThat(results).isEmpty();
  }

  @Test
  public void parse_returns_all_empty_fields_when_empty_description() {
    when(rule.htmlDescription()).thenReturn("");

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    assertThat(results).isEmpty();
  }

  @Test
  public void parse_to_risk_description_fields_when_desc_contains_no_section() {
    String descriptionWithoutTitles = "description without titles";
    when(rule.htmlDescription()).thenReturn(descriptionWithoutTitles);

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    Map<String, String> sectionKeyToContent = results.stream().collect(toMap(RuleDescriptionSectionDto::getKey, RuleDescriptionSectionDto::getContent));
    assertThat(sectionKeyToContent).hasSize(2)
      .containsEntry(ROOT_CAUSE_SECTION_KEY, descriptionWithoutTitles)
      .containsEntry(DEFAULT_SECTION_KEY, rule.htmlDescription());

  }

  @Test
  public void parse_return_null_risk_when_desc_starts_with_ask_yourself_title() {
    when(rule.htmlDescription()).thenReturn(ASKATRISK + RECOMMENTEDCODINGPRACTICE);

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    Map<String, String> sectionKeyToContent = results.stream().collect(toMap(RuleDescriptionSectionDto::getKey, RuleDescriptionSectionDto::getContent));
    assertThat(sectionKeyToContent).hasSize(3)
      .containsEntry(DEFAULT_SECTION_KEY, rule.htmlDescription())
      .containsEntry(ASSESS_THE_PROBLEM_SECTION_KEY, ASKATRISK)
      .containsEntry(HOW_TO_FIX_SECTION_KEY, RECOMMENTEDCODINGPRACTICE);
  }

  @Test
  public void parse_return_null_vulnerable_when_no_ask_yourself_whether_title() {
    when(rule.htmlDescription()).thenReturn(DESCRIPTION + RECOMMENTEDCODINGPRACTICE);

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    Map<String, String> sectionKeyToContent = results.stream().collect(toMap(RuleDescriptionSectionDto::getKey, RuleDescriptionSectionDto::getContent));
    assertThat(sectionKeyToContent).hasSize(3)
      .containsEntry(DEFAULT_SECTION_KEY, rule.htmlDescription())
      .containsEntry(ROOT_CAUSE_SECTION_KEY, DESCRIPTION)
      .containsEntry(HOW_TO_FIX_SECTION_KEY, RECOMMENTEDCODINGPRACTICE);
  }

  @Test
  public void parse_return_null_fixIt_when_desc_has_no_Recommended_Secure_Coding_Practices_title() {
    when(rule.htmlDescription()).thenReturn(DESCRIPTION + ASKATRISK);

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    Map<String, String> sectionKeyToContent = results.stream().collect(toMap(RuleDescriptionSectionDto::getKey, RuleDescriptionSectionDto::getContent));
    assertThat(sectionKeyToContent).hasSize(3)
      .containsEntry(DEFAULT_SECTION_KEY, rule.htmlDescription())
      .containsEntry(ROOT_CAUSE_SECTION_KEY, DESCRIPTION)
      .containsEntry(ASSESS_THE_PROBLEM_SECTION_KEY, ASKATRISK);
  }

  @Test
  public void parse_with_noncompliant_section_not_removed() {
    when(rule.htmlDescription()).thenReturn(DESCRIPTION + NONCOMPLIANTCODE + COMPLIANTCODE);

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    Map<String, String> sectionKeyToContent = results.stream().collect(toMap(RuleDescriptionSectionDto::getKey, RuleDescriptionSectionDto::getContent));
    assertThat(sectionKeyToContent).hasSize(4)
      .containsEntry(DEFAULT_SECTION_KEY, rule.htmlDescription())
      .containsEntry(ROOT_CAUSE_SECTION_KEY, DESCRIPTION)
      .containsEntry(ASSESS_THE_PROBLEM_SECTION_KEY, NONCOMPLIANTCODE)
      .containsEntry(HOW_TO_FIX_SECTION_KEY, COMPLIANTCODE);
  }

  @Test
  public void parse_moved_noncompliant_code() {
    when(rule.htmlDescription()).thenReturn(DESCRIPTION + RECOMMENTEDCODINGPRACTICE + NONCOMPLIANTCODE + SEE);

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    Map<String, String> sectionKeyToContent = results.stream().collect(toMap(RuleDescriptionSectionDto::getKey, RuleDescriptionSectionDto::getContent));
    assertThat(sectionKeyToContent).hasSize(4)
      .containsEntry(DEFAULT_SECTION_KEY, rule.htmlDescription())
      .containsEntry(ROOT_CAUSE_SECTION_KEY, DESCRIPTION)
      .containsEntry(ASSESS_THE_PROBLEM_SECTION_KEY, NONCOMPLIANTCODE)
      .containsEntry(HOW_TO_FIX_SECTION_KEY, RECOMMENTEDCODINGPRACTICE + SEE);
  }

  @Test
  public void parse_moved_sensitivecode_code() {
    when(rule.htmlDescription()).thenReturn(DESCRIPTION + ASKATRISK + RECOMMENTEDCODINGPRACTICE + SENSITIVECODE + SEE);

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    Map<String, String> sectionKeyToContent = results.stream().collect(toMap(RuleDescriptionSectionDto::getKey, RuleDescriptionSectionDto::getContent));
    assertThat(sectionKeyToContent).hasSize(4)
      .containsEntry(DEFAULT_SECTION_KEY, rule.htmlDescription())
      .containsEntry(ROOT_CAUSE_SECTION_KEY, DESCRIPTION)
      .containsEntry(ASSESS_THE_PROBLEM_SECTION_KEY, ASKATRISK + SENSITIVECODE)
      .containsEntry(HOW_TO_FIX_SECTION_KEY, RECOMMENTEDCODINGPRACTICE + SEE);
  }

  @Test
  public void parse_md_rule_description() {
    String ruleDescription = "This is the custom rule description";
    String exceptionsContent = "This the exceptions section content";
    String askContent = "This is the ask section content";
    String recommendedContent = "This is the recommended section content";

    when(rule.markdownDescription()).thenReturn(ruleDescription + "\n"
      + "== Exceptions" + "\n"
      + exceptionsContent + "\n"
      + "== Ask Yourself Whether" + "\n"
      + askContent + "\n"
      + "== Recommended Secure Coding Practices" + "\n"
      + recommendedContent + "\n");

    Set<RuleDescriptionSectionDto> results = generator.generateSections(rule);

    Map<String, String> sectionKeyToContent = results.stream().collect(toMap(RuleDescriptionSectionDto::getKey, RuleDescriptionSectionDto::getContent));
    assertThat(sectionKeyToContent).hasSize(4)
      .containsEntry(DEFAULT_SECTION_KEY, Markdown.convertToHtml(rule.markdownDescription()))
      .containsEntry(ROOT_CAUSE_SECTION_KEY, ruleDescription + "<br/>"
        + "<h2>Exceptions</h2>"
        + exceptionsContent + "<br/>")
      .containsEntry(ASSESS_THE_PROBLEM_SECTION_KEY, "<h2>Ask Yourself Whether</h2>"
        + askContent + "<br/>")
      .containsEntry(HOW_TO_FIX_SECTION_KEY, "<h2>Recommended Secure Coding Practices</h2>"
        + recommendedContent + "<br/>");

  }

}

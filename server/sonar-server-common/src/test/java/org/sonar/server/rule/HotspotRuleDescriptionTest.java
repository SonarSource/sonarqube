/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class HotspotRuleDescriptionTest {

  @Test
  public void parse_returns_all_empty_fields_when_no_description() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(null);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk()).isEmpty();
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt()).isEmpty();
  }

  @Test
  public void parse_returns_all_empty_fields_when_empty_description() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription("");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk()).isEmpty();
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt()).isEmpty();
  }

  @Test
  @UseDataProvider("descriptionsWithoutTitles")
  public void parse_to_risk_description_fields_when_desc_contains_no_section(String description) {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(description);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo(description);
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt()).isEmpty();
  }

  @DataProvider
  public static Object[][] descriptionsWithoutTitles() {
    return new Object[][] {
      {randomAlphabetic(123)},
      {"bar\n" +
        "acme\n" +
        "foo"}
    };
  }

  @Test
  public void parse_return_null_risk_when_desc_starts_with_ask_yourself_title() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(ASKATRISK + RECOMMENTEDCODINGPRACTICE);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk()).isEmpty();
    assertThat(result.getVulnerable().get()).isEqualTo(ASKATRISK);
    assertThat(result.getFixIt().get()).isEqualTo(RECOMMENTEDCODINGPRACTICE);
  }

  @Test
  public void parse_return_null_vulnerable_when_no_ask_yourself_whether_title() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(DESCRIPTION + RECOMMENTEDCODINGPRACTICE);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo(DESCRIPTION);
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt().get()).isEqualTo(RECOMMENTEDCODINGPRACTICE);
  }

  @Test
  public void parse_return_null_fixIt_when_desc_has_no_Recommended_Secure_Coding_Practices_title() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(DESCRIPTION + ASKATRISK);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo(DESCRIPTION);
    assertThat(result.getVulnerable().get()).isEqualTo(ASKATRISK);
    assertThat(result.getFixIt()).isEmpty();
  }

  @Test
  public void parse_with_noncompliant_section_not_removed() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(DESCRIPTION + NONCOMPLIANTCODE + COMPLIANTCODE);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo(DESCRIPTION);
    assertThat(result.getVulnerable().get()).isEqualTo(NONCOMPLIANTCODE);
    assertThat(result.getFixIt().get()).isEqualTo(COMPLIANTCODE);
  }

  @Test
  public void parse_moved_noncompliant_code() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(DESCRIPTION + RECOMMENTEDCODINGPRACTICE + NONCOMPLIANTCODE + SEE);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo(DESCRIPTION);

    assertThat(result.getVulnerable().get()).isEqualTo(NONCOMPLIANTCODE);
    assertThat(result.getFixIt().get()).isEqualTo(RECOMMENTEDCODINGPRACTICE + SEE);

  }

  @Test
  public void parse_moved_sensitivecode_code() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(DESCRIPTION + ASKATRISK + RECOMMENTEDCODINGPRACTICE + SENSITIVECODE + SEE);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo(DESCRIPTION);
    assertThat(result.getVulnerable().get()).isEqualTo(ASKATRISK + SENSITIVECODE);
    assertThat(result.getFixIt().get()).isEqualTo(RECOMMENTEDCODINGPRACTICE + SEE);
  }

  /*
   * Bunch of static constant to create rule description.
   */
  private static final String DESCRIPTION = "<p>The use of operators pairs ( <code>=+</code>, <code>=-</code> or <code>=!</code> ) where the reversed, single operator was meant (<code>+=</code>,\n"
    +
    "<code>-=</code> or <code>!=</code>) will compile and run, but not produce the expected results.</p>\n" +
    "<p>This rule raises an issue when <code>=+</code>, <code>=-</code>, or <code>=!</code> is used without any spacing between the two operators and when\n" +
    "there is at least one whitespace character after.</p>\n";
  private static final String NONCOMPLIANTCODE = "<h2>Noncompliant Code Example</h2>\n" +
    "<pre>Integer target = -5;\n" +
    "Integer num = 3;\n" +
    "\n" +
    "target =- num;  // Noncompliant; target = -3. Is that really what's meant?\n" +
    "target =+ num; // Noncompliant; target = 3\n" +
    "</pre>\n";

  private static final String COMPLIANTCODE = "<h2>Compliant Solution</h2>\n" +
    "<pre>Integer target = -5;\n" +
    "Integer num = 3;\n" +
    "\n" +
    "target = -num;  // Compliant; intent to assign inverse value of num is clear\n" +
    "target += num;\n" +
    "</pre>\n";

  private static final String SEE = "<h2>See</h2>\n" +
    "<ul>\n" +
    "  <li> <a href=\"https://cwe.mitre.org/data/definitions/352.html\">MITRE, CWE-352</a> - Cross-Site Request Forgery (CSRF) </li>\n" +
    "  <li> <a href=\"https://www.owasp.org/index.php/Top_10-2017_A6-Security_Misconfiguration\">OWASP Top 10 2017 Category A6</a> - Security\n" +
    "  Misconfiguration </li>\n" +
    "  <li> <a href=\"https://www.owasp.org/index.php/Cross-Site_Request_Forgery_%28CSRF%29\">OWASP: Cross-Site Request Forgery</a> </li>\n" +
    "  <li> <a href=\"https://www.sans.org/top25-software-errors/#cat1\">SANS Top 25</a> - Insecure Interaction Between Components </li>\n" +
    "  <li> Derived from FindSecBugs rule <a href=\"https://find-sec-bugs.github.io/bugs.htm#SPRING_CSRF_PROTECTION_DISABLED\">SPRING_CSRF_PROTECTION_DISABLED</a> </li>\n" +
    "  <li> <a href=\"https://docs.spring.io/spring-security/site/docs/current/reference/html/csrf.html#when-to-use-csrf-protection\">Spring Security\n" +
    "  Official Documentation: When to use CSRF protection</a> </li>\n" +
    "</ul>\n";

  private static final String RECOMMENTEDCODINGPRACTICE = "<h2>Recommended Secure Coding Practices</h2>\n" +
    "<ul>\n" +
    "  <li> activate Spring Security's CSRF protection. </li>\n" +
    "</ul>\n";

  private static final String ASKATRISK = "<h2>Ask Yourself Whether</h2>\n" +
    "<ul>\n" +
    "  <li> Any URLs responding with <code>Access-Control-Allow-Origin: *</code> include sensitive content. </li>\n" +
    "  <li> Any domains specified in <code>Access-Control-Allow-Origin</code> headers are checked against a whitelist. </li>\n" +
    "</ul>\n";

  private static final String SENSITIVECODE = "<h2>Sensitive Code Example</h2>\n" +
    "<pre>\n" +
    "// === Java Servlet ===\n" +
    "@Override\n" +
    "protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {\n" +
    "  resp.setHeader(\"Content-Type\", \"text/plain; charset=utf-8\");\n" +
    "  resp.setHeader(\"Access-Control-Allow-Origin\", \"http://localhost:8080\"); // Questionable\n" +
    "  resp.setHeader(\"Access-Control-Allow-Credentials\", \"true\"); // Questionable\n" +
    "  resp.setHeader(\"Access-Control-Allow-Methods\", \"GET\"); // Questionable\n" +
    "  resp.getWriter().write(\"response\");\n" +
    "}\n" +
    "</pre>\n" +
    "<pre>\n" +
    "// === Spring MVC Controller annotation ===\n" +
    "@CrossOrigin(origins = \"http://domain1.com\") // Questionable\n" +
    "@RequestMapping(\"\")\n" +
    "public class TestController {\n" +
    "    public String home(ModelMap model) {\n" +
    "        model.addAttribute(\"message\", \"ok \");\n" +
    "        return \"view\";\n" +
    "    }\n" +
    "\n" +
    "    @CrossOrigin(origins = \"http://domain2.com\") // Questionable\n" +
    "    @RequestMapping(value = \"/test1\")\n" +
    "    public ResponseEntity&lt;String&gt; test1() {\n" +
    "        return ResponseEntity.ok().body(\"ok\");\n" +
    "    }\n" +
    "}\n" +
    "</pre>\n";
}

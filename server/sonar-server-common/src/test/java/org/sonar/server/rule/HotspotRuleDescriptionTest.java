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
  @UseDataProvider("noContentVariants")
  public void parse_returns_all_empty_fields_when_empty_description(String noContent) {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription("");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk()).isEmpty();
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt()).isEmpty();
  }

  @Test
  public void parse_ignores_titles_if_not_h2() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(
      "acme\" +" +
        "<h1>Ask Yourself Whether</h1>\n" +
        "bar\n" +
        "<h1>Recommended Secure Coding Practices</h1>\n" +
        "foo");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk()).isEmpty();
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt()).isEmpty();
  }

  @Test
  @UseDataProvider("whiteSpaceBeforeAndAfterCombinations")
  public void parse_does_not_trim_content_of_h2_titles(String whiteSpaceBefore, String whiteSpaceAfter) {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(
      "acme\" +" +
        "<h2>" + whiteSpaceBefore + "Ask Yourself Whether" + whiteSpaceAfter + "</h2>\n" +
        "bar\n" +
        "<h2>" + whiteSpaceBefore + "Recommended Secure Coding Practices\" + whiteSpaceAfter + \"</h2>\n" +
        "foo");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk()).isEmpty();
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt()).isEmpty();
  }

  @DataProvider
  public static Object[][] whiteSpaceBeforeAndAfterCombinations() {
    String whiteSpace = " ";
    String noWithSpace = "";
    return new Object[][] {
      {noWithSpace, whiteSpace},
      {whiteSpace, noWithSpace},
      {whiteSpace, whiteSpace}
    };
  }

  @Test
  @UseDataProvider("descriptionsWithoutTitles")
  public void parse_return_null_fields_when_desc_contains_neither_title(String description) {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(description);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk()).isEmpty();
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt()).isEmpty();
  }

  @DataProvider
  public static Object[][] descriptionsWithoutTitles() {
    return new Object[][] {
      {""},
      {randomAlphabetic(123)},
      {"bar\n" +
        "acme\n" +
        "foo"}
    };
  }

  @Test
  public void parse_return_null_risk_when_desc_starts_with_ask_yourself_title() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(
      "<h2>Ask Yourself Whether</h2>\n" +
        "bar\n" +
        "<h2>Recommended Secure Coding Practices</h2>\n" +
        "foo");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk()).isEmpty();
    assertThat(result.getVulnerable().get()).isEqualTo("<h2>Ask Yourself Whether</h2>\nbar");
    assertThat(result.getFixIt().get()).isEqualTo("<h2>Recommended Secure Coding Practices</h2>\nfoo");
  }

  @Test
  public void parse_return_null_vulnerable_when_no_ask_yourself_whether_title() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(
        "bar\n" +
        "<h2>Recommended Secure Coding Practices</h2>\n" +
        "foo");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo("bar");
    assertThat(result.getVulnerable()).isEmpty();
    assertThat(result.getFixIt().get()).isEqualTo("<h2>Recommended Secure Coding Practices</h2>\nfoo");
  }

  @Test
  @UseDataProvider("noContentVariants")
  public void parse_returns_vulnerable_with_only_title_when_no_content_between_titles(String noContent) {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(
      "bar\n" +
        "<h2>Ask Yourself Whether</h2>\n" +
        noContent +
        "<h2>Recommended Secure Coding Practices</h2>\n" +
        "foo");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo("bar");
    assertThat(result.getVulnerable().get()).isEqualTo("<h2>Ask Yourself Whether</h2>");
    assertThat(result.getFixIt().get()).isEqualTo("<h2>Recommended Secure Coding Practices</h2>\nfoo");
  }

  @Test
  @UseDataProvider("noContentVariants")
  public void parse_returns_fixIt_with_only_title_when_no_content_after_Recommended_Secure_Coding_Practices_title(String noContent) {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(
      "bar\n" +
        "<h2>Ask Yourself Whether</h2>\n" +
        "bar" +
        "<h2>Recommended Secure Coding Practices</h2>\n" +
        noContent);

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo("bar");
    assertThat(result.getVulnerable().get()).isEqualTo("<h2>Ask Yourself Whether</h2>\nbar");
    assertThat(result.getFixIt().get()).isEqualTo("<h2>Recommended Secure Coding Practices</h2>");
  }

  @DataProvider
  public static Object[][] noContentVariants() {
    return new Object[][] {
      {""},
      {"\n"},
      {" \n "},
      {"\t\n  \n"},
    };
  }

  @Test
  public void parse_return_null_fixIt_when_desc_has_no_Recommended_Secure_Coding_Practices_title() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(
      "bar\n" +
        "<h2>Ask Yourself Whether</h2>\n" +
        "foo");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo("bar");
    assertThat(result.getVulnerable().get()).isEqualTo("<h2>Ask Yourself Whether</h2>\nfoo");
    assertThat(result.getFixIt()).isEmpty();
  }

  @Test
  public void parse_returns_regular_description() {
    RuleDefinitionDto dto = RuleTesting.newRule().setDescription(
      "<p>Enabling Cross-Origin Resource Sharing (CORS) is security-sensitive. For example, it has led in the past to the following vulnerabilities:</p>\n" +
        "<ul>\n" +
        "  <li> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-0269\">CVE-2018-0269</a> </li>\n" +
        "  <li> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-14460\">CVE-2017-14460</a> </li>\n" +
        "</ul>\n" +
        "<p>Applications that enable CORS will effectively relax the same-origin policy in browsers, which is in place to prevent AJAX requests to hosts other\n" +
        "than the one showing in the browser address bar. Being too permissive, CORS can potentially allow an attacker to gain access to sensitive\n" +
        "information.</p>\n" +
        "<p>This rule flags code that enables CORS or specifies any HTTP response headers associated with CORS. The goal is to guide security code reviews.</p>\n" +
        "<h2>Ask Yourself Whether</h2>\n" +
        "<ul>\n" +
        "  <li> Any URLs responding with <code>Access-Control-Allow-Origin: *</code> include sensitive content. </li>\n" +
        "  <li> Any domains specified in <code>Access-Control-Allow-Origin</code> headers are checked against a whitelist. </li>\n" +
        "</ul>\n" +
        "<h2>Recommended Secure Coding Practices</h2>\n" +
        "<ul>\n" +
        "  <li> The <code>Access-Control-Allow-Origin</code> header should be set only on specific URLs that require access from other domains. Don't enable\n" +
        "  the header on the entire domain. </li>\n" +
        "  <li> Don't rely on the <code>Origin</code> header blindly without validation as it could be spoofed by an attacker. Use a whitelist to check that\n" +
        "  the <code>Origin</code> domain (including protocol) is allowed before returning it back in the <code>Access-Control-Allow-Origin</code> header.\n" +
        "  </li>\n" +
        "  <li> Use <code>Access-Control-Allow-Origin: *</code> only if your application absolutely requires it, for example in the case of an open/public API.\n" +
        "  For such endpoints, make sure that there is no sensitive content or information included in the response. </li>\n" +
        "</ul>\n" +
        "<h2>Sensitive Code Example</h2>\n" +
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
        "</pre>\n" +
        "<h2>See</h2>\n" +
        "<ul>\n" +
        "  <li> <a href=\"https://www.owasp.org/index.php/Top_10-2017_A6-Security_Misconfiguration\">OWASP Top 10 2017 Category A6</a> - Security\n" +
        "  Misconfiguration </li>\n" +
        "  <li> <a href=\"https://www.owasp.org/index.php/HTML5_Security_Cheat_Sheet#Cross_Origin_Resource_Sharing\">OWASP HTML5 Security Cheat Sheet</a> - Cross\n" +
        "  Origin Resource Sharing </li>\n" +
        "  <li> <a href=\"https://www.owasp.org/index.php/CORS_OriginHeaderScrutiny\">OWASP CORS OriginHeaderScrutiny</a> </li>\n" +
        "  <li> <a href=\"https://www.owasp.org/index.php/CORS_RequestPreflighScrutiny\">OWASP CORS RequestPreflighScrutiny</a> </li>\n" +
        "  <li> <a href=\"https://cwe.mitre.org/data/definitions/346.html\">MITRE, CWE-346</a> - Origin Validation Error </li>\n" +
        "  <li> <a href=\"https://cwe.mitre.org/data/definitions/942.html\">MITRE, CWE-942</a> - Overly Permissive Cross-domain Whitelist </li>\n" +
        "  <li> <a href=\"https://www.sans.org/top25-software-errors/#cat3\">SANS Top 25</a> - Porous Defenses </li>\n" +
        "</ul>");

    HotspotRuleDescription result = HotspotRuleDescription.from(dto);

    assertThat(result.getRisk().get()).isEqualTo(
      "<p>Enabling Cross-Origin Resource Sharing (CORS) is security-sensitive. For example, it has led in the past to the following vulnerabilities:</p>\n" +
        "<ul>\n" +
        "  <li> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-0269\">CVE-2018-0269</a> </li>\n" +
        "  <li> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2017-14460\">CVE-2017-14460</a> </li>\n" +
        "</ul>\n" +
        "<p>Applications that enable CORS will effectively relax the same-origin policy in browsers, which is in place to prevent AJAX requests to hosts other\n" +
        "than the one showing in the browser address bar. Being too permissive, CORS can potentially allow an attacker to gain access to sensitive\n" +
        "information.</p>\n" +
        "<p>This rule flags code that enables CORS or specifies any HTTP response headers associated with CORS. The goal is to guide security code reviews.</p>");
    assertThat(result.getVulnerable().get()).isEqualTo(
      "<h2>Ask Yourself Whether</h2>\n" +
      "<ul>\n" +
        "  <li> Any URLs responding with <code>Access-Control-Allow-Origin: *</code> include sensitive content. </li>\n" +
        "  <li> Any domains specified in <code>Access-Control-Allow-Origin</code> headers are checked against a whitelist. </li>\n" +
        "</ul>");
    assertThat(result.getFixIt().get()).isEqualTo(
      "<h2>Recommended Secure Coding Practices</h2>\n" +
      "<ul>\n" +
        "  <li> The <code>Access-Control-Allow-Origin</code> header should be set only on specific URLs that require access from other domains. Don't enable\n" +
        "  the header on the entire domain. </li>\n" +
        "  <li> Don't rely on the <code>Origin</code> header blindly without validation as it could be spoofed by an attacker. Use a whitelist to check that\n" +
        "  the <code>Origin</code> domain (including protocol) is allowed before returning it back in the <code>Access-Control-Allow-Origin</code> header.\n" +
        "  </li>\n" +
        "  <li> Use <code>Access-Control-Allow-Origin: *</code> only if your application absolutely requires it, for example in the case of an open/public API.\n" +
        "  For such endpoints, make sure that there is no sensitive content or information included in the response. </li>\n" +
        "</ul>\n" +
        "<h2>Sensitive Code Example</h2>\n" +
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
        "</pre>\n" +
        "<h2>See</h2>\n" +
        "<ul>\n" +
        "  <li> <a href=\"https://www.owasp.org/index.php/Top_10-2017_A6-Security_Misconfiguration\">OWASP Top 10 2017 Category A6</a> - Security\n" +
        "  Misconfiguration </li>\n" +
        "  <li> <a href=\"https://www.owasp.org/index.php/HTML5_Security_Cheat_Sheet#Cross_Origin_Resource_Sharing\">OWASP HTML5 Security Cheat Sheet</a> - Cross\n" +
        "  Origin Resource Sharing </li>\n" +
        "  <li> <a href=\"https://www.owasp.org/index.php/CORS_OriginHeaderScrutiny\">OWASP CORS OriginHeaderScrutiny</a> </li>\n" +
        "  <li> <a href=\"https://www.owasp.org/index.php/CORS_RequestPreflighScrutiny\">OWASP CORS RequestPreflighScrutiny</a> </li>\n" +
        "  <li> <a href=\"https://cwe.mitre.org/data/definitions/346.html\">MITRE, CWE-346</a> - Origin Validation Error </li>\n" +
        "  <li> <a href=\"https://cwe.mitre.org/data/definitions/942.html\">MITRE, CWE-942</a> - Overly Permissive Cross-domain Whitelist </li>\n" +
        "  <li> <a href=\"https://www.sans.org/top25-software-errors/#cat3\">SANS Top 25</a> - Porous Defenses </li>\n" +
        "</ul>");
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.issue.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.trim;
import static org.assertj.core.api.Assertions.assertThat;

public class TrackerTest {

  public static final RuleKey RULE_SYSTEM_PRINT = RuleKey.of("java", "SystemPrint");
  public static final RuleKey RULE_UNUSED_LOCAL_VARIABLE = RuleKey.of("java", "UnusedLocalVariable");
  public static final RuleKey RULE_UNUSED_PRIVATE_METHOD = RuleKey.of("java", "UnusedPrivateMethod");
  public static final RuleKey RULE_NOT_DESIGNED_FOR_EXTENSION = RuleKey.of("java", "NotDesignedForExtension");
  public static final RuleKey RULE_USE_DIAMOND = RuleKey.of("java", "UseDiamond");
  public static final RuleKey RULE_MISSING_PACKAGE_INFO = RuleKey.of("java", "MissingPackageInfo");

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  Tracker<Issue, Issue> tracker = new Tracker<>();

  /**
   * Of course rule must match
   */
  @Test
  public void similar_issues_except_rule_do_not_match() {
    FakeInput baseInput = new FakeInput("H1");
    baseInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg");

    FakeInput rawInput = new FakeInput("H1");
    Issue raw = rawInput.createIssueOnLine(1, RULE_UNUSED_LOCAL_VARIABLE, "msg");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isNull();
  }

  @Test
  public void line_hash_has_greater_priority_than_line() {
    FakeInput baseInput = new FakeInput("H1", "H2", "H3");
    Issue base1 = baseInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg");
    Issue base2 = baseInput.createIssueOnLine(3, RULE_SYSTEM_PRINT, "msg");

    FakeInput rawInput = new FakeInput("a", "b", "H1", "H2", "H3");
    Issue raw1 = rawInput.createIssueOnLine(3, RULE_SYSTEM_PRINT, "msg");
    Issue raw2 = rawInput.createIssueOnLine(5, RULE_SYSTEM_PRINT, "msg");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw1)).isSameAs(base1);
    assertThat(tracking.baseFor(raw2)).isSameAs(base2);
  }

  /**
   * SONAR-2928
   */
  @Test
  public void no_lines_and_different_messages_match() {
    FakeInput baseInput = new FakeInput("H1", "H2", "H3");
    Issue base = baseInput.createIssue(RULE_SYSTEM_PRINT, "msg1");

    FakeInput rawInput = new FakeInput("H10", "H11", "H12");
    Issue raw = rawInput.createIssue(RULE_SYSTEM_PRINT, "msg2");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isSameAs(base);
  }

  @Test
  public void similar_issues_except_message_match() {
    FakeInput baseInput = new FakeInput("H1");
    Issue base = baseInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg1");

    FakeInput rawInput = new FakeInput("H1");
    Issue raw = rawInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg2");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isSameAs(base);
  }

  @Test
  public void similar_issues_if_trimmed_messages_match() {
    FakeInput baseInput = new FakeInput("H1");
    Issue base = baseInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "   message  ");

    FakeInput rawInput = new FakeInput("H2");
    Issue raw = rawInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "message");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isSameAs(base);
  }

  /**
   * Source code of this line was changed, but line and message still match
   */
  @Test
  public void similar_issues_except_line_hash_match() {
    FakeInput baseInput = new FakeInput("H1");
    Issue base = baseInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg");

    FakeInput rawInput = new FakeInput("H2");
    Issue raw = rawInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isSameAs(base);
  }

  @Test
  public void similar_issues_except_line_match() {
    FakeInput baseInput = new FakeInput("H1", "H2");
    Issue base = baseInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg");

    FakeInput rawInput = new FakeInput("H2", "H1");
    Issue raw = rawInput.createIssueOnLine(2, RULE_SYSTEM_PRINT, "msg");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isSameAs(base);
  }

  /**
   * SONAR-2812
   */
  @Test
  public void only_same_line_hash_match_match() {
    FakeInput baseInput = new FakeInput("H1", "H2");
    Issue base = baseInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg");

    FakeInput rawInput = new FakeInput("H3", "H4", "H1");
    Issue raw = rawInput.createIssueOnLine(3, RULE_SYSTEM_PRINT, "other message");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isSameAs(base);
  }

  @Test
  public void do_not_fail_if_base_issue_without_line() {
    FakeInput baseInput = new FakeInput("H1", "H2");
    Issue base = baseInput.createIssueOnLine(1, RULE_SYSTEM_PRINT, "msg1");

    FakeInput rawInput = new FakeInput("H3", "H4", "H5");
    Issue raw = rawInput.createIssue(RULE_UNUSED_LOCAL_VARIABLE, "msg2");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isNull();
    assertThat(tracking.getUnmatchedBases()).containsOnly(base);
  }

  @Test
  public void do_not_fail_if_raw_issue_without_line() {
    FakeInput baseInput = new FakeInput("H1", "H2");
    Issue base = baseInput.createIssue(RULE_SYSTEM_PRINT, "msg1");

    FakeInput rawInput = new FakeInput("H3", "H4", "H5");
    Issue raw = rawInput.createIssueOnLine(1, RULE_UNUSED_LOCAL_VARIABLE, "msg2");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw)).isNull();
    assertThat(tracking.getUnmatchedBases()).containsOnly(base);
  }

  @Test
  public void do_not_fail_if_raw_line_does_not_exist() {
    FakeInput baseInput = new FakeInput();
    FakeInput rawInput = new FakeInput("H1").addIssue(new Issue(200, "H200", RULE_SYSTEM_PRINT, "msg", org.sonar.api.issue.Issue.STATUS_OPEN, new Date()));

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);

    assertThat(tracking.getUnmatchedRaws()).hasSize(1);
  }

  /**
   * SONAR-3072
   */
  @Test
  public void recognize_blocks_1() {
    FakeInput baseInput = FakeInput.createForSourceLines(
      "package example1;",
      "",
      "public class Toto {",
      "",
      "    public void doSomething() {",
      "        // doSomething",
      "        }",
      "",
      "    public void doSomethingElse() {",
      "        // doSomethingElse",
      "        }",
      "}");
    Issue base1 = baseInput.createIssueOnLine(7, RULE_SYSTEM_PRINT, "Indentation");
    Issue base2 = baseInput.createIssueOnLine(11, RULE_SYSTEM_PRINT, "Indentation");

    FakeInput rawInput = FakeInput.createForSourceLines(
      "package example1;",
      "",
      "public class Toto {",
      "",
      "    public Toto(){}",
      "",
      "    public void doSomethingNew() {",
      "        // doSomethingNew",
      "        }",
      "",
      "    public void doSomethingElseNew() {",
      "        // doSomethingElseNew",
      "        }",
      "",
      "    public void doSomething() {",
      "        // doSomething",
      "        }",
      "",
      "    public void doSomethingElse() {",
      "        // doSomethingElse",
      "        }",
      "}");
    Issue raw1 = rawInput.createIssueOnLine(9, RULE_SYSTEM_PRINT, "Indentation");
    Issue raw2 = rawInput.createIssueOnLine(13, RULE_SYSTEM_PRINT, "Indentation");
    Issue raw3 = rawInput.createIssueOnLine(17, RULE_SYSTEM_PRINT, "Indentation");
    Issue raw4 = rawInput.createIssueOnLine(21, RULE_SYSTEM_PRINT, "Indentation");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw1)).isNull();
    assertThat(tracking.baseFor(raw2)).isNull();
    assertThat(tracking.baseFor(raw3)).isSameAs(base1);
    assertThat(tracking.baseFor(raw4)).isSameAs(base2);
    assertThat(tracking.getUnmatchedBases()).isEmpty();
  }

  // SONAR-10194
  @Test
  public void no_match_if_only_same_rulekey() {
    FakeInput baseInput = FakeInput.createForSourceLines(
      "package aa;",
      "",
      "/**",
      " * Hello world",
      " *",
      " */",
      "public class App {",
      "",
      "    public static void main(String[] args) {",
      "",
      "        int magicNumber = 42;",
      "",
      "        String s = new String(\"Very long line that does not meet our maximum 120 character line length criteria and should be wrapped to avoid SonarQube issues.\");\r\n"
        +
        "    }",
      "}");
    Issue base1 = baseInput.createIssueOnLine(11, RuleKey.of("squid", "S109"), "Assign this magic number 42 to a well-named constant, and use the constant instead.");
    Issue base2 = baseInput.createIssueOnLine(13, RuleKey.of("squid", "S00103"), "Split this 163 characters long line (which is greater than 120 authorized).");

    FakeInput rawInput = FakeInput.createForSourceLines(
      "package aa;",
      "",
      "/**",
      " * Hello world",
      " *",
      " */",
      "public class App {",
      "",
      "    public static void main(String[] args) {",
      "        ",
      "        System.out.println(\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque vel diam purus. Curabitur ut nisi lacus....\");",
      "        ",
      "        int a = 0;",
      "        ",
      "        int x = a + 123;",
      "    }",
      "}");
    Issue raw1 = rawInput.createIssueOnLine(11, RuleKey.of("squid", "S00103"), "Split this 139 characters long line (which is greater than 120 authorized).");
    Issue raw2 = rawInput.createIssueOnLine(15, RuleKey.of("squid", "S109"), "Assign this magic number 123 to a well-named constant, and use the constant instead.");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw1)).isNull();
    assertThat(tracking.baseFor(raw2)).isNull();
    assertThat(tracking.getUnmatchedBases()).hasSize(2);
  }

  /**
   * SONAR-3072
   */
  @Test
  public void recognize_blocks_2() {
    FakeInput baseInput = FakeInput.createForSourceLines(
      "package example2;",
      "",
      "public class Toto {",
      "  void method1() {",
      "    System.out.println(\"toto\");",
      "  }",
      "}");
    Issue base1 = baseInput.createIssueOnLine(5, RULE_SYSTEM_PRINT, "SystemPrintln");

    FakeInput rawInput = FakeInput.createForSourceLines(
      "package example2;",
      "",
      "public class Toto {",
      "",
      "  void method2() {",
      "    System.out.println(\"toto\");",
      "  }",
      "",
      "  void method1() {",
      "    System.out.println(\"toto\");",
      "  }",
      "",
      "  void method3() {",
      "    System.out.println(\"toto\");",
      "  }",
      "}");
    Issue raw1 = rawInput.createIssueOnLine(6, RULE_SYSTEM_PRINT, "SystemPrintln");
    Issue raw2 = rawInput.createIssueOnLine(10, RULE_SYSTEM_PRINT, "SystemPrintln");
    Issue raw3 = rawInput.createIssueOnLine(14, RULE_SYSTEM_PRINT, "SystemPrintln");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.baseFor(raw1)).isNull();
    assertThat(tracking.baseFor(raw2)).isSameAs(base1);
    assertThat(tracking.baseFor(raw3)).isNull();
  }

  @Test
  public void recognize_blocks_3() {
    FakeInput baseInput = FakeInput.createForSourceLines(
      "package sample;",
      "",
      "public class Sample {",
      "\t",
      "\tpublic Sample(int i) {",
      "\t\tint j = i+1;", // UnusedLocalVariable
      "\t}",
      "",
      "\tpublic boolean avoidUtilityClass() {", // NotDesignedForExtension
      "\t\treturn true;",
      "\t}",
      "",
      "\tprivate String myMethod() {", // UnusedPrivateMethod
      "\t\treturn \"hello\";",
      "\t}",
      "}");
    Issue base1 = baseInput.createIssueOnLine(6, RULE_UNUSED_LOCAL_VARIABLE, "Avoid unused local variables such as 'j'.");
    Issue base2 = baseInput.createIssueOnLine(13, RULE_UNUSED_PRIVATE_METHOD, "Avoid unused private methods such as 'myMethod()'.");
    Issue base3 = baseInput.createIssueOnLine(9, RULE_NOT_DESIGNED_FOR_EXTENSION,
      "Method 'avoidUtilityClass' is not designed for extension - needs to be abstract, final or empty.");

    FakeInput rawInput = FakeInput.createForSourceLines(
      "package sample;",
      "",
      "public class Sample {",
      "",
      "\tpublic Sample(int i) {",
      "\t\tint j = i+1;", // UnusedLocalVariable is still there
      "\t}",
      "\t",
      "\tpublic boolean avoidUtilityClass() {", // NotDesignedForExtension is still there
      "\t\treturn true;",
      "\t}",
      "\t",
      "\tprivate String myMethod() {", // issue UnusedPrivateMethod is fixed because it's called at line 18
      "\t\treturn \"hello\";",
      "\t}",
      "",
      "  public void newIssue() {",
      "    String msg = myMethod();", // new issue UnusedLocalVariable
      "  }",
      "}");

    Issue newRaw = rawInput.createIssueOnLine(18, RULE_UNUSED_LOCAL_VARIABLE, "Avoid unused local variables such as 'msg'.");
    Issue rawSameAsBase1 = rawInput.createIssueOnLine(6, RULE_UNUSED_LOCAL_VARIABLE, "Avoid unused local variables such as 'j'.");
    Issue rawSameAsBase3 = rawInput.createIssueOnLine(9, RULE_NOT_DESIGNED_FOR_EXTENSION,
      "Method 'avoidUtilityClass' is not designed for extension - needs to be abstract, final or empty.");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);

    assertThat(tracking.baseFor(newRaw)).isNull();
    assertThat(tracking.baseFor(rawSameAsBase1)).isSameAs(base1);
    assertThat(tracking.baseFor(rawSameAsBase3)).isSameAs(base3);
    assertThat(tracking.getUnmatchedBases()).containsOnly(base2);
  }

  /**
   * https://jira.sonarsource.com/browse/SONAR-7595
   */
  @Test
  public void match_only_one_issue_when_multiple_blocks_match_the_same_block() {
    FakeInput baseInput = FakeInput.createForSourceLines(
      "public class Toto {",
      "  private final Deque<Set<DataItem>> one = new ArrayDeque<Set<DataItem>>();",
      "  private final Deque<Set<DataItem>> two = new ArrayDeque<Set<DataItem>>();",
      "  private final Deque<Integer> three = new ArrayDeque<Integer>();",
      "  private final Deque<Set<Set<DataItem>>> four = new ArrayDeque<Set<DataItem>>();");
    Issue base1 = baseInput.createIssueOnLine(2, RULE_USE_DIAMOND, "Use diamond");
    baseInput.createIssueOnLine(3, RULE_USE_DIAMOND, "Use diamond");
    baseInput.createIssueOnLine(4, RULE_USE_DIAMOND, "Use diamond");
    baseInput.createIssueOnLine(5, RULE_USE_DIAMOND, "Use diamond");

    FakeInput rawInput = FakeInput.createForSourceLines(
      "public class Toto {",
      "  // move all lines",
      "  private final Deque<Set<DataItem>> one = new ArrayDeque<Set<DataItem>>();",
      "  private final Deque<Set<DataItem>> two = new ArrayDeque<>();",
      "  private final Deque<Integer> three = new ArrayDeque<>();",
      "  private final Deque<Set<Set<DataItem>>> four = new ArrayDeque<>();");
    Issue raw1 = rawInput.createIssueOnLine(3, RULE_USE_DIAMOND, "Use diamond");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.getUnmatchedBases()).hasSize(3);
    assertThat(tracking.baseFor(raw1)).isEqualTo(base1);
  }

  @Test
  public void match_issues_with_same_rule_key_on_project_level() {
    FakeInput baseInput = new FakeInput();
    Issue base1 = baseInput.createIssue(RULE_MISSING_PACKAGE_INFO, "[com.test:abc] Missing package-info.java in package.");
    Issue base2 = baseInput.createIssue(RULE_MISSING_PACKAGE_INFO, "[com.test:abc/def] Missing package-info.java in package.");

    FakeInput rawInput = new FakeInput();
    Issue raw1 = rawInput.createIssue(RULE_MISSING_PACKAGE_INFO, "[com.test:abc/def] Missing package-info.java in package.");
    Issue raw2 = rawInput.createIssue(RULE_MISSING_PACKAGE_INFO, "[com.test:abc] Missing package-info.java in package.");

    Tracking<Issue, Issue> tracking = tracker.trackNonClosed(rawInput, baseInput);
    assertThat(tracking.getUnmatchedBases()).hasSize(0);
    assertThat(tracking.baseFor(raw1)).isEqualTo(base2);
    assertThat(tracking.baseFor(raw2)).isEqualTo(base1);
  }

  private static class Issue implements Trackable {
    private final RuleKey ruleKey;
    private final Integer line;
    private final String message, lineHash;
    private final String status;
    private final Date updateDate;

    Issue(@Nullable Integer line, String lineHash, RuleKey ruleKey, String message, String status, Date updateDate) {
      this.line = line;
      this.lineHash = lineHash;
      this.ruleKey = ruleKey;
      this.status = status;
      this.updateDate = updateDate;
      this.message = trim(message);
    }

    @Override
    public Integer getLine() {
      return line;
    }

    @Override
    public String getMessage() {
      return message;
    }

    @Override
    public String getLineHash() {
      return lineHash;
    }

    @Override
    public RuleKey getRuleKey() {
      return ruleKey;
    }

    @Override
    public String getStatus() {
      return status;
    }

    @Override
    public Date getUpdateDate() {
      return updateDate;
    }
  }

  private static class FakeInput implements Input<Issue> {
    private final List<Issue> issues = new ArrayList<>();
    private final List<String> lineHashes;

    FakeInput(String... lineHashes) {
      this.lineHashes = asList(lineHashes);
    }

    static FakeInput createForSourceLines(String... lines) {
      String[] hashes = new String[lines.length];
      for (int i = 0; i < lines.length; i++) {
        hashes[i] = DigestUtils.md5Hex(lines[i].replaceAll("[\t ]", ""));
      }
      return new FakeInput(hashes);
    }

    Issue createIssueOnLine(int line, RuleKey ruleKey, String message) {
      Issue issue = new Issue(line, lineHashes.get(line - 1), ruleKey, message, org.sonar.api.issue.Issue.STATUS_OPEN, new Date());
      issues.add(issue);
      return issue;
    }

    /**
     * No line (line 0)
     */
    Issue createIssue(RuleKey ruleKey, String message) {
      Issue issue = new Issue(null, "", ruleKey, message, org.sonar.api.issue.Issue.STATUS_OPEN, new Date());
      issues.add(issue);
      return issue;
    }

    FakeInput addIssue(Issue issue) {
      this.issues.add(issue);
      return this;
    }

    @Override
    public LineHashSequence getLineHashSequence() {
      return new LineHashSequence(lineHashes);
    }

    @Override
    public BlockHashSequence getBlockHashSequence() {
      return new BlockHashSequence(getLineHashSequence(), 2);
    }

    @Override
    public Collection<Issue> getIssues() {
      return issues;
    }
  }
}

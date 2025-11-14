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
package org.sonar.server.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.server.rule.RulesDefinition.OwaspAsvsVersion;
import org.sonar.api.server.rule.RulesDefinition.PciDssVersion;
import org.sonar.api.server.rule.RulesDefinition.StigVersion;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.sonar.api.server.rule.RulesDefinition.PciDssVersion.V3_2;
import static org.sonar.server.security.SecurityStandards.VulnerabilityProbability.HIGH;
import static org.sonar.server.security.SecurityStandards.VulnerabilityProbability.LOW;
import static org.sonar.server.security.SecurityStandards.VulnerabilityProbability.MEDIUM;

@Immutable
public final class SecurityStandards {

  public static final String UNKNOWN_STANDARD = "unknown";

  /**
   * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
   */
  @Deprecated(since = "10.0", forRemoval = true)
  public static final String SANS_TOP_25_INSECURE_INTERACTION = "insecure-interaction";
  @Deprecated(since = "10.0", forRemoval = true)
  public static final String SANS_TOP_25_RISKY_RESOURCE = "risky-resource";
  @Deprecated(since = "10.0", forRemoval = true)
  public static final String SANS_TOP_25_POROUS_DEFENSES = "porous-defenses";

  private static final String OWASP_MOBILE_TOP10_2024_PREFIX = "owaspMobileTop10-2024:";
  private static final String OWASP_TOP10_PREFIX = "owaspTop10:";
  private static final String OWASP_TOP10_2021_PREFIX = "owaspTop10-2021:";
  private static final String PCI_DSS_32_PREFIX = V3_2.prefix() + ":";
  private static final String PCI_DSS_40_PREFIX = PciDssVersion.V4_0.prefix() + ":";
  private static final String OWASP_ASVS_40_PREFIX = OwaspAsvsVersion.V4_0.prefix() + ":";
  private static final String CWE_PREFIX = "cwe:";
  // See https://www.sans.org/top25-software-errors

  /**
   * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
   */
  @Deprecated(since = "10.0", forRemoval = true)
  private static final Set<String> INSECURE_CWE = new HashSet<>(asList("89", "78", "79", "434", "352", "601"));
  @Deprecated(since = "10.0", forRemoval = true)
  private static final Set<String> RISKY_CWE = new HashSet<>(asList("120", "22", "494", "829", "676", "131", "134", "190"));
  @Deprecated(since = "10.0", forRemoval = true)
  private static final Set<String> POROUS_CWE = new HashSet<>(asList("306", "862", "798", "311", "807", "250", "863", "732", "327", "307", "759"));

  /**
   * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
   */
  @Deprecated
  public static final Map<String, Set<String>> CWES_BY_SANS_TOP_25 = ImmutableMap.of(
    SANS_TOP_25_INSECURE_INTERACTION, INSECURE_CWE,
    SANS_TOP_25_RISKY_RESOURCE, RISKY_CWE,
    SANS_TOP_25_POROUS_DEFENSES, POROUS_CWE);

  // https://cwe.mitre.org/top25/archive/2021/2021_cwe_top25.html
  public static final List<String> CWE_TOP25_2021 = List.of("787", "79", "125", "20", "78", "89", "416", "22", "352", "434", "306", "190", "502", "287", "476",
    "798", "119", "862", "276", "200", "522", "732", "611", "918", "77");

  // https://cwe.mitre.org/top25/archive/2022/2022_cwe_top25.html
  public static final List<String> CWE_TOP25_2022 = List.of("787", "79", "89", "20", "125", "78", "416", "22", "352", "434", "476", "502", "190", "287", "798",
    "862", "77", "306", "119", "276", "918", "362", "400", "611", "94");

  // https://cwe.mitre.org/top25/archive/2023/2023_top25_list.html#tableView
  public static final List<String> CWE_TOP25_2023 = List.of("787", "79", "89", "416", "78", "20", "125", "22", "352", "434", "862", "476", "287", "190", "502",
    "77", "119", "798", "918", "306", "362", "269", "94", "863", "276");

  // https://cwe.mitre.org/top25/archive/2024/2024_cwe_top25.html#tableView
  public static final List<String> CWE_TOP25_2024 = List.of("79", "787", "89", "352", "22", "125", "78", "416", "862", "434", "94", "20", "77", "287", "269",
    "502", "200", "863", "918", "119", "476", "798", "190", "400", "306");

  public static final String CWE_YEAR_2021 = "2021";
  public static final String CWE_YEAR_2022 = "2022";
  public static final String CWE_YEAR_2023 = "2023";
  public static final String CWE_YEAR_2024 = "2024";

  public static final Map<String, List<String>> CWES_BY_CWE_TOP_25 = Map.of(
    CWE_YEAR_2021, CWE_TOP25_2021,
    CWE_YEAR_2022, CWE_TOP25_2022,
    CWE_YEAR_2023, CWE_TOP25_2023,
    CWE_YEAR_2024, CWE_TOP25_2024);

  private static final List<String> OWASP_ASVS_40_LEVEL_1 = List.of("2.1.1", "2.1.10", "2.1.11", "2.1.12", "2.1.2", "2.1.3", "2.1.4", "2.1.5", "2.1.6", "2.1.7", "2.1.8", "2.1.9",
    "2.10.1", "2.10.2", "2.10.3", "2.10.4", "2.2.1", "2.2.2", "2.2.3", "2.3.1", "2.5.1", "2.5.2", "2.5.3", "2.5.4", "2.5.5", "2.5.6", "2.7.1", "2.7.2", "2.7.3", "2.7.4", "2.8.1",
    "3.1.1", "3.2.1", "3.2.2", "3.2.3", "3.3.1", "3.3.2", "3.4.1", "3.4.2", "3.4.3", "3.4.4", "3.4.5", "3.7.1", "4.1.1", "4.1.2", "4.1.3", "4.1.4", "4.1.5", "4.2.1", "4.2.2",
    "4.3.1", "4.3.2", "5.1.1", "5.1.2", "5.1.3", "5.1.4", "5.1.5", "5.2.1", "5.2.2", "5.2.3", "5.2.4", "5.2.5", "5.2.6", "5.2.7", "5.2.8", "5.3.1", "5.3.10", "5.3.2", "5.3.3",
    "5.3.4", "5.3.5", "5.3.6", "5.3.7", "5.3.8", "5.3.9", "5.5.1", "5.5.2", "5.5.3", "5.5.4", "6.2.1", "7.1.1", "7.1.2", "7.4.1", "8.2.1", "8.2.2", "8.2.3", "8.3.1", "8.3.2",
    "8.3.3", "8.3.4", "9.1.1", "9.1.2", "9.1.3", "10.3.1", "10.3.2", "10.3.3", "11.1.1", "11.1.2", "11.1.3", "11.1.4", "11.1.5", "12.1.1", "12.3.1", "12.3.2", "12.3.3", "12.3.4",
    "12.3.5", "12.4.1", "12.4.2", "12.5.1", "12.5.2", "12.6.1", "13.1.1", "13.1.2", "13.1.3", "13.2.1", "13.2.2", "13.2.3", "13.3.1", "14.2.1", "14.2.2", "14.2.3", "14.3.1",
    "14.3.2", "14.3.3", "14.4.1", "14.4.2", "14.4.3", "14.4.4", "14.4.5", "14.4.6", "14.4.7", "14.5.1", "14.5.2", "14.5.3");

  private static final List<String> OWASP_ASVS_40_LEVEL_2 = Stream.concat(Stream.of("1.1.1", "1.1.2", "1.1.3", "1.1.4", "1.1.5", "1.1.6",
    "1.1.7", "1.10.1", "1.11.1", "1.11.2", "1.12.1", "1.12.2", "1.14.1", "1.14.2", "1.14.3", "1.14.4", "1.14.5", "1.14.6", "1.2.1", "1.2.2", "1.2.3", "1.2.4", "1.4.1", "1.4.2",
    "1.4.3", "1.4.4", "1.4.5", "1.5.1", "1.5.2", "1.5.3", "1.5.4", "1.6.1", "1.6.2", "1.6.3", "1.6.4", "1.7.1", "1.7.2", "1.8.1", "1.8.2", "1.9.1", "1.9.2", "2.3.2", "2.3.3",
    "2.4.1", "2.4.2", "2.4.3", "2.4.4", "2.4.5", "2.5.7", "2.6.1", "2.6.2", "2.6.3", "2.7.5", "2.7.6", "2.8.2", "2.8.3", "2.8.4", "2.8.5", "2.8.6", "2.9.1", "2.9.2", "2.9.3",
    "3.2.4", "3.3.3", "3.3.4", "3.5.1", "3.5.2", "3.5.3", "4.3.3", "5.4.1", "5.4.2", "5.4.3", "6.1.1", "6.1.2", "6.1.3", "6.2.2", "6.2.3", "6.2.4", "6.2.5", "6.2.6", "6.3.1",
    "6.3.2", "6.4.1", "6.4.2", "7.1.3", "7.1.4", "7.2.1", "7.2.2", "7.3.1", "7.3.2", "7.3.3", "7.3.4", "7.4.2", "7.4.3", "8.1.1", "8.1.2", "8.1.3", "8.1.4", "8.3.5", "8.3.6",
    "8.3.7", "8.3.8", "9.2.1", "9.2.2", "9.2.3", "9.2.4", "10.2.1", "10.2.2", "11.1.6", "11.1.7", "11.1.8", "12.1.2", "12.1.3", "12.2.1", "12.3.6", "13.1.4", "13.1.5", "13.2.4",
    "13.2.5", "13.2.6", "13.3.2", "13.4.1", "13.4.2", "14.1.1", "14.1.2", "14.1.3", "14.1.4", "14.2.4", "14.2.5", "14.2.6", "14.5.4"), OWASP_ASVS_40_LEVEL_1.stream())
    .toList();

  private static final List<String> OWASP_ASVS_40_LEVEL_3 = Stream
    .concat(Stream.of("1.11.3", "2.2.4", "2.2.5", "2.2.6", "2.2.7", "2.8.7", "3.6.1", "3.6.2", "6.2.7", "6.2.8", "6.3.3", "8.1.5",
      "8.1.6", "9.2.5", "10.1.1", "10.2.3", "10.2.4", "10.2.5", "10.2.6", "14.1.5"), OWASP_ASVS_40_LEVEL_2.stream())
    .toList();

  public static final Map<Integer, List<String>> OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL = Map.of(
    1, OWASP_ASVS_40_LEVEL_1,
    2, OWASP_ASVS_40_LEVEL_2,
    3, OWASP_ASVS_40_LEVEL_3);

  public static final Map<OwaspAsvsVersion, Map<Integer, List<String>>> OWASP_ASVS_REQUIREMENTS_BY_LEVEL = Map.of(
    OwaspAsvsVersion.V4_0, OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL);

  public enum VulnerabilityProbability {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int score;

    VulnerabilityProbability(int index) {
      this.score = index;
    }

    public int getScore() {
      return score;
    }

    public static Optional<VulnerabilityProbability> byScore(@Nullable Integer score) {
      if (score == null) {
        return Optional.empty();
      }
      return Arrays.stream(values())
        .filter(t -> t.score == score)
        .findFirst();
    }
  }

  public enum SQCategory {
    BUFFER_OVERFLOW("buffer-overflow", HIGH),
    SQL_INJECTION("sql-injection", HIGH),
    RCE("rce", MEDIUM),
    OBJECT_INJECTION("object-injection", LOW),
    COMMAND_INJECTION("command-injection", HIGH),
    PATH_TRAVERSAL_INJECTION("path-traversal-injection", HIGH),
    LDAP_INJECTION("ldap-injection", LOW),
    XPATH_INJECTION("xpath-injection", LOW),
    LOG_INJECTION("log-injection", LOW),
    XXE("xxe", MEDIUM),
    XSS("xss", HIGH),
    DOS("dos", MEDIUM),
    SSRF("ssrf", MEDIUM),
    CSRF("csrf", HIGH),
    HTTP_RESPONSE_SPLITTING("http-response-splitting", LOW),
    OPEN_REDIRECT("open-redirect", MEDIUM),
    WEAK_CRYPTOGRAPHY("weak-cryptography", MEDIUM),
    AUTH("auth", HIGH),
    INSECURE_CONF("insecure-conf", LOW),
    FILE_MANIPULATION("file-manipulation", LOW),
    ENCRYPTION_OF_SENSITIVE_DATA("encrypt-data", LOW),
    TRACEABILITY("traceability", LOW),
    PERMISSION("permission", MEDIUM),
    OTHERS("others", LOW);

    private static final Map<String, SQCategory> SQ_CATEGORY_BY_KEY = stream(values()).collect(Collectors.toMap(SQCategory::getKey, Function.identity()));
    private final String key;
    private final VulnerabilityProbability vulnerability;

    SQCategory(String key, VulnerabilityProbability vulnerability) {
      this.key = key;
      this.vulnerability = vulnerability;
    }

    public String getKey() {
      return key;
    }

    public VulnerabilityProbability getVulnerability() {
      return vulnerability;
    }

    public static Optional<SQCategory> fromKey(@Nullable String key) {
      return Optional.ofNullable(key).map(SQ_CATEGORY_BY_KEY::get);
    }
  }

  public enum StigSupportedRequirement {
    V222612("V-222612"),
    V222578("V-222578"),
    V222577("V-222577"),
    V222609("V-222609"),
    V222608("V-222608"),
    V222602("V-222602"),
    V222607("V-222607"),
    V222604("V-222604"),
    V222550("V-222550"),
    V222596("V-222596"),
    V222620("V-222620"),
    V222542("V-222542"),
    V222642("V-222642"),
    V222567("V-222567"),
    V222618("V-222618"),
    V222610("V-222610"),
    V222579("V-222579"),
    V222615("V-222615"),
    V222575("V-222575"),
    V222576("V-222576"),
    V222562("V-222562"),
    V222563("V-222563"),
    V222603("V-222603"),
    V222606("V-222606"),
    V222605("V-222605"),
    V222391("V-222391"),
    V222588("V-222588"),
    V222582("V-222582"),
    V222519("V-222519"),
    V222599("V-222599"),
    V222593("V-222593"),
    V222597("V-222597"),
    V222594("V-222594"),
    V222397("V-222397"),
    V222534("V-222534"),
    V222641("V-222641"),
    V222598("V-222598"),
    V254803("V-254803"),
    V222667("V-222667"),
    V222653("V-222653"),
    V222649("V-222649");

    private final String requirement;

    StigSupportedRequirement(String requirement) {

      this.requirement = requirement;
    }

    public String getRequirement() {
      return requirement;
    }
  }

  public enum PciDss {
    R1("1"), R2("2"), R3("3"), R4("4"), R5("5"), R6("6"), R7("7"), R8("8"), R9("9"), R10("10"), R11("11"), R12("12");

    private final String category;

    PciDss(String category) {
      this.category = category;
    }

    public String category() {
      return category;
    }
  }

  public enum OwaspAsvs {
    C1("1"), C2("2"), C3("3"), C4("4"), C5("5"), C6("6"), C7("7"), C8("8"), C9("9"), C10("10"), C11("11"), C12("12"), C13("13"), C14("14");

    private final String category;

    OwaspAsvs(String category) {
      this.category = category;
    }

    public String category() {
      return category;
    }
  }

  public static final Map<SQCategory, Set<String>> CWES_BY_SQ_CATEGORY = ImmutableMap.<SQCategory, Set<String>>builder()
    .put(SQCategory.BUFFER_OVERFLOW, Set.of("119", "120", "131", "676", "788"))
    .put(SQCategory.SQL_INJECTION, Set.of("89", "564", "943"))
    .put(SQCategory.COMMAND_INJECTION, Set.of("77", "78", "88", "214"))
    .put(SQCategory.PATH_TRAVERSAL_INJECTION, Set.of("22"))
    .put(SQCategory.LDAP_INJECTION, Set.of("90"))
    .put(SQCategory.XPATH_INJECTION, Set.of("643"))
    .put(SQCategory.RCE, Set.of("94", "95"))
    .put(SQCategory.DOS, Set.of("400", "624"))
    .put(SQCategory.SSRF, Set.of("918"))
    .put(SQCategory.CSRF, Set.of("352"))
    .put(SQCategory.XSS, Set.of("79", "80", "81", "82", "83", "84", "85", "86", "87"))
    .put(SQCategory.LOG_INJECTION, Set.of("117"))
    .put(SQCategory.HTTP_RESPONSE_SPLITTING, Set.of("113"))
    .put(SQCategory.OPEN_REDIRECT, Set.of("601"))
    .put(SQCategory.XXE, Set.of("611", "827"))
    .put(SQCategory.OBJECT_INJECTION, Set.of("134", "470", "502"))
    .put(SQCategory.WEAK_CRYPTOGRAPHY, Set.of("295", "297", "321", "322", "323", "324", "325", "326", "327", "328", "330", "780"))
    .put(SQCategory.AUTH, Set.of("798", "640", "620", "549", "522", "521", "263", "262", "261", "259", "308"))
    .put(SQCategory.INSECURE_CONF, Set.of("102", "215", "346", "614", "489", "942"))
    .put(SQCategory.FILE_MANIPULATION, Set.of("97", "73"))
    .put(SQCategory.ENCRYPTION_OF_SENSITIVE_DATA, Set.of("311", "315", "319"))
    .put(SQCategory.TRACEABILITY, Set.of("778"))
    .put(SQCategory.PERMISSION, Set.of("266", "269", "284", "668", "732"))
    .build();
  private static final Ordering<SQCategory> SQ_CATEGORY_ORDERING = Ordering.explicit(stream(SQCategory.values()).toList());
  public static final Ordering<String> SQ_CATEGORY_KEYS_ORDERING = Ordering.explicit(stream(SQCategory.values()).map(SQCategory::getKey).toList());

  public static final Map<String, String> CWES_BY_CASA_CATEGORY;

  static {
    Map<String, String> map = new HashMap<>();
    map.put("1.1.4", "1059");
    map.put("1.14.6", "477");
    map.put("1.4.1", "602");
    map.put("1.8.1", null);
    map.put("1.8.2", null);
    map.put("2.1.1", "521");
    map.put("2.3.1", "330");
    map.put("2.4.1", "916");
    map.put("2.5.4", "16");
    map.put("2.6.1", "308");
    map.put("2.7.2", "287");
    map.put("2.7.6", "310");
    map.put("3.3.1", "613");
    map.put("3.3.3", "613");
    map.put("3.4.1", "614");
    map.put("3.4.2", "1004");
    map.put("3.4.3", "1275");
    map.put("3.5.2", "798");
    map.put("3.5.3", "345");
    map.put("3.7.1", "306");
    map.put("4.1.1", "602");
    map.put("4.1.2", "639");
    map.put("4.1.3", "285");
    map.put("4.1.5", "285");
    map.put("4.2.1", "639");
    map.put("4.2.2", "352");
    map.put("4.3.1", "419");
    map.put("4.3.2", "548");
    map.put("5.1.1", "235");
    map.put("5.1.5", "601");
    map.put("5.2.3", "147");
    map.put("5.2.4", "95");
    map.put("5.2.5", "94");
    map.put("5.2.6", "918");
    map.put("5.2.7", "159");
    map.put("5.3.1", "116");
    map.put("5.3.10", "643");
    map.put("5.3.3", "79");
    map.put("5.3.4", "89");
    map.put("5.3.6", "830");
    map.put("5.3.7", "90");
    map.put("5.3.8", "78");
    map.put("5.3.9", "829");
    map.put("5.5.2", "611");
    map.put("6.1.1", "311");
    map.put("6.2.1", "310");
    map.put("6.2.3", "326");
    map.put("6.2.4", "326");
    map.put("6.2.7", "326");
    map.put("6.2.8", "385");
    map.put("6.3.2", "338");
    map.put("6.4.2", "320");
    map.put("7.1.1", "532");
    map.put("8.1.1", "524");
    map.put("8.2.2", "922");
    map.put("8.3.1", "319");
    map.put("8.3.5", "532");
    map.put("9.1.2", "326");
    map.put("9.2.1", "295");
    map.put("9.2.4", "299");
    map.put("10.3.2", "353");
    map.put("10.3.3", "350");
    map.put("11.1.4", "770");
    map.put("12.4.1", "552");
    map.put("12.4.2", "509");
    map.put("13.1.3", "598");
    map.put("13.1.4", "285");
    map.put("13.2.1", "650");
    map.put("14.1.1", null);
    map.put("14.1.4", null);
    map.put("14.1.5", null);
    map.put("14.3.2", "497");
    map.put("14.5.2", "346");
    CWES_BY_CASA_CATEGORY = Collections.unmodifiableMap(map);
  }

  public static final List<String> CASA_ROOT_CATEGORIES =
    IntStream.rangeClosed(1, 14).mapToObj(String::valueOf).collect(Collectors.toUnmodifiableList());

  private final Set<String> standards;
  private final Set<String> cwe;
  private final Set<String> casaCategories;
  private final SQCategory sqCategory;
  private final Set<SQCategory> ignoredSQCategories;

  private SecurityStandards(Set<String> standards, Set<String> cwe, Set<String> casaCategories,
    SQCategory sqCategory, Set<SQCategory> ignoredSQCategories) {
    this.standards = standards;
    this.cwe = cwe;
    this.casaCategories = casaCategories;
    this.sqCategory = sqCategory;
    this.ignoredSQCategories = ignoredSQCategories;
  }

  public Set<String> getStandards() {
    return standards;
  }

  public Set<String> getCwe() {
    return cwe;
  }

  public Set<String> getPciDss32() {
    return getMatchingStandards(standards, PCI_DSS_32_PREFIX);
  }

  public Set<String> getPciDss40() {
    return getMatchingStandards(standards, PCI_DSS_40_PREFIX);
  }

  public Set<String> getOwaspAsvs40() {
    return getMatchingStandards(standards, OWASP_ASVS_40_PREFIX);
  }

  public Set<String> getOwaspMobileTop10For2024() {
    return getMatchingStandards(standards, OWASP_MOBILE_TOP10_2024_PREFIX);
  }

  public Set<String> getOwaspTop10() {
    return getMatchingStandards(standards, OWASP_TOP10_PREFIX);
  }

  public Set<String> getOwaspTop10For2021() {
    return getMatchingStandards(standards, OWASP_TOP10_2021_PREFIX);
  }

  public Set<String> getStig(StigVersion version) {
    return getMatchingStandards(standards, version.prefix() + ":");
  }

  public Set<String> getCasa() {
    return casaCategories;
  }

  /**
   * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
   */
  @Deprecated
  public Set<String> getSansTop25() {
    return toSansTop25(cwe);
  }

  public Set<String> getCweTop25() {
    return toCweTop25(cwe);
  }

  public SQCategory getSqCategory() {
    return sqCategory;
  }

  /**
   * If CWEs mapped to multiple {@link SQCategory}, those which are not taken into account are listed here.
   */
  public Set<SQCategory> getIgnoredSQCategories() {
    return ignoredSQCategories;
  }

  /**
   * @throws IllegalStateException if {@code securityStandards} maps to multiple {@link SQCategory SQCategories}
   */
  public static SecurityStandards fromSecurityStandards(Set<String> securityStandards) {
    Set<String> standards = securityStandards.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    Set<String> cwe = toCwes(standards);
    List<SQCategory> sq = toSortedSQCategories(cwe);
    SQCategory sqCategory = sq.iterator().next();
    Set<SQCategory> ignoredSQCategories = sq.stream().skip(1).collect(Collectors.toSet());
    Set<String> casaCategories = toCasaCategories(cwe);
    return new SecurityStandards(standards, cwe, casaCategories, sqCategory, ignoredSQCategories);
  }

  public static Set<String> getRequirementsForCategoryAndLevel(String category, int level) {
    return OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL.get(level).stream()
      .filter(req -> req.startsWith(category + "."))
      .collect(Collectors.toSet());
  }

  public static Set<String> getRequirementsForCategoryAndLevel(OwaspAsvs category, int level) {
    return getRequirementsForCategoryAndLevel(category.category(), level);
  }

  private static Set<String> getMatchingStandards(Set<String> securityStandards, String prefix) {
    return securityStandards.stream()
      .filter(s -> s.startsWith(prefix))
      .map(s -> s.substring(prefix.length()))
      .collect(Collectors.toSet());
  }

  private static Set<String> toCwes(Collection<String> securityStandards) {
    Set<String> result = securityStandards.stream()
      .filter(s -> s.startsWith(CWE_PREFIX))
      .map(s -> s.substring(CWE_PREFIX.length()))
      .collect(Collectors.toSet());
    return result.isEmpty() ? singleton(UNKNOWN_STANDARD) : result;
  }

  private static Set<String> toCweTop25(Set<String> cwe) {
    return CWES_BY_CWE_TOP_25
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(CWES_BY_CWE_TOP_25.get(k)::contains))
      .collect(Collectors.toSet());
  }

  private static Set<String> toSansTop25(Collection<String> cwe) {
    return CWES_BY_SANS_TOP_25
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(CWES_BY_SANS_TOP_25.get(k)::contains))
      .collect(Collectors.toSet());
  }

  private static List<SQCategory> toSortedSQCategories(Collection<String> cwe) {
    List<SQCategory> result = CWES_BY_SQ_CATEGORY
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(CWES_BY_SQ_CATEGORY.get(k)::contains))
      .sorted(SQ_CATEGORY_ORDERING)
      .toList();
    return result.isEmpty() ? singletonList(SQCategory.OTHERS) : result;
  }

  private static Set<String> toCasaCategories(Set<String> cwe) {
    return CWES_BY_CASA_CATEGORY
      .keySet()
      .stream()
      .filter(k -> cwe.contains(CWES_BY_CASA_CATEGORY.get(k)))
      .collect(Collectors.toSet());
  }

}

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
package org.sonar.server.security;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class SecurityStandardHelper {

  public static final String UNKNOWN_STANDARD = "unknown";
  public static final String SANS_TOP_25_INSECURE_INTERACTION = "insecure-interaction";
  public static final String SANS_TOP_25_RISKY_RESOURCE = "risky-resource";
  public static final String SANS_TOP_25_POROUS_DEFENSES = "porous-defenses";

  private static final String OWASP_TOP10_PREFIX = "owaspTop10:";
  private static final String CWE_PREFIX = "cwe:";
  // See https://www.sans.org/top25-software-errors
  private static final Set<String> INSECURE_CWE = new HashSet<>(asList("89", "78", "79", "434", "352", "601"));
  private static final Set<String> RISKY_CWE = new HashSet<>(asList("120", "22", "494", "829", "676", "131", "134", "190"));
  private static final Set<String> POROUS_CWE = new HashSet<>(asList("306", "862", "798", "311", "807", "250", "863", "732", "327", "307", "759"));
  public static final Map<String, Set<String>> SANS_TOP_25_CWE_MAPPING = ImmutableMap.of(
    SANS_TOP_25_INSECURE_INTERACTION, INSECURE_CWE,
    SANS_TOP_25_RISKY_RESOURCE, RISKY_CWE,
    SANS_TOP_25_POROUS_DEFENSES, POROUS_CWE);

  public static final Map<String, Set<String>> SONARSOURCE_CWE_MAPPING = ImmutableMap.<String, Set<String>>builder()
    .put("sql-injection", ImmutableSet.of("89", "564"))
    .put("command-injection", ImmutableSet.of("77", "78", "88", "214"))
    .put("path-traversal-injection", ImmutableSet.of("22"))
    .put("ldap-injection", ImmutableSet.of("90"))
    .put("xpath-injection", ImmutableSet.of("643"))
    .put("rce", ImmutableSet.of("94", "95"))
    .put("dos", ImmutableSet.of("400", "624"))
    .put("ssrf", ImmutableSet.of("918"))
    .put("csrf", ImmutableSet.of("352"))
    .put("xss", ImmutableSet.of("79", "80", "81", "82", "83", "84", "85", "86", "87"))
    .put("log-injection", ImmutableSet.of("117"))
    .put("http-response-splitting", ImmutableSet.of("113"))
    .put("open-redirect", ImmutableSet.of("601"))
    .put("xxe", ImmutableSet.of("611", "827"))
    .put("object-injection", ImmutableSet.of("134", "470", "502"))
    .put("weak-cryptography", ImmutableSet.of("295", "297", "321", "322", "323", "324", "325", "326", "327", "328", "330", "780"))
    .put("auth", ImmutableSet.of("798", "640", "620", "549", "522", "521", "263", "262", "261", "259", "284"))
    .put("insecure-conf", ImmutableSet.of("102", "215", "311", "315", "346", "614", "489", "942"))
    .put("file-manipulation", ImmutableSet.of("97", "73"))
    .build();
  public static final String SONARSOURCE_OTHER_CWES_CATEGORY = "others";
  public static final Ordering<String> SONARSOURCE_CATEGORY_ORDERING = Ordering.explicit(
    ImmutableList.<String>builder().addAll(SONARSOURCE_CWE_MAPPING.keySet()).add(SONARSOURCE_OTHER_CWES_CATEGORY).build());

  private static final Splitter SECURITY_STANDARDS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private SecurityStandardHelper() {
    // Utility class
  }

  public static List<String> getSecurityStandards(@Nullable String securityStandards) {
    return securityStandards == null ? emptyList() : SECURITY_STANDARDS_SPLITTER.splitToList(securityStandards);
  }

  public static List<String> getSansTop25(Collection<String> cwe) {
    return SANS_TOP_25_CWE_MAPPING
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(SANS_TOP_25_CWE_MAPPING.get(k)::contains))
      .collect(toList());
  }

  public static List<String> getSonarSourceSecurityCategories(Collection<String> cwe) {
    List<String> result = SONARSOURCE_CWE_MAPPING
      .keySet()
      .stream()
      .filter(k -> cwe.stream().anyMatch(SONARSOURCE_CWE_MAPPING.get(k)::contains))
      .collect(toList());
    return result.isEmpty() ? singletonList(SONARSOURCE_OTHER_CWES_CATEGORY) : result;
  }

  public static List<String> getOwaspTop10(Collection<String> securityStandards) {
    return securityStandards.stream()
      .filter(s -> s.startsWith(OWASP_TOP10_PREFIX))
      .map(s -> s.substring(OWASP_TOP10_PREFIX.length()))
      .collect(toList());
  }

  public static List<String> getCwe(Collection<String> securityStandards) {
    List<String> result = securityStandards.stream()
      .filter(s -> s.startsWith(CWE_PREFIX))
      .map(s -> s.substring(CWE_PREFIX.length()))
      .collect(toList());
    return result.isEmpty() ? singletonList(UNKNOWN_STANDARD) : result;
  }

  public static List<String> getSansTop25(String securityStandards) {
    return getSansTop25(getCwe(getSecurityStandards(securityStandards)));
  }

  public static List<String> getSonarSourceSecurityCategories(String securityStandards) {
    return getSonarSourceSecurityCategories(getCwe(getSecurityStandards(securityStandards)));
  }

  public static List<String> getOwaspTop10(String securityStandards) {
    return getOwaspTop10(getSecurityStandards(securityStandards));
  }

  public static List<String> getCwe(String securityStandards) {
    return getCwe(getSecurityStandards(securityStandards));
  }
}

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
package org.sonar.core.issue.tracking;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

public class Tracking<RAW extends Trackable, BASE extends Trackable> {

  /**
   * Tracked issues -> a raw issue is associated to a base issue
   */
  private final IdentityHashMap<RAW, BASE> rawToBase = new IdentityHashMap<>();

  /**
   * The raw issues that are not associated to a base issue.
   */
  private final Set<RAW> unmatchedRaws = Collections.newSetFromMap(new IdentityHashMap<RAW, Boolean>());

  /**
   * IdentityHashSet of the base issues that are not associated to a raw issue.
   */
  private final Set<BASE> unmatchedBases = Collections.newSetFromMap(new IdentityHashMap<BASE, Boolean>());

  /**
   * The manual issues that are still valid (related source code still exists). They
   * are grouped by line. Lines start with 1. The key 0 references the manual
   * issues that do not relate to a line.
   */
  private final Multimap<Integer, BASE> openManualIssues = ArrayListMultimap.create();

  public Tracking(Input<RAW> rawInput, Input<BASE> baseInput) {
    for (RAW raw : rawInput.getIssues()) {
      // Extra verification if some plugins create issues on wrong lines
      Integer rawLine = raw.getLine();
      if (rawLine != null && !rawInput.getLineHashSequence().hasLine(rawLine)) {
        throw new IllegalArgumentException("Issue line is not valid: " + raw);
      }
      this.unmatchedRaws.add(raw);
    }
    this.unmatchedBases.addAll(baseInput.getIssues());
  }

  public Set<RAW> getUnmatchedRaws() {
    return unmatchedRaws;
  }

  public Map<RAW, BASE> getMatchedRaws() {
    return rawToBase;
  }

  @CheckForNull
  public BASE baseFor(RAW raw) {
    return rawToBase.get(raw);
  }

  /**
   * The base issues that are not matched by a raw issue and that need to be closed. Manual
   */
  public Set<BASE> getUnmatchedBases() {
    return unmatchedBases;
  }

  boolean containsUnmatchedBase(BASE base) {
    return unmatchedBases.contains(base);
  }

  void associateRawToBase(RAW raw, BASE base) {
    rawToBase.put(raw, base);
    unmatchedBases.remove(base);
  }

  void markRawAsAssociated(RAW raw) {
    unmatchedRaws.remove(raw);
  }

  void markRawsAsAssociated(Collection<RAW> c) {
    unmatchedRaws.removeAll(c);
  }

  boolean isComplete() {
    return unmatchedRaws.isEmpty() || unmatchedBases.isEmpty();
  }

  public Multimap<Integer, BASE> getOpenManualIssuesByLine() {
    return openManualIssues;
  }

  void associateManualIssueToLine(BASE manualIssue, int line) {
    openManualIssues.put(line, manualIssue);
    unmatchedBases.remove(manualIssue);
  }
}

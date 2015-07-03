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

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Tracking<RAW extends Trackable, BASE extends Trackable> {

  /**
   * Matched issues -> a raw issue is associated to a base issue
   */
  private final IdentityHashMap<RAW, BASE> rawToBase = new IdentityHashMap<>();
  private final IdentityHashMap<BASE, RAW> baseToRaw = new IdentityHashMap<>();

  private final Collection<RAW> raws;
  private final Collection<BASE> bases;

  private final Predicate<RAW> unmatchedRawPredicate = new Predicate<RAW>() {
    @Override
    public boolean apply(@Nonnull RAW raw) {
      return !rawToBase.containsKey(raw);
    }
  };

  private final Predicate<BASE> unmatchedBasePredicate = new Predicate<BASE>() {
    @Override
    public boolean apply(@Nonnull BASE raw) {
      return !baseToRaw.containsKey(raw);
    }
  };

  /**
   * The manual issues that are still valid (related source code still exists). They
   * are grouped by line. Lines start with 1. The key 0 references the manual
   * issues that do not relate to a line.
   */
  private final Multimap<Integer, BASE> openManualIssuesByLine = ArrayListMultimap.create();

  public Tracking(Input<RAW> rawInput, Input<BASE> baseInput) {
    this.raws = rawInput.getIssues();
    this.bases = baseInput.getIssues();
  }

  /**
   * Returns an Iterable to be traversed when matching issues. That means
   * that the traversal does not fail if method {@link #match(Trackable, Trackable)}
   * is called.
   */
  public Iterable<RAW> getUnmatchedRaws() {
    return Iterables.filter(raws, unmatchedRawPredicate);
  }

  public Map<RAW, BASE> getMatchedRaws() {
    return rawToBase;
  }

  @CheckForNull
  public BASE baseFor(RAW raw) {
    return rawToBase.get(raw);
  }

  /**
   * The base issues that are not matched by a raw issue and that need to be closed.
   */
  public Iterable<BASE> getUnmatchedBases() {
    return Iterables.filter(bases, unmatchedBasePredicate);
  }

  boolean containsUnmatchedBase(BASE base) {
    return !baseToRaw.containsKey(base);
  }

  void match(RAW raw, BASE base) {
    rawToBase.put(raw, base);
    baseToRaw.put(base, raw);
  }

  boolean isComplete() {
    return rawToBase.size() == raws.size();
  }

  public Multimap<Integer, BASE> getOpenManualIssuesByLine() {
    return openManualIssuesByLine;
  }

  void keepManualIssueOpen(BASE manualIssue, @Nullable Integer line) {
    openManualIssuesByLine.put(line, manualIssue);
    baseToRaw.put(manualIssue, null);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("rawToBase", rawToBase)
      .add("baseToRaw", baseToRaw)
      .add("raws", raws)
      .add("bases", bases)
      .add("openManualIssuesByLine", openManualIssuesByLine)
      .toString();
  }
}

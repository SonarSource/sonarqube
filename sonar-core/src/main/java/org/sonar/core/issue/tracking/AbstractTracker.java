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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;

import static java.util.Comparator.comparing;

public class AbstractTracker<RAW extends Trackable, BASE extends Trackable> {

  protected void match(Tracking<RAW, BASE> tracking, Function<Trackable, SearchKey> searchKeyFactory) {
    if (tracking.isComplete()) {
      return;
    }

    Multimap<SearchKey, BASE> baseSearch = ArrayListMultimap.create();
    tracking.getUnmatchedBases()
      .forEach(base -> baseSearch.put(searchKeyFactory.apply(base), base));

    tracking.getUnmatchedRaws().forEach(raw -> {
      SearchKey rawKey = searchKeyFactory.apply(raw);
      Collection<BASE> bases = baseSearch.get(rawKey);
      bases.stream()
        .sorted(comparing(this::statusRank).reversed()
          .thenComparing(comparing(Trackable::getCreationDate)))
        .findFirst()
        .ifPresent(match -> {
          tracking.match(raw, match);
          baseSearch.remove(rawKey, match);
        });
    });
  }

  private int statusRank(BASE i) {
    switch (i.getStatus()) {
      case Issue.STATUS_RESOLVED:
        return 2;
      case Issue.STATUS_CONFIRMED:
        return 1;
      default:
        return 0;
    }
  }

  protected interface SearchKey {
  }

  protected static class LineAndLineHashKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String lineHash;
    private final Integer line;

    protected LineAndLineHashKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.line = trackable.getLine();
      this.lineHash = StringUtils.defaultString(trackable.getLineHash(), "");
    }

    @Override
    public boolean equals(@Nonnull Object o) {
      if (this == o) {
        return true;
      }
      LineAndLineHashKey that = (LineAndLineHashKey) o;
      // start with most discriminant field
      return Objects.equals(line, that.line)
        && lineHash.equals(that.lineHash)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, lineHash, line != null ? line : 0);
    }
  }

  protected static class LineAndLineHashAndMessage implements SearchKey {
    private final RuleKey ruleKey;
    private final String lineHash;
    private final String message;
    private final Integer line;

    protected LineAndLineHashAndMessage(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.line = trackable.getLine();
      this.message = trackable.getMessage();
      this.lineHash = StringUtils.defaultString(trackable.getLineHash(), "");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LineAndLineHashAndMessage that = (LineAndLineHashAndMessage) o;
      // start with most discriminant field
      return Objects.equals(line, that.line)
        && lineHash.equals(that.lineHash)
        && message.equals(that.message)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, lineHash, message, line != null ? line : 0);
    }
  }

  protected static class LineHashAndMessageKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String message;
    private final String lineHash;

    LineHashAndMessageKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.message = trackable.getMessage();
      this.lineHash = StringUtils.defaultString(trackable.getLineHash(), "");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LineHashAndMessageKey that = (LineHashAndMessageKey) o;
      // start with most discriminant field
      return lineHash.equals(that.lineHash)
        && message.equals(that.message)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, message, lineHash);
    }
  }

  protected static class LineAndMessageKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String message;
    private final Integer line;

    LineAndMessageKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.message = trackable.getMessage();
      this.line = trackable.getLine();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LineAndMessageKey that = (LineAndMessageKey) o;
      // start with most discriminant field
      return Objects.equals(line, that.line)
        && message.equals(that.message)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, message, line != null ? line : 0);
    }
  }

  protected static class LineHashKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String lineHash;

    LineHashKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.lineHash = StringUtils.defaultString(trackable.getLineHash(), "");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LineHashKey that = (LineHashKey) o;
      // start with most discriminant field
      return lineHash.equals(that.lineHash)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, lineHash);
    }
  }

}

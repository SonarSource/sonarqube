/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
        // Choose the more recently updated issue first to get the latest changes in siblings
        .sorted(comparing(Trackable::getUpdateDate).reversed())
        .findFirst()
        .ifPresent(match -> {
          tracking.match(raw, match);
          baseSearch.remove(rawKey, match);
        });
    });
  }

  protected interface SearchKey {
  }

  protected static class LineAndLineHashKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String lineHash;
    private final Integer line;
    private final String cveId;

    protected LineAndLineHashKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.line = trackable.getLine();
      this.lineHash = Objects.toString(trackable.getLineHash(), "");
      this.cveId = trackable.getCveId();
    }

    @Override
    public boolean equals(@Nonnull Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LineAndLineHashKey that = (LineAndLineHashKey) o;
      // start with most discriminant field
      return Objects.equals(line, that.line)
        && lineHash.equals(that.lineHash)
        && ruleKey.equals(that.ruleKey)
        && Objects.equals(cveId, that.cveId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, lineHash, line != null ? line : 0, cveId);
    }
  }

  protected static class LineAndLineHashAndMessage implements SearchKey {
    private final RuleKey ruleKey;
    private final String lineHash;
    private final String message;
    private final Integer line;
    private final String cveId;

    protected LineAndLineHashAndMessage(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.line = trackable.getLine();
      this.message = trackable.getMessage();
      this.lineHash = Objects.toString(trackable.getLineHash(), "");
      this.cveId = trackable.getCveId();
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
        && Objects.equals(message, that.message)
        && ruleKey.equals(that.ruleKey)
        && Objects.equals(cveId, that.cveId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, lineHash, message, line != null ? line : 0, cveId);
    }
  }

  protected static class LineHashAndMessageKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String message;
    private final String lineHash;
    private final String cveId;

    LineHashAndMessageKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.message = trackable.getMessage();
      this.lineHash = Objects.toString(trackable.getLineHash(), "");
      this.cveId = trackable.getCveId();
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
        && Objects.equals(message, that.message)
        && ruleKey.equals(that.ruleKey)
        && Objects.equals(cveId, that.cveId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, message, lineHash, cveId);
    }
  }

  protected static class LineAndMessageKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String message;
    private final Integer line;
    private final String cveId;

    LineAndMessageKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.message = trackable.getMessage();
      this.line = trackable.getLine();
      this.cveId = trackable.getCveId();
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
        && Objects.equals(message, that.message)
        && ruleKey.equals(that.ruleKey)
        && Objects.equals(cveId, that.cveId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, message, line != null ? line : 0);
    }
  }

  protected static class LineHashKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String lineHash;
    private final String cveId;

    LineHashKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.lineHash = Objects.toString(trackable.getLineHash(), "");
      this.cveId = trackable.getCveId();
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
      return lineHash.equals(that.lineHash) && ruleKey.equals(that.ruleKey) && Objects.equals(cveId, that.cveId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleKey, lineHash, cveId);
    }
  }

}

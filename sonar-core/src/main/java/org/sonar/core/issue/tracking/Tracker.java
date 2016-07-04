/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.rule.RuleKey;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class Tracker<RAW extends Trackable, BASE extends Trackable> {

  public Tracking<RAW, BASE> track(Input<RAW> rawInput, Input<BASE> baseInput) {
    Tracking<RAW, BASE> tracking = new Tracking<>(rawInput, baseInput);

    // 1. match issues with same rule, same line and same line hash, but not necessarily with same message
    match(tracking, LineAndLineHashKeyFactory.INSTANCE);

    // 2. detect code moves by comparing blocks of codes
    detectCodeMoves(rawInput, baseInput, tracking);

    // 3. match issues with same rule, same message and same line hash
    match(tracking, LineHashAndMessageKeyFactory.INSTANCE);

    // 4. match issues with same rule, same line and same message
    match(tracking, LineAndMessageKeyFactory.INSTANCE);

    // 5. match issues with same rule and same line hash but different line and different message.
    // See SONAR-2812
    match(tracking, LineHashKeyFactory.INSTANCE);

    return tracking;
  }

  private void detectCodeMoves(Input<RAW> rawInput, Input<BASE> baseInput, Tracking<RAW, BASE> tracking) {
    if (!tracking.isComplete()) {
      new BlockRecognizer<RAW, BASE>().match(rawInput, baseInput, tracking);
    }
  }

  private void match(Tracking<RAW, BASE> tracking, SearchKeyFactory factory) {
    if (tracking.isComplete()) {
      return;
    }

    Multimap<SearchKey, BASE> baseSearch = ArrayListMultimap.create();
    for (BASE base : tracking.getUnmatchedBases()) {
      baseSearch.put(factory.create(base), base);
    }

    for (RAW raw : tracking.getUnmatchedRaws()) {
      SearchKey rawKey = factory.create(raw);
      Collection<BASE> bases = baseSearch.get(rawKey);
      if (!bases.isEmpty()) {
        // TODO taking the first one. Could be improved if there are more than 2 issues on the same line.
        // Message could be checked to take the best one.
        BASE match = bases.iterator().next();
        tracking.match(raw, match);
        baseSearch.remove(rawKey, match);
      }
    }
  }

  private interface SearchKey {
  }

  private interface SearchKeyFactory {
    SearchKey create(Trackable trackable);
  }

  private static class LineAndLineHashKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String lineHash;
    private final Integer line;

    LineAndLineHashKey(Trackable trackable) {
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
      int result = ruleKey.hashCode();
      result = 31 * result + lineHash.hashCode();
      result = 31 * result + (line != null ? line.hashCode() : 0);
      return result;
    }
  }

  private enum LineAndLineHashKeyFactory implements SearchKeyFactory {
    INSTANCE;
    @Override
    public SearchKey create(Trackable t) {
      return new LineAndLineHashKey(t);
    }
  }

  private static class LineHashAndMessageKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String message;
    private final String lineHash;

    LineHashAndMessageKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.message = trackable.getMessage();
      this.lineHash = StringUtils.defaultString(trackable.getLineHash(), "");
    }

    @Override
    public boolean equals(@Nonnull Object o) {
      if (this == o) {
        return true;
      }
      LineHashAndMessageKey that = (LineHashAndMessageKey) o;
      // start with most discriminant field
      return lineHash.equals(that.lineHash)
        && message.equals(that.message)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      int result = ruleKey.hashCode();
      result = 31 * result + message.hashCode();
      result = 31 * result + lineHash.hashCode();
      return result;
    }
  }

  private enum LineHashAndMessageKeyFactory implements SearchKeyFactory {
    INSTANCE;
    @Override
    public SearchKey create(Trackable t) {
      return new LineHashAndMessageKey(t);
    }
  }

  private static class LineAndMessageKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String message;
    private final Integer line;

    LineAndMessageKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.message = trackable.getMessage();
      this.line = trackable.getLine();
    }

    @Override
    public boolean equals(@Nonnull Object o) {
      if (this == o) {
        return true;
      }
      LineAndMessageKey that = (LineAndMessageKey) o;
      // start with most discriminant field
      return Objects.equals(line, that.line)
        && message.equals(that.message)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      int result = ruleKey.hashCode();
      result = 31 * result + message.hashCode();
      result = 31 * result + (line != null ? line.hashCode() : 0);
      return result;
    }
  }

  private enum LineAndMessageKeyFactory implements SearchKeyFactory {
    INSTANCE;
    @Override
    public SearchKey create(Trackable t) {
      return new LineAndMessageKey(t);
    }
  }

  private static class LineHashKey implements SearchKey {
    private final RuleKey ruleKey;
    private final String lineHash;

    LineHashKey(Trackable trackable) {
      this.ruleKey = trackable.getRuleKey();
      this.lineHash = StringUtils.defaultString(trackable.getLineHash(), "");
    }

    @Override
    public boolean equals(@Nonnull Object o) {
      if (this == o) {
        return true;
      }
      LineHashKey that = (LineHashKey) o;
      // start with most discriminant field
      return lineHash.equals(that.lineHash)
        && ruleKey.equals(that.ruleKey);
    }

    @Override
    public int hashCode() {
      int result = ruleKey.hashCode();
      result = 31 * result + lineHash.hashCode();
      return result;
    }
  }

  private enum LineHashKeyFactory implements SearchKeyFactory {
    INSTANCE;
    @Override
    public SearchKey create(Trackable t) {
      return new LineHashKey(t);
    }
  }
}

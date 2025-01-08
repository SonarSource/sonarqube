/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.function.Function;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.tracking.AbstractTracker.LineAndLineHashAndMessage;
import org.sonar.core.issue.tracking.AbstractTracker.LineAndLineHashKey;
import org.sonar.core.issue.tracking.AbstractTracker.LineAndMessageKey;
import org.sonar.core.issue.tracking.AbstractTracker.LineHashAndMessageKey;
import org.sonar.core.issue.tracking.AbstractTracker.LineHashKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractTrackerTest {
  private final Trackable base = trackable(RuleKey.of("r1", "r1"), 0, "m1", "hash1");
  private final Trackable same = trackable(RuleKey.of("r1", "r1"), 0, "m1", "hash1");
  private final Trackable diffRule = trackable(RuleKey.of("r1", "r2"), 0, "m1", "hash1");
  private final Trackable diffMessage = trackable(RuleKey.of("r1", "r1"), 0, null, "hash1");
  private final Trackable diffLineHash = trackable(RuleKey.of("r1", "r1"), 0, "m1", null);
  private final Trackable diffLine = trackable(RuleKey.of("r1", "r1"), null, "m1", "hash1");

  @Test
  public void lineAndLineHashKey() {
    Comparator comparator = new Comparator(LineAndLineHashKey::new);
    comparator.sameEqualsAndHashcode(base, same);
    comparator.sameEqualsAndHashcode(base, diffMessage);
    comparator.differentEquals(base, diffRule);
    comparator.differentEquals(base, diffLineHash);
    comparator.differentEquals(base, diffLine);
  }

  @Test
  public void lineAndLineHashAndMessage() {
    Comparator comparator = new Comparator(LineAndLineHashAndMessage::new);
    comparator.sameEqualsAndHashcode(base, same);
    comparator.differentEquals(base, diffMessage);
    comparator.differentEquals(base, diffRule);
    comparator.differentEquals(base, diffLineHash);
    comparator.differentEquals(base, diffLine);
  }

  @Test
  public void lineHashAndMessageKey() {
    Comparator comparator = new Comparator(LineHashAndMessageKey::new);
    comparator.sameEqualsAndHashcode(base, same);
    comparator.sameEqualsAndHashcode(base, diffLine);
    comparator.differentEquals(base, diffMessage);
    comparator.differentEquals(base, diffRule);
    comparator.differentEquals(base, diffLineHash);
  }

  @Test
  public void lineAndMessageKey() {
    Comparator comparator = new Comparator(LineAndMessageKey::new);
    comparator.sameEqualsAndHashcode(base, same);
    comparator.sameEqualsAndHashcode(base, diffLineHash);
    comparator.differentEquals(base, diffMessage);
    comparator.differentEquals(base, diffRule);
    comparator.differentEquals(base, diffLine);
  }

  @Test
  public void lineHashKey() {
    Comparator comparator = new Comparator(LineHashKey::new);
    comparator.sameEqualsAndHashcode(base, same);
    comparator.sameEqualsAndHashcode(base, diffLine);
    comparator.sameEqualsAndHashcode(base, diffMessage);
    comparator.differentEquals(base, diffRule);
    comparator.differentEquals(base, diffLineHash);
  }

  private static Trackable trackable(RuleKey ruleKey, Integer line, String message, String lineHash) {
    Trackable trackable = mock(Trackable.class);
    when(trackable.getRuleKey()).thenReturn(ruleKey);
    when(trackable.getLine()).thenReturn(line);
    when(trackable.getMessage()).thenReturn(message);
    when(trackable.getLineHash()).thenReturn(lineHash);
    return trackable;
  }

  private static class Comparator {
    private final Function<Trackable, AbstractTracker.SearchKey> searchKeyFactory;

    private Comparator(Function<Trackable, AbstractTracker.SearchKey> searchKeyFactory) {
      this.searchKeyFactory = searchKeyFactory;
    }

    private void sameEqualsAndHashcode(Trackable t1, Trackable t2) {
      AbstractTracker.SearchKey k1 = searchKeyFactory.apply(t1);
      AbstractTracker.SearchKey k2 = searchKeyFactory.apply(t2);

      assertThat(k1).isEqualTo(k1);
      assertThat(k1).isEqualTo(k2);
      assertThat(k2).isEqualTo(k1);
      assertThat(k1).hasSameHashCodeAs(k1);
      assertThat(k1).hasSameHashCodeAs(k2);
      assertThat(k2).hasSameHashCodeAs(k1);
    }

    private void differentEquals(Trackable t1, Trackable t2) {
      AbstractTracker.SearchKey k1 = searchKeyFactory.apply(t1);
      AbstractTracker.SearchKey k2 = searchKeyFactory.apply(t2);

      assertThat(k1).isNotEqualTo(k2);
      assertThat(k2).isNotEqualTo(k1);
      assertThat(k1).isNotEqualTo(new Object());
      assertThat(k1).isNotNull();
    }
  }
}

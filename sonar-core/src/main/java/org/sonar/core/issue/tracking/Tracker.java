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

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.issue.Issue;

import static org.sonar.core.util.stream.MoreCollectors.toList;

@ScannerSide
public class Tracker<RAW extends Trackable, BASE extends Trackable> extends AbstractTracker<RAW, BASE> {

  public NonClosedTracking<RAW, BASE> trackNonClosed(Input<RAW> rawInput, Input<BASE> baseInput) {
    NonClosedTracking<RAW, BASE> tracking = NonClosedTracking.of(rawInput, baseInput);

    // 1. match by rule, line, line hash and message
    match(tracking, LineAndLineHashAndMessage::new);

    // 2. match issues with same rule, same line and same line hash, but not necessarily with same message
    match(tracking, LineAndLineHashKey::new);

    // 3. detect code moves by comparing blocks of codes
    detectCodeMoves(rawInput, baseInput, tracking);

    // 4. match issues with same rule, same message and same line hash
    match(tracking, LineHashAndMessageKey::new);

    // 5. match issues with same rule, same line and same message
    match(tracking, LineAndMessageKey::new);

    // 6. match issues with same rule and same line hash but different line and different message.
    // See SONAR-2812
    match(tracking, LineHashKey::new);

    return tracking;
  }

  public Tracking<RAW, BASE> trackClosed(NonClosedTracking<RAW, BASE> nonClosedTracking, Input<BASE> baseInput) {
    ClosedTracking<RAW, BASE> closedTracking = ClosedTracking.of(nonClosedTracking, baseInput);
    match(closedTracking, LineAndLineHashAndMessage::new);

    return new MergedTracking<>(nonClosedTracking, closedTracking);
  }

  private void detectCodeMoves(Input<RAW> rawInput, Input<BASE> baseInput, Tracking<RAW, BASE> tracking) {
    if (!tracking.isComplete()) {
      new BlockRecognizer<RAW, BASE>().match(rawInput, baseInput, tracking);
    }
  }

  private static class ClosedTracking<RAW extends Trackable, BASE extends Trackable> extends Tracking<RAW, BASE> {
    private final Input<BASE> baseInput;

    ClosedTracking(NonClosedTracking<RAW, BASE> nonClosedTracking, Input<BASE> closedBaseInput) {
      super(nonClosedTracking.getRawInput().getIssues(), closedBaseInput.getIssues(), nonClosedTracking.rawToBase, nonClosedTracking.baseToRaw);
      this.baseInput = closedBaseInput;
    }

    public static <RAW extends Trackable, BASE extends Trackable> ClosedTracking<RAW, BASE> of(NonClosedTracking<RAW, BASE> nonClosedTracking, Input<BASE> baseInput) {
      Input<BASE> closedBaseInput = new FilteringBaseInputWrapper<>(baseInput, t -> Issue.STATUS_CLOSED.equals(t.getStatus()));
      return new ClosedTracking<>(nonClosedTracking, closedBaseInput);
    }

    public Input<BASE> getBaseInput() {
      return baseInput;
    }
  }

  private static class MergedTracking<RAW extends Trackable, BASE extends Trackable> extends Tracking<RAW, BASE> {
    private MergedTracking(NonClosedTracking<RAW, BASE> nonClosedTracking, ClosedTracking<RAW, BASE> closedTracking) {
      super(
        nonClosedTracking.getRawInput().getIssues(),
        concatIssues(nonClosedTracking, closedTracking),
        closedTracking.rawToBase, closedTracking.baseToRaw);
    }

    private static <RAW extends Trackable, BASE extends Trackable> List<BASE> concatIssues(
      NonClosedTracking<RAW, BASE> nonClosedTracking, ClosedTracking<RAW, BASE> closedTracking) {
      Collection<BASE> nonClosedIssues = nonClosedTracking.getBaseInput().getIssues();
      Collection<BASE> closeIssues = closedTracking.getBaseInput().getIssues();
      return Stream.concat(nonClosedIssues.stream(), closeIssues.stream())
        .collect(toList(nonClosedIssues.size() + closeIssues.size()));
    }
  }
}

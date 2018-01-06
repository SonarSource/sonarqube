/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.filemove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.sonar.server.computation.task.projectanalysis.filemove.FileMoveDetectionStep.MIN_REQUIRED_SCORE;

abstract class MatchesByScore implements Iterable<List<Match>> {

  public static MatchesByScore create(ScoreMatrix scoreMatrix) {
    if (scoreMatrix.getMaxScore() < MIN_REQUIRED_SCORE) {
      return EmptyMatchesByScore.INSTANCE;
    }
    MatchesByScoreImpl res = new MatchesByScoreImpl(scoreMatrix);
    res.populate();
    return res;
  }

  public abstract int getSize();

  private static final class MatchesByScoreImpl extends MatchesByScore implements ScoreMatrix.ScoreMatrixVisitor {
    private final ScoreMatrix scoreMatrix;
    private final List<Match>[] matches;
    private int totalMatches = 0;

    private MatchesByScoreImpl(ScoreMatrix scoreMatrix) {
      this.scoreMatrix = scoreMatrix;
      this.matches = new List[scoreMatrix.getMaxScore() - MIN_REQUIRED_SCORE + 1];
    }

    private void populate() {
      scoreMatrix.accept(this);
    }

    @Override
    public void visit(String dbFileKey, String reportFileKey, int score) {
      if (!isAcceptableScore(score)) {
        return;
      }

      // Store higher score first so that iterator get higher score first
      int index = scoreMatrix.getMaxScore() - score;
      if (matches[index] == null) {
        matches[index] = new ArrayList<>(1);
      }
      Match match = new Match(dbFileKey, reportFileKey);
      matches[index].add(match);
      totalMatches++;
    }

    private static boolean isAcceptableScore(int score) {
      return score >= MIN_REQUIRED_SCORE;
    }

    @Override
    public int getSize() {
      return totalMatches;
    }

    @Override
    public Iterator<List<Match>> iterator() {
      return Arrays.asList(matches).iterator();
    }
  }

  private static class EmptyMatchesByScore extends MatchesByScore {
    private static final EmptyMatchesByScore INSTANCE = new EmptyMatchesByScore();

    @Override
    public int getSize() {
      return 0;
    }

    @Override
    public Iterator<List<Match>> iterator() {
      return Collections.<List<Match>>emptyList().iterator();
    }
  }
}

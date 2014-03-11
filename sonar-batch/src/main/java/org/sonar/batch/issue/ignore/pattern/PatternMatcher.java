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
package org.sonar.batch.issue.ignore.pattern;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.sonar.api.issue.Issue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class PatternMatcher {

  private Multimap<String, IssuePattern> patternByComponent = LinkedHashMultimap.create();

  public IssuePattern getMatchingPattern(Issue issue) {
    IssuePattern matchingPattern = null;
    Iterator<IssuePattern> patternIterator = getPatternsForComponent(issue.componentKey()).iterator();
    while(matchingPattern == null && patternIterator.hasNext()) {
      IssuePattern nextPattern = patternIterator.next();
      if (nextPattern.match(issue)) {
        matchingPattern = nextPattern;
      }
    }
    return matchingPattern;
  }

  public Collection<IssuePattern> getPatternsForComponent(String componentKey) {
    return patternByComponent.get(componentKey);
  }

  public void addPatternForComponent(String component, IssuePattern pattern) {
    patternByComponent.put(component, pattern.forResource(component));
  }

  public void addPatternToExcludeResource(String resource) {
    addPatternForComponent(resource, new IssuePattern(resource, "*").setCheckLines(false));
  }

  public void addPatternToExcludeLines(String resource, Set<LineRange> lineRanges) {
    addPatternForComponent(resource, new IssuePattern(resource, "*", lineRanges).setCheckLines(true));
  }

}

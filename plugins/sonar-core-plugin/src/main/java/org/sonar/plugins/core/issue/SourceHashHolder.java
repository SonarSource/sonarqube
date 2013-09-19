/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.issue;

import java.util.Collection;

import org.sonar.api.batch.SonarIndex;
import org.sonar.api.resources.Resource;
import org.sonar.batch.scan.LastSnapshots;
import org.sonar.plugins.core.issue.tracking.HashedSequence;
import org.sonar.plugins.core.issue.tracking.StringText;
import org.sonar.plugins.core.issue.tracking.StringTextComparator;



public class SourceHashHolder {

  private final SonarIndex index;
  private final LastSnapshots lastSnapshots;
  private final Resource resource;

  private String source;
  private boolean sourceInitialized;
  private String referenceSource;
  private boolean referenceSourceInitialized;

  private HashedSequence<StringText> hashedReference;
  private HashedSequence<StringText> hashedSource;

  public SourceHashHolder(SonarIndex index, LastSnapshots lastSnapshots, Resource resource) {
    this.index = index;
    this.lastSnapshots = lastSnapshots;
    this.resource = resource;
  }

  private void initHashes() {
    hashedReference = HashedSequence.wrap(new StringText(getReferenceSource()), StringTextComparator.IGNORE_WHITESPACE);
    hashedSource = HashedSequence.wrap(new StringText(getSource()), StringTextComparator.IGNORE_WHITESPACE);
  }

  public HashedSequence<StringText> getHashedReference() {
    initHashesIfNull(hashedReference);
    return hashedReference;
  }

  public HashedSequence<StringText> getHashedSource() {
    initHashesIfNull(hashedSource);
    return hashedSource;
  }

  public String getSource() {
    if (! sourceInitialized) {
      source = index.getSource(resource);
      sourceInitialized = true;
    }
    return source;
  }

  public String getReferenceSource() {
    if (! referenceSourceInitialized) {
      if (resource != null) {
        referenceSource = lastSnapshots.getSource(resource);
      }
      referenceSourceInitialized = true;
    }
    return referenceSource;
  }

  public boolean hasBothReferenceAndCurrentSource() {
    return getSource() != null && getReferenceSource() != null;
  }

  private void initHashesIfNull(Object required) {
    if(required == null) {
      initHashes();
    }
  }

  public Collection<Integer> getNewLinesMatching(Integer originLine) {
    return getHashedSource().getLinesForHash(getHashedReference().getHash(originLine));
  }
}


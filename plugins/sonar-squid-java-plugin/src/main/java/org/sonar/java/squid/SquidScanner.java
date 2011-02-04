/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.java.squid;

import java.util.Collection;
import java.util.Collections;

import org.sonar.java.squid.check.SquidCheck;
import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.CodeScanner;
import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.indexer.SquidIndex;

public class SquidScanner extends CodeScanner<CodeVisitor> {

  private SquidIndex indexer;

  public SquidScanner(SquidIndex indexer) {
    this.indexer = indexer;
  }

  public void scan() {
    Collection<SourceCode> files = indexer.search(new QueryByType(SourceFile.class));
    notifySquidVisitors(files);
  }

  private void notifySquidVisitors(Collection<SourceCode> files) {
    SquidVisitor[] visitorArray = getVisitors().toArray(new SquidVisitor[getVisitors().size()]);
    for (SourceCode sourceFile : files) {
      new SquidVisitorNotifier((SourceFile) sourceFile, visitorArray).notifyVisitors();
    }
  }

  @Override
  public Collection<Class<? extends CodeVisitor>> getVisitorClasses() {
    return Collections.emptyList();
  }

  @Override
  public void accept(CodeVisitor visitor) {
    if (visitor instanceof SquidCheck) {
      super.accept(visitor);
    }
  }

}

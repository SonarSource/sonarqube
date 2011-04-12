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

package org.sonar.java.squid.check;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

@Rule(key = "EmptyFile", name = "Empty file", priority = Priority.MAJOR,
    description = "Detect empty files, which do not have any lines of code. Example: <pre>\n//package org.foo;\n//\n//public class Bar {}\n</pre>")
public final class EmptyFileCheck extends SquidCheck {

  @Override
  public void visitFile(SourceFile file) {
    int loc = file.getInt(Metric.LINES_OF_CODE);
    if (loc==0) {
      CheckMessage message = new CheckMessage(this, "This Java file is empty");
      file.log(message);
    }
  }

}

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
package org.sonar.batch.scan2;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.issue.Issue.Severity;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.scan.filesystem.InputPathCache;

class DefaultIssueValueCoder implements ValueCoder {

  private final InputPathCache inputPathCache;

  public DefaultIssueValueCoder(InputPathCache inputPathCache) {
    this.inputPathCache = inputPathCache;
  }

  @Override
  public void put(Value value, Object object, CoderContext context) {
    DefaultIssue issue = (DefaultIssue) object;
    value.putString(issue.key());
    InputPath inputPath = issue.inputPath();
    if (inputPath != null) {
      if (inputPath instanceof InputDir) {
        value.put(0);
        value.putString(((DefaultInputDir) inputPath).moduleKey());
        value.putString(inputPath.relativePath());
      } else {
        value.put(1);
        value.putString(((DefaultInputFile) inputPath).moduleKey());
        value.putString(inputPath.relativePath());
        value.put(issue.line());
      }
    } else {
      value.putNull();
    }
    value.put(issue.message());
    value.put(issue.effortToFix());
    value.put(issue.ruleKey().repository());
    value.put(issue.ruleKey().rule());
    Severity overridenSeverity = issue.overridenSeverity();
    if (overridenSeverity == null) {
      value.putNull();
    } else {
      value.put(overridenSeverity.ordinal());
    }
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    DefaultIssue newIssue = new DefaultIssue(null);
    newIssue.withKey(value.getString());
    if (value.isNull(true)) {
      newIssue.onProject();
    } else {
      int type = value.getInt();
      String moduleKey = value.getString();
      String relativePath = value.getString();
      if (type == 0) {
        InputDir dir = inputPathCache.getDir(moduleKey, relativePath);
        newIssue.onDir(dir);
      } else {
        InputFile f = inputPathCache.getFile(moduleKey, relativePath);
        newIssue.onFile(f);
        if (!value.isNull(true)) {
          newIssue.atLine(value.getInt());
        }
      }
    }
    newIssue.message(value.getString());
    newIssue.effortToFix(value.isNull(true) ? null : value.getDouble());
    String repo = value.getString();
    String rule = value.getString();
    newIssue.ruleKey(RuleKey.of(repo, rule));
    newIssue.overrideSeverity(value.isNull(true) ? null : Severity.values()[value.getInt()]);
    return newIssue;
  }

}

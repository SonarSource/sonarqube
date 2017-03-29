/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
module.exports = function(source, line, options) {
  const currentLine = source.find(row => row.lineNumber === line);
  const prevLine = source.find(row => row.lineNumber === line - 1);
  let changed = true;
  if (currentLine && prevLine && currentLine.scm && prevLine.scm) {
    changed = currentLine.scm.author !== prevLine.scm.author ||
      currentLine.scm.date !== prevLine.scm.date ||
      !prevLine.show;
  }
  return changed ? options.fn(this) : options.inverse(this);
};

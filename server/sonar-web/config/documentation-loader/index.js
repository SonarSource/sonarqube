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
const path = require('path');
const parseDirectory = require('./parse-directory');
const fetchMatter = require('./fetch-matter');

module.exports = function(source) {
  this.cacheable();

  const failure = this.async();
  const success = failure.bind(null, null);

  const config = this.exec(source, this.resourcePath);
  const root = path.resolve(path.dirname(this.resourcePath), config.root);
  this.addContextDependency(root);

  parseDirectory(root)
    .then(files => fetchMatter(root, files))
    .then(result => `module.exports = ${JSON.stringify(result)};`)
    .then(success)
    .catch(failure);
};

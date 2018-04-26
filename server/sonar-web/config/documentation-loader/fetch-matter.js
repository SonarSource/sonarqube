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
const fs = require('fs');
const path = require('path');
const matter = require('gray-matter');

const compare = (a, b) => {
  if (a.order === b.order) return a.title.localeCompare(b.title);
  if (a.order === -1) return 1;
  if (b.order === -1) return -1;
  return a.order - b.order;
};

module.exports = (root, files) => {
  return files
    .map(file => {
      const content = fs.readFileSync(root + '/' + file, 'utf8');
      const headerData = matter(content).data;
      return {
        name: path.basename(file).slice(0, -3),
        relativeName: file.slice(0, -3),
        title: headerData.title || file,
        order: headerData.order || -1
      };
    })
    .sort(compare);
};

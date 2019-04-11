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
import slug from 'remark-slug';
import util from 'mdast-util-toc';

/**
 * This is a simplified version of the remark-toc plugin: https://github.com/remarkjs/remark-toc
 * It *only* renders the TOC, and leaves all the rest out.
 */
export default function onlyToc() {
  this.use(slug);

  return transformer;

  function transformer(node) {
    const result = util(node, { heading: 'doctoc', maxDepth: 2 });

    if (result.index === null || result.index === -1 || !result.map) {
      node.children = [];
    } else {
      node.children = [result.map];
    }
  }
}

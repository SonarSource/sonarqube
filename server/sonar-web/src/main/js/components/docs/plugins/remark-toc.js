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

const DEFAULT_HEADING = 'toc|table[ -]of[ -]contents?';

/**
 * This comes from the remark-toc plugin: https://github.com/remarkjs/remark-toc
 * We cannot use it directly before the following issue gets fixed: https://github.com/remarkjs/remark-toc/issues/18
 */
export default function toc(options) {
  const settings = options || {};
  const heading = settings.heading || DEFAULT_HEADING;
  const depth = settings.maxDepth || 6;
  const { tight } = settings;

  this.use(slug);

  return transformer;

  function transformer(node) {
    const result = util(node, { heading, maxDepth: depth, tight });

    if (result.index === null || result.index === -1 || !result.map) {
      return;
    }

    node.children = [].concat(
      node.children.slice(0, result.index),
      result.map,
      node.children.slice(result.index)
    );
  }
}

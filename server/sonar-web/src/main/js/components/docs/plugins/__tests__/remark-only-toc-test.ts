/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import util from 'mdast-util-toc';
import onlyToc from '../remark-only-toc';

jest.mock('mdast-util-toc', () => ({
  __esModule: true,
  default: jest.fn().mockReturnValue({})
}));

it('should only render toc', () => {
  const node = { type: 'test', children: ['a'] };
  onlyToc()(node);
  expect(node.children).toHaveLength(0);

  (util as jest.Mock).mockReturnValue({ index: -1 });
  node.children.push('a');

  onlyToc()(node);
  expect(node.children).toHaveLength(0);

  (util as jest.Mock).mockReturnValue({ index: 0 });
  node.children.push('a');

  onlyToc()(node);
  expect(node.children).toHaveLength(0);

  (util as jest.Mock).mockReturnValue({ index: 0, map: 'a' });
  node.children.push('a');

  onlyToc()(node);
  expect(node.children).toHaveLength(1);
});

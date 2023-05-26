/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { mockRule } from '../../../helpers/testMocks';
import { ADVANCED_RULE, S131_RULE, S6069_RULE, SIMPLE_RULE } from './ids';

export function mockRuleList() {
  return [
    mockRule({
      key: SIMPLE_RULE,
      name: 'Simple rule',
      lang: 'java',
      langName: 'Java',
      type: 'CODE_SMELL',
    }),
    mockRule({
      key: ADVANCED_RULE,
      name: 'Advanced rule',
      lang: 'web',
      langName: 'HTML',
      type: 'VULNERABILITY',
    }),
    mockRule({
      key: S6069_RULE,
      lang: 'cpp',
      langName: 'C++',
      name: 'Security hotspot rule',
      type: 'SECURITY_HOTSPOT',
    }),
    mockRule({
      key: S131_RULE,
      name: '"CASE" expressions should end with "ELSE" clauses',
      lang: 'tsql',
      langName: 'T-SQL',
    }),
  ];
}

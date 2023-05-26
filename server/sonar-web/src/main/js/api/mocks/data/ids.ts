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

// Component tree.
export const PARENT_COMPONENT_KEY = 'foo';
//// Root folder.
export const FILE1_KEY = 'index.tsx';
export const FILE2_KEY = 'test1.js';
export const FILE3_KEY = 'test2.js';
export const FILE4_KEY = 'testSymb.tsx';
export const FILE5_KEY = 'empty.js';
export const FILE6_KEY = 'huge.js';
export const FOLDER1_KEY = 'folderA';
//// Inside folderA.
export const FILE7_KEY = 'out.tsx';

// Rules.
export const SIMPLE_RULE = 'simpleRuleId';
export const ADVANCED_RULE = 'advancedRuleId';
export const S6069_RULE = 'cpp:S6069';
export const S131_RULE = 'tsql:S131';

// Issues.
export const ISSUE_0 = 'issue0';
export const ISSUE_1 = 'issue1';
export const ISSUE_2 = 'issue2';
export const ISSUE_3 = 'issue3';
export const ISSUE_4 = 'issue4';
export const ISSUE_11 = 'issue11';
export const ISSUE_101 = 'issue101';
export const ISSUE_1101 = 'issue1101';

// Issue to rule map.
export const ISSUE_TO_RULE = {
  [ISSUE_0]: SIMPLE_RULE,
  [ISSUE_1]: SIMPLE_RULE,
  [ISSUE_2]: ADVANCED_RULE,
  [ISSUE_3]: 'other',
  [ISSUE_4]: 'other',
  [ISSUE_11]: SIMPLE_RULE,
  [ISSUE_101]: SIMPLE_RULE,
  [ISSUE_1101]: SIMPLE_RULE,
};

// Issue to files map.
export const ISSUE_TO_FILES = {
  [ISSUE_0]: [FILE2_KEY],
  [ISSUE_1]: [FILE6_KEY],
  [ISSUE_2]: [FILE3_KEY],
  [ISSUE_3]: [FILE3_KEY],
  [ISSUE_4]: [FILE3_KEY],
  [ISSUE_11]: [FILE2_KEY, FILE3_KEY],
  [ISSUE_101]: [FILE2_KEY, FILE3_KEY],
  [ISSUE_1101]: [FILE7_KEY],
};

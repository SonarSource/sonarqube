/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { SecurityStandard, Standards } from '../../../types/security';

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
export const FILE8_KEY = 'in.tsx';

// Rules.
export const SIMPLE_RULE = 'simpleRuleId';
export const ADVANCED_RULE = 'advancedRuleId';
export const S6069_RULE = 'cpp:S6069';
export const S131_RULE = 'tsql:S131';
export const RULE_1 = 'rule1';
export const RULE_2 = 'rule2';
export const RULE_3 = 'rule3';
export const RULE_4 = 'rule4';
export const RULE_5 = 'rule5';
export const RULE_6 = 'rule6';
export const RULE_7 = 'rule7';
export const RULE_8 = 'rule8';
export const RULE_9 = 'rule9';
export const RULE_10 = 'rule10';
export const RULE_11 = 'rule11';
export const RULE_12 = 'rule12';

// Quality Profiles.
export const QP_1 = 'p1';
export const QP_2_Parent = 'p2parent';
export const QP_2 = 'p2';
export const QP_3 = 'p3';
export const QP_4 = 'p4';
export const QP_5 = 'p5';
export const QP_6 = 'p6';

// Issues.
export const ISSUE_0 = 'issue0';
export const ISSUE_1 = 'issue1';
export const ISSUE_2 = 'issue2';
export const ISSUE_3 = 'issue3';
export const ISSUE_4 = 'issue4';
export const ISSUE_5 = 'issue5';
export const ISSUE_11 = 'issue11';
export const ISSUE_101 = 'issue101';
export const ISSUE_1101 = 'issue1101';
export const ISSUE_1102 = 'issue1102';
export const ISSUE_1103 = 'issue1103';

// Issue to rule map.
export const ISSUE_TO_RULE = {
  [ISSUE_0]: SIMPLE_RULE,
  [ISSUE_1]: SIMPLE_RULE,
  [ISSUE_2]: ADVANCED_RULE,
  [ISSUE_3]: 'other',
  [ISSUE_4]: 'other',
  [ISSUE_5]: 'other',
  [ISSUE_11]: SIMPLE_RULE,
  [ISSUE_101]: SIMPLE_RULE,
  [ISSUE_1101]: SIMPLE_RULE,
  [ISSUE_1102]: SIMPLE_RULE,
  [ISSUE_1103]: SIMPLE_RULE,
};

// Issue to files map.
export const ISSUE_TO_FILES = {
  [ISSUE_0]: [FILE2_KEY],
  [ISSUE_1]: [FILE6_KEY],
  [ISSUE_2]: [FILE3_KEY],
  [ISSUE_3]: [FILE3_KEY],
  [ISSUE_4]: [FILE3_KEY],
  [ISSUE_5]: [FILE3_KEY],
  [ISSUE_11]: [FILE2_KEY, FILE3_KEY],
  [ISSUE_101]: [FILE2_KEY, FILE3_KEY],
  [ISSUE_1101]: [`${FOLDER1_KEY}/${FILE7_KEY}`],
  [ISSUE_1102]: [`${FOLDER1_KEY}/${FILE8_KEY}`],
  [ISSUE_1103]: [`${FOLDER1_KEY}/${FILE8_KEY}`],
};

export const STANDARDS_TO_RULES: Partial<{
  [category in keyof Standards]: { [standard: string]: string[] };
}> = {
  [SecurityStandard.SONARSOURCE]: {
    'buffer-overflow': [RULE_1, RULE_2, RULE_3, RULE_4, RULE_5, RULE_6],
  },
  [SecurityStandard.OWASP_TOP10_2021]: {
    a2: [RULE_1, RULE_2, RULE_3, RULE_4, RULE_5],
  },
  [SecurityStandard.OWASP_TOP10]: {
    a3: [RULE_1, RULE_2, RULE_3, RULE_4],
  },
  [SecurityStandard.CWE]: {
    '102': [RULE_1, RULE_2, RULE_3],
    '297': [RULE_1, RULE_4],
  },
};

// Webhooks.
export const WEBHOOK_GLOBAL_1 = 'global-webhook1';
export const WEBHOOK_GLOBAL_1_LATEST_DELIVERY_ID = 'global-delivery1';
export const WEBHOOK_GLOBAL_2 = 'global-webhook2';
export const WEBHOOK_PROJECT_1 = 'project1';
export const WEBHOOK_PROJECT_1_1 = 'project1-webhook1';
export const WEBHOOK_PROJECT_1_2 = 'project1-webhook2';

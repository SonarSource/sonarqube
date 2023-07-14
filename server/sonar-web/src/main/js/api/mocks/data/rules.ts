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

import { RuleDescriptionSections } from '../../../apps/coding-rules/rule';
import { mockRule, mockRuleActivation, mockRuleDetails } from '../../../helpers/testMocks';
import {
  ADVANCED_RULE,
  RULE_1,
  RULE_10,
  RULE_11,
  RULE_2,
  RULE_3,
  RULE_4,
  RULE_5,
  RULE_6,
  RULE_7,
  RULE_8,
  RULE_9,
  S131_RULE,
  S6069_RULE,
  SIMPLE_RULE,
} from './ids';

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

export const resourceContent = 'Some link <a href="http://example.com">Awsome Reading</a>';
export const introTitle = 'Introduction to this rule';
export const rootCauseContent = 'Root cause';
export const howToFixContent = 'This is how to fix';

export function mockRuleDetailsList() {
  return [
    mockRuleDetails({
      key: RULE_1,
      repo: 'repo1',
      type: 'BUG',
      lang: 'java',
      langName: 'Java',
      name: 'Awsome java rule',
      tags: ['awesome'],
      params: [
        { key: '1', type: 'TEXT', htmlDesc: 'html description for key 1' },
        { key: '2', type: 'NUMBER', defaultValue: 'default value for key 2' },
      ],
    }),
    mockRuleDetails({
      key: RULE_2,
      repo: 'repo1',
      name: 'Hot hotspot',
      tags: ['awesome'],
      type: 'SECURITY_HOTSPOT',
      lang: 'js',
      descriptionSections: [
        { key: RuleDescriptionSections.INTRODUCTION, content: introTitle },
        { key: RuleDescriptionSections.ROOT_CAUSE, content: rootCauseContent },
        { key: RuleDescriptionSections.HOW_TO_FIX, content: howToFixContent },
        { key: RuleDescriptionSections.ASSESS_THE_PROBLEM, content: 'Assess' },
        {
          key: RuleDescriptionSections.RESOURCES,
          content: resourceContent,
        },
      ],
      langName: 'JavaScript',
    }),
    mockRuleDetails({
      key: RULE_3,
      repo: 'repo2',
      name: 'Unknown rule',
      lang: 'js',
      langName: 'JavaScript',
    }),
    mockRuleDetails({
      key: RULE_4,
      type: 'BUG',
      lang: 'c',
      langName: 'C',
      name: 'Awsome C rule',
    }),
    mockRuleDetails({
      key: RULE_5,
      type: 'VULNERABILITY',
      lang: 'py',
      langName: 'Python',
      name: 'Awsome Python rule',
      descriptionSections: [
        { key: RuleDescriptionSections.INTRODUCTION, content: introTitle },
        { key: RuleDescriptionSections.HOW_TO_FIX, content: rootCauseContent },
        {
          key: RuleDescriptionSections.RESOURCES,
          content: resourceContent,
        },
      ],
    }),
    mockRuleDetails({
      key: RULE_6,
      type: 'BUG',
      lang: 'py',
      langName: 'Python',
      name: 'Bad Python rule',
      isExternal: true,
      descriptionSections: undefined,
    }),
    mockRuleDetails({
      key: RULE_7,
      type: 'VULNERABILITY',
      severity: 'MINOR',
      lang: 'py',
      langName: 'Python',
      name: 'Python rule with context',
      descriptionSections: [
        {
          key: RuleDescriptionSections.INTRODUCTION,
          content: 'Introduction to this rule with context',
        },
        {
          key: RuleDescriptionSections.HOW_TO_FIX,
          content: 'This is how to fix for spring',
          context: { key: 'spring', displayName: 'Spring' },
        },
        {
          key: RuleDescriptionSections.HOW_TO_FIX,
          content: 'This is how to fix for spring boot',
          context: { key: 'spring_boot', displayName: 'Spring boot' },
        },
        {
          key: RuleDescriptionSections.RESOURCES,
          content: resourceContent,
        },
      ],
    }),
    mockRuleDetails({
      key: RULE_8,
      type: 'BUG',
      severity: 'MINOR',
      lang: 'py',
      langName: 'Python',
      tags: ['awesome'],
      name: 'Template rule',
      params: [
        { key: '1', type: 'TEXT', htmlDesc: 'html description for key 1' },
        { key: '2', type: 'NUMBER', defaultValue: 'default value for key 2' },
      ],
      isTemplate: true,
    }),
    mockRuleDetails({
      key: RULE_9,
      type: 'BUG',
      severity: 'MINOR',
      lang: 'py',
      langName: 'Python',
      tags: ['awesome', 'cute'],
      name: 'Custom Rule based on rule8',
      params: [
        { key: '1', type: 'TEXT', htmlDesc: 'html description for key 1' },
        { key: '2', type: 'NUMBER', defaultValue: 'default value for key 2' },
      ],
      templateKey: 'rule8',
    }),
    mockRuleDetails({
      createdAt: '2022-12-16T17:26:54+0100',
      key: RULE_10,
      type: 'VULNERABILITY',
      severity: 'MINOR',
      lang: 'py',
      langName: 'Python',
      tags: ['awesome'],
      name: 'Awesome Python rule with education principles',
      descriptionSections: [
        { key: RuleDescriptionSections.INTRODUCTION, content: introTitle },
        { key: RuleDescriptionSections.HOW_TO_FIX, content: rootCauseContent },
        {
          key: RuleDescriptionSections.RESOURCES,
          content: resourceContent,
        },
      ],
      educationPrinciples: ['defense_in_depth', 'never_trust_user_input'],
    }),
    mockRuleDetails({
      key: RULE_11,
      type: 'BUG',
      lang: 'java',
      langName: 'Java',
      name: 'Common java rule',
    }),
  ];
}

export function mockRulesActivationsInQP() {
  return {
    [RULE_1]: [mockRuleActivation({ qProfile: 'p1' })],
  };
}

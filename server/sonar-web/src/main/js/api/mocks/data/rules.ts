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

import { RuleDescriptionSections } from '../../../apps/coding-rules/rule';
import { mockRule, mockRuleActivation, mockRuleDetails } from '../../../helpers/testMocks';
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../../types/clean-code-taxonomy';
import { RuleStatus } from '../../../types/rules';
import {
  ADVANCED_RULE,
  QP_1,
  QP_2,
  QP_2_Parent,
  QP_4,
  QP_5,
  QP_6,
  RULE_1,
  RULE_10,
  RULE_11,
  RULE_12,
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
      impacts: [
        {
          softwareQuality: SoftwareQuality.Maintainability,
          severity: SoftwareImpactSeverity.Medium,
        },
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
      impacts: [],
      cleanCodeAttributeCategory: undefined,
      cleanCodeAttribute: undefined,
      langName: 'JavaScript',
    }),
    mockRuleDetails({
      key: RULE_3,
      repo: 'repo2',
      name: 'Unknown rule',
      impacts: [
        { softwareQuality: SoftwareQuality.Maintainability, severity: SoftwareImpactSeverity.Low },
      ],
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
      cleanCodeAttributeCategory: CleanCodeAttributeCategory.Consistent,
      name: 'Awsome Python rule',
      descriptionSections: [
        { key: RuleDescriptionSections.INTRODUCTION, content: introTitle },
        { key: RuleDescriptionSections.ROOT_CAUSE, content: rootCauseContent },
        { key: RuleDescriptionSections.HOW_TO_FIX, content: howToFixContent },
        {
          key: RuleDescriptionSections.RESOURCES,
          content: resourceContent,
        },
      ],
      impacts: [
        {
          softwareQuality: SoftwareQuality.Maintainability,
          severity: SoftwareImpactSeverity.Medium,
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
      impacts: [
        {
          softwareQuality: SoftwareQuality.Maintainability,
          severity: SoftwareImpactSeverity.Medium,
        },
        {
          softwareQuality: SoftwareQuality.Security,
          severity: SoftwareImpactSeverity.Low,
        },
      ],
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
      impacts: [
        {
          softwareQuality: SoftwareQuality.Maintainability,
          severity: SoftwareImpactSeverity.Low,
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
      impacts: [
        { softwareQuality: SoftwareQuality.Reliability, severity: SoftwareImpactSeverity.Low },
      ],
      lang: 'py',
      langName: 'Python',
      tags: ['awesome', 'cute'],
      name: 'Custom Rule based on rule8',
      params: [
        { key: '1', type: 'TEXT', htmlDesc: 'html description for key 1' },
        { key: '2', type: 'NUMBER', defaultValue: 'default value for key 2' },
      ],
      templateKey: RULE_8,
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
      impacts: [
        {
          softwareQuality: SoftwareQuality.Maintainability,
          severity: SoftwareImpactSeverity.Low,
        },
        {
          softwareQuality: SoftwareQuality.Reliability,
          severity: SoftwareImpactSeverity.High,
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
    mockRuleDetails({
      key: RULE_12,
      type: 'BUG',
      severity: 'MINOR',
      lang: 'py',
      langName: 'Python',
      tags: ['awesome'],
      name: 'Deleted custom rule based on rule8',
      templateKey: RULE_8,
      status: RuleStatus.Removed,
    }),
  ];
}

export function mockRulesActivationsInQP() {
  return {
    [RULE_1]: [mockRuleActivation({ qProfile: QP_1 }), mockRuleActivation({ qProfile: QP_6 })],
    [RULE_7]: [
      mockRuleActivation({
        qProfile: QP_2,
        impacts: [
          {
            softwareQuality: SoftwareQuality.Maintainability,
            severity: SoftwareImpactSeverity.Medium,
          },
          { softwareQuality: SoftwareQuality.Reliability, severity: SoftwareImpactSeverity.High },
          { softwareQuality: SoftwareQuality.Security, severity: SoftwareImpactSeverity.High },
        ],
      }),
    ],
    [RULE_8]: [mockRuleActivation({ qProfile: QP_2 })],
    [RULE_9]: [
      mockRuleActivation({
        qProfile: QP_2,
        params: [
          { key: '1', value: '' },
          { key: '2', value: 'default value for key 2' },
        ],
        inherit: 'INHERITED',
        impacts: [
          { softwareQuality: SoftwareQuality.Reliability, severity: SoftwareImpactSeverity.Medium },
        ],
      }),
    ],
    [RULE_10]: [
      mockRuleActivation({
        qProfile: QP_2,
        inherit: 'OVERRIDES',
        impacts: [
          {
            softwareQuality: SoftwareQuality.Maintainability,
            severity: SoftwareImpactSeverity.Medium,
          },
          {
            softwareQuality: SoftwareQuality.Reliability,
            severity: SoftwareImpactSeverity.Info,
          },
        ],
        prioritizedRule: true,
      }),
      mockRuleActivation({
        qProfile: QP_2_Parent,
        severity: 'MINOR',
        impacts: [
          {
            softwareQuality: SoftwareQuality.Maintainability,
            severity: SoftwareImpactSeverity.Low,
          },
          {
            softwareQuality: SoftwareQuality.Reliability,
            severity: SoftwareImpactSeverity.Blocker,
          },
        ],
      }),
    ],
    [RULE_11]: [
      mockRuleActivation({ qProfile: QP_4 }),
      mockRuleActivation({ qProfile: QP_5, inherit: 'INHERITED' }),
    ],
  };
}

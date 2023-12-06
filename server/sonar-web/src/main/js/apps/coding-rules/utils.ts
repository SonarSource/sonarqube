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

import { omit } from 'lodash';
import { RestRuleDetails, RuleDetails } from '../../types/types';

export const REST_RULE_KEYS_TO_OLD_KEYS = {
  repositoryKey: 'repo',
  external: 'isExternal',
  markdownDescription: 'mdDesc',
  markdownNote: 'mdNote',
  template: 'isTemplate',
  systemTags: 'sysTags',
  language: 'lang',
  languageName: 'langName',
};

// Mapping new resource to old api. We should get rid of it with migration of rules/search & rule/details
export function mapRestRuleToRule(rule: RestRuleDetails): RuleDetails {
  return {
    ...omit(rule, Object.keys(REST_RULE_KEYS_TO_OLD_KEYS)),
    ...Object.entries(REST_RULE_KEYS_TO_OLD_KEYS).reduce(
      (obj, [key, value]: [keyof RestRuleDetails, keyof RuleDetails]) => {
        obj[value] = rule[key] as never;
        return obj;
      },
      {} as RuleDetails,
    ),
    params: rule.parameters?.map((param) => ({
      ...omit(param, 'htmlDescription'),
      htmlDesc: param.htmlDescription,
    })),
  };
}

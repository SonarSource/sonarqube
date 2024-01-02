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
import { ReferencedRule } from '../../types/issues';
import { IssueChangelog } from '../../types/types';

export function mockReferencedRule(overrides: Partial<ReferencedRule> = {}): ReferencedRule {
  return {
    langName: 'Javascript',
    name: 'RuleFoo',
    ...overrides,
  };
}

export function mockIssueChangelog(overrides: Partial<IssueChangelog> = {}): IssueChangelog {
  return {
    creationDate: '2018-10-01',
    isUserActive: true,
    user: 'luke.skywalker',
    userName: 'Luke Skywalker',
    diffs: [
      {
        key: 'assign',
        newValue: 'darth.vader',
        oldValue: 'luke.skywalker',
      },
    ],
    ...overrides,
  };
}

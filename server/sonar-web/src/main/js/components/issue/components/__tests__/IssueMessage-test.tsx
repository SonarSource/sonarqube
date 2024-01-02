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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockIssue } from '../../../../helpers/testMocks';
import { RuleStatus } from '../../../../types/rules';
import IssueMessage, { IssueMessageProps } from '../IssueMessage';

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useContext: jest
      .fn()
      .mockImplementation(() => ({ externalRulesRepoNames: {}, openRule: jest.fn() })),
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ issue: mockIssue(false, { externalRuleEngine: 'js' }) })).toMatchSnapshot(
    'with engine info'
  );
  expect(shallowRender({ issue: mockIssue(false, { quickFixAvailable: true }) })).toMatchSnapshot(
    'with quick fix'
  );
  expect(
    shallowRender({ issue: mockIssue(false, { ruleStatus: RuleStatus.Deprecated }) })
  ).toMatchSnapshot('is deprecated rule');
  expect(
    shallowRender({ issue: mockIssue(false, { ruleStatus: RuleStatus.Removed }) })
  ).toMatchSnapshot('is removed rule');
  expect(shallowRender({ displayWhyIsThisAnIssue: false })).toMatchSnapshot(
    'hide why is it an issue'
  );
});

function shallowRender(props: Partial<IssueMessageProps> = {}) {
  return shallow<IssueMessageProps>(
    <IssueMessage
      issue={mockIssue(false, {
        message: 'Reduce the number of conditional operators (4) used in the expression',
      })}
      displayWhyIsThisAnIssue={true}
      branchLike={mockBranch()}
      {...props}
    />
  );
}

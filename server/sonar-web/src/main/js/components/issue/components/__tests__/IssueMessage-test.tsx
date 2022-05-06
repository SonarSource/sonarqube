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
import { shallow } from 'enzyme';
import * as React from 'react';
import { RuleStatus } from '../../../../types/rules';
import { ButtonLink } from '../../../controls/buttons';
import IssueMessage, { IssueMessageProps } from '../IssueMessage';

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useContext: jest
      .fn()
      .mockImplementation(() => ({ externalRulesRepoNames: {}, openRule: jest.fn() }))
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ engine: 'js' })).toMatchSnapshot('with engine info');
  expect(shallowRender({ quickFixAvailable: true })).toMatchSnapshot('with quick fix');
  expect(shallowRender({ manualVulnerability: true })).toMatchSnapshot('is manual vulnerability');
  expect(shallowRender({ ruleStatus: RuleStatus.Deprecated })).toMatchSnapshot(
    'is deprecated rule'
  );
  expect(shallowRender({ ruleStatus: RuleStatus.Removed })).toMatchSnapshot('is removed rule');
  expect(shallowRender({ displayWhyIsThisAnIssue: false })).toMatchSnapshot(
    'hide why is it an issue'
  );
});

it('should open why is this an issue workspace', () => {
  const openRule = jest.fn();
  (React.useContext as jest.Mock).mockImplementationOnce(() => ({
    externalRulesRepoNames: {},
    openRule
  }));
  const wrapper = shallowRender();
  wrapper.find(ButtonLink).simulate('click');

  expect(openRule).toBeCalled();
});

function shallowRender(props: Partial<IssueMessageProps> = {}) {
  return shallow<IssueMessageProps>(
    <IssueMessage
      manualVulnerability={false}
      message="Reduce the number of conditional operators (4) used in the expression"
      displayWhyIsThisAnIssue={true}
      ruleKey="javascript:S1067"
      {...props}
    />
  );
}

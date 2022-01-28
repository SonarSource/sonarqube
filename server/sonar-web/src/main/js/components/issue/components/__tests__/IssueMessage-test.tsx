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
import { ButtonLink } from '../../../../components/controls/buttons';
import { click } from '../../../../helpers/testUtils';
import { RuleStatus } from '../../../../types/rules';
import IssueMessage, { IssueMessageProps } from '../IssueMessage';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ engine: 'js' })).toMatchSnapshot('with engine info');
  expect(shallowRender({ quickFixAvailable: true })).toMatchSnapshot('with quick fix');
  expect(shallowRender({ engineName: 'JS' })).toMatchSnapshot('with engine name');
  expect(shallowRender({ manualVulnerability: true })).toMatchSnapshot('is manual vulnerability');
  expect(shallowRender({ ruleStatus: RuleStatus.Deprecated })).toMatchSnapshot(
    'is deprecated rule'
  );
  expect(shallowRender({ ruleStatus: RuleStatus.Removed })).toMatchSnapshot('is removed rule');
});

it('should handle click correctly', () => {
  const onOpenRule = jest.fn();
  const wrapper = shallowRender({ onOpenRule });
  click(wrapper.find(ButtonLink));
  expect(onOpenRule).toBeCalledWith({
    key: 'javascript:S1067'
  });
});

function shallowRender(props: Partial<IssueMessageProps> = {}) {
  return shallow<IssueMessageProps>(
    <IssueMessage
      manualVulnerability={false}
      message="Reduce the number of conditional operators (4) used in the expression"
      onOpenRule={jest.fn()}
      ruleKey="javascript:S1067"
      {...props}
    />
  );
}

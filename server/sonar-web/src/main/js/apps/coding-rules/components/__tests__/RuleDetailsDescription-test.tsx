/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { change, click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import RuleDetailsDescription from '../RuleDetailsDescription';

jest.mock('../../../../api/rules', () => ({
  updateRule: jest.fn().mockResolvedValue('updatedrule')
}));

const RULE: T.RuleDetails = {
  key: 'squid:S1133',
  repo: 'squid',
  name: 'Deprecated code should be removed',
  createdAt: '2013-07-26T09:40:51+0200',
  htmlDesc: '<p>Html Description</p>',
  mdNote: 'Md Note',
  severity: 'INFO',
  status: 'READY',
  lang: 'java',
  langName: 'Java',
  type: 'CODE_SMELL'
};

const EXTERNAL_RULE: T.RuleDetails = {
  createdAt: '2013-07-26T09:40:51+0200',
  key: 'external_xoo:OneExternalIssuePerLine',
  repo: 'external_xoo',
  name: 'xoo:OneExternalIssuePerLine',
  severity: 'MAJOR',
  status: 'READY',
  isExternal: true,
  type: 'UNKNOWN'
};

const EXTERNAL_RULE_WITH_DATA: T.RuleDetails = {
  key: 'external_xoo:OneExternalIssueWithDetailsPerLine',
  repo: 'external_xoo',
  name: 'One external issue per line',
  createdAt: '2018-05-31T11:19:51+0200',
  htmlDesc: '<p>Html Description</p>',
  severity: 'MAJOR',
  status: 'READY',
  isExternal: true,
  type: 'BUG'
};

it('should display correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
  expect(getWrapper({ ruleDetails: EXTERNAL_RULE })).toMatchSnapshot();
  expect(getWrapper({ ruleDetails: EXTERNAL_RULE_WITH_DATA })).toMatchSnapshot();
});

it('should add extra description', async () => {
  const onChange = jest.fn();
  const wrapper = getWrapper({ canWrite: true, onChange });
  click(wrapper.find('#coding-rules-detail-extend-description'));
  expect(wrapper.find('textarea').exists()).toBeTruthy();
  change(wrapper.find('textarea'), 'new description');
  click(wrapper.find('#coding-rules-detail-extend-description-submit'));
  await waitAndUpdate(wrapper);
  expect(onChange).toBeCalledWith('updatedrule');
});

function getWrapper(props = {}) {
  return shallow(
    <RuleDetailsDescription
      canWrite={false}
      onChange={jest.fn()}
      organization={undefined}
      ruleDetails={RULE}
      {...props}
    />
  );
}

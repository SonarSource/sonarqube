/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import RuleDetailsMeta from '../RuleDetailsMeta';
import { RuleScope, RuleType } from '../../../../app/types';
import RuleDetailsTagsPopup from '../RuleDetailsTagsPopup';

const RULE = {
  key: 'squid:S1133',
  repo: 'squid',
  name: 'Deprecated code should be removed',
  createdAt: '2013-07-26T09:40:51+0200',
  severity: 'INFO',
  status: 'READY',
  lang: 'java',
  langName: 'Java',
  scope: RuleScope.Main,
  type: RuleType.CodeSmell
};

const EXTERNAL_RULE = {
  key: 'external_xoo:OneExternalIssuePerLine',
  repo: 'external_xoo',
  name: 'xoo:OneExternalIssuePerLine',
  createdAt: '2018-05-31T11:22:13+0200',
  status: 'READY',
  scope: RuleScope.All,
  isExternal: true,
  type: RuleType.Unknown
};

const EXTERNAL_RULE_WITH_DATA = {
  key: 'external_xoo:OneExternalIssueWithDetailsPerLine',
  repo: 'external_xoo',
  name: 'One external issue per line',
  createdAt: '2018-05-31T11:19:51+0200',
  severity: 'MAJOR',
  status: 'READY',
  tags: ['tag'],
  lang: 'xoo',
  langName: 'Xoo',
  scope: RuleScope.All,
  isExternal: true,
  type: RuleType.Bug
};

it('should display right meta info', () => {
  expect(getWrapper()).toMatchSnapshot();
  expect(
    getWrapper({ hideSimilarRulesFilter: true, ruleDetails: EXTERNAL_RULE })
  ).toMatchSnapshot();
  expect(
    getWrapper({ hideSimilarRulesFilter: true, ruleDetails: EXTERNAL_RULE_WITH_DATA })
  ).toMatchSnapshot();
});

it('should edit tags', () => {
  const onTagsChange = jest.fn();
  const wrapper = getWrapper({ onTagsChange });
  expect(wrapper.find('[data-meta="tags"]')).toMatchSnapshot();
  const overlay = wrapper
    .find('[data-meta="tags"]')
    .find('Dropdown')
    .prop('overlay') as RuleDetailsTagsPopup;

  overlay.props.setTags(['foo', 'bar']);
  expect(onTagsChange).toBeCalledWith(['foo', 'bar']);
});

function getWrapper(props = {}) {
  return shallow(
    <RuleDetailsMeta
      canWrite={true}
      onFilterChange={jest.fn()}
      onTagsChange={jest.fn()}
      organization={undefined}
      referencedRepositories={{}}
      ruleDetails={RULE}
      {...props}
    />
  );
}

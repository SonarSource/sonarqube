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
import { RuleDetails } from '../../../../app/types';
import RuleDetailsTagsPopup from '../RuleDetailsTagsPopup';

const ruleDetails: RuleDetails = {
  createdAt: '',
  repo: '',
  key: 'key',
  lang: '',
  langName: '',
  name: '',
  severity: '',
  status: '',
  type: ''
};

it('should edit tags', () => {
  const onTagsChange = jest.fn();
  const wrapper = shallow(
    <RuleDetailsMeta
      canWrite={true}
      onFilterChange={jest.fn()}
      onTagsChange={onTagsChange}
      organization={undefined}
      referencedRepositories={{}}
      ruleDetails={ruleDetails}
    />
  );
  expect(wrapper.find('[data-meta="tags"]')).toMatchSnapshot();
  const overlay = wrapper
    .find('[data-meta="tags"]')
    .find('Dropdown')
    .prop('overlay') as RuleDetailsTagsPopup;

  overlay.props.setTags(['foo', 'bar']);
  expect(onTagsChange).toBeCalledWith(['foo', 'bar']);
});

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
import { SelectComponentsProps } from 'react-select/src/Select';
import Select from '../../../../components/controls/Select';
import SearchableFilterFooter from '../SearchableFilterFooter';

const options = [
  { label: 'java', value: 'java' },
  { label: 'js', value: 'js' },
  { label: 'csharp', value: 'csharp' },
];

it('should render items without the ones in the facet', () => {
  const wrapper = shallow(
    <SearchableFilterFooter
      onQueryChange={jest.fn()}
      options={options}
      property="languages"
      query={{ languages: ['java'] }}
    />
  );
  expect(wrapper.find<SelectComponentsProps>(Select).props().options).toMatchSnapshot();
});

it('should properly handle a change of the facet value', () => {
  const onQueryChange = jest.fn();
  const wrapper = shallow(
    <SearchableFilterFooter
      onQueryChange={onQueryChange}
      options={options}
      property="languages"
      query={{ languages: ['java'] }}
    />
  );
  wrapper.find(Select).simulate('change', { value: 'js' });
  expect(onQueryChange).toHaveBeenCalledWith({ languages: 'java,js' });
});

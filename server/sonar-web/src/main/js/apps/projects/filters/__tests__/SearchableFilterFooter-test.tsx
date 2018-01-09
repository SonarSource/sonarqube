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
import SearchableFilterFooter from '../SearchableFilterFooter';

const options = [
  { label: 'java', value: 'java' },
  { label: 'js', value: 'js' },
  { label: 'csharp', value: 'csharp' }
];

it('should render items without the ones in the facet', () => {
  const wrapper = shallow(
    <SearchableFilterFooter
      onQueryChange={jest.fn()}
      property="languages"
      query={{ languages: ['java'] }}
      options={options}
    />,
    { context: { router: { push: jest.fn() } } }
  );
  expect(wrapper.find('Select').prop('options')).toMatchSnapshot();
});

it('should render items without the ones in the facet', () => {
  const onQueryChange = jest.fn();
  const wrapper = shallow(
    <SearchableFilterFooter
      onQueryChange={onQueryChange}
      property="languages"
      query={{ languages: ['java'] }}
      options={options}
    />,
    { context: { router: { push: jest.fn() } } }
  );
  (wrapper.find('Select').prop('onChange') as Function)({ value: 'js' });
  expect(onQueryChange).toBeCalledWith({ languages: 'java,js' });
});

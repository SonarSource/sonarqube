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
import * as React from 'react';
import { shallow } from 'enzyme';
import SearchShowMore from '../SearchShowMore';
import { click } from '../../../../helpers/testUtils';

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should trigger showing more', () => {
  const onMoreClick = jest.fn();
  const wrapper = shallowRender({ onMoreClick });
  click(wrapper.find('a'), {
    currentTarget: {
      blur() {},
      dataset: { qualifier: 'TRK' },
      preventDefault() {},
      stopPropagation() {}
    }
  });
  expect(onMoreClick).toBeCalledWith('TRK');
});

it('should select on mouse over', () => {
  const onSelect = jest.fn();
  const wrapper = shallowRender({ onSelect });
  wrapper.find('a').simulate('mouseenter', { currentTarget: { dataset: { qualifier: 'TRK' } } });
  expect(onSelect).toBeCalledWith('qualifier###TRK');
});

function shallowRender(props: Partial<SearchShowMore['props']> = {}) {
  return shallow(
    <SearchShowMore
      allowMore={true}
      onMoreClick={jest.fn()}
      onSelect={jest.fn()}
      qualifier="TRK"
      selected={false}
      {...props}
    />
  );
}

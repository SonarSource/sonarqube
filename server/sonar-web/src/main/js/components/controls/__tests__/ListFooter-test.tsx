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
import ListFooter from '../ListFooter';
import { click } from '../../../helpers/testUtils';

it('should render "3 of 5 shown"', () => {
  const listFooter = shallow(<ListFooter count={3} total={5} />);
  expect(listFooter.text()).toContain('x_of_y_shown.3.5');
});

it('should not render "show more"', () => {
  const listFooter = shallow(<ListFooter count={3} total={5} />);
  expect(listFooter.find('a').length).toBe(0);
});

it('should not render "show more"', () => {
  const listFooter = shallow(<ListFooter count={5} loadMore={jest.fn()} total={5} />);
  expect(listFooter.find('a').length).toBe(0);
});

it('should "show more"', () => {
  const loadMore = jest.fn();
  const listFooter = shallow(<ListFooter count={3} loadMore={loadMore} total={5} />);
  const link = listFooter.find('a');
  expect(link.length).toBe(1);
  click(link);
  expect(loadMore).toBeCalled();
});

it('should display spinner while loading', () => {
  expect(
    shallow(<ListFooter count={3} loadMore={jest.fn()} loading={true} total={10} />)
      .find('DeferredSpinner')
      .exists()
  ).toBe(true);
});

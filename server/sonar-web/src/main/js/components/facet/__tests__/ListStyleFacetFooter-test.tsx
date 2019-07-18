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
import { click } from 'sonar-ui-common/helpers/testUtils';
import ListStyleFacetFooter from '../ListStyleFacetFooter';

it('should not render "show more"', () => {
  expect(
    shallow(<ListStyleFacetFooter count={3} showLess={undefined} showMore={jest.fn()} total={3} />)
  ).toMatchSnapshot();
});

it('should show more', () => {
  const showMore = jest.fn();
  const wrapper = shallow(
    <ListStyleFacetFooter count={3} showLess={undefined} showMore={showMore} total={15} />
  );
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('a'));
  expect(showMore).toBeCalled();
});

it('should show less', () => {
  const showLess = jest.fn();
  const wrapper = shallow(
    <ListStyleFacetFooter count={15} showLess={showLess} showMore={jest.fn()} total={15} />
  );
  expect(wrapper).toMatchSnapshot();
  click(wrapper.find('a'));
  expect(showLess).toBeCalled();
});

it('should not render "show less"', () => {
  const wrapper = shallow(
    <ListStyleFacetFooter count={15} showLess={undefined} showMore={jest.fn()} total={15} />
  );
  expect(wrapper).toMatchSnapshot();
});

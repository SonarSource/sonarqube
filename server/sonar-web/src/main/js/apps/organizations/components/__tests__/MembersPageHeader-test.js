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
import React from 'react';
import { shallow } from 'enzyme';
import MembersPageHeader from '../MembersPageHeader';

it('should render the members page header', () => {
  const wrapper = shallow(<MembersPageHeader />);
  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ loading: true });
  expect(wrapper.find('.spinner')).toMatchSnapshot();
});

it('should render the members page header with the total', () => {
  const wrapper = shallow(<MembersPageHeader total="5" />);
  expect(wrapper).toMatchSnapshot();
});

it('should render its children', () => {
  const wrapper = shallow(
    <MembersPageHeader loading={true} total="5">
      <span>children test</span>
    </MembersPageHeader>
  );
  expect(wrapper).toMatchSnapshot();
});

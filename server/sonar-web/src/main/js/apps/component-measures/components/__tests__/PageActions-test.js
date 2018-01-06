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
import PageActions from '../PageActions';

it('should display correctly for a project', () => {
  expect(
    shallow(<PageActions loading={true} isFile={false} view="list" totalLoadedComponents={20} />)
  ).toMatchSnapshot();
});

it('should display correctly for a file', () => {
  const wrapper = shallow(
    <PageActions loading={false} isFile={true} view="tree" totalLoadedComponents={10} />
  );
  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ paging: { total: 100 } });
  expect(wrapper).toMatchSnapshot();
});

it('should not display shortcuts for treemap', () => {
  expect(
    shallow(<PageActions loading={true} isFile={false} view="treemap" totalLoadedComponents={20} />)
  ).toMatchSnapshot();
});

it('should display the total of files', () => {
  expect(
    shallow(
      <PageActions
        current={12}
        loading={true}
        isFile={false}
        view="treemap"
        totalLoadedComponents={20}
        paging={{ total: 120 }}
      />
    )
  ).toMatchSnapshot();
  expect(
    shallow(
      <PageActions
        current={12}
        loading={false}
        isFile={true}
        view="list"
        totalLoadedComponents={20}
        paging={{ total: 120 }}
      />
    )
  ).toMatchSnapshot();
});

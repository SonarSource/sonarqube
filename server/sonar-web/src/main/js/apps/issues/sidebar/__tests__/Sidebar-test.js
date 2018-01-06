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
import Sidebar from '../Sidebar';

jest.mock('../../../../store/rootReducer', () => ({}));

const renderSidebar = props =>
  shallow(<Sidebar facets={{}} myIssues={false} openFacets={{}} query={{}} {...props} />)
    .children()
    .map(node => node.name());

it('should render facets for global page', () => {
  expect(renderSidebar()).toMatchSnapshot();
});

it('should render facets for project', () => {
  expect(renderSidebar({ component: { qualifier: 'TRK' } })).toMatchSnapshot();
});

it('should render facets for module', () => {
  expect(renderSidebar({ component: { qualifier: 'BRC' } })).toMatchSnapshot();
});

it('should render facets for directory', () => {
  expect(renderSidebar({ component: { qualifier: 'DIR' } })).toMatchSnapshot();
});

it('should render facets for developer', () => {
  expect(renderSidebar({ component: { qualifier: 'DEV' } })).toMatchSnapshot();
});

it('should render facets when my issues are selected', () => {
  expect(renderSidebar({ myIssues: true })).toMatchSnapshot();
});

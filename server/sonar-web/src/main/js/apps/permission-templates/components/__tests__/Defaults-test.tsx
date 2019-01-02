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
import Defaults from '../Defaults';

const SAMPLE: T.PermissionTemplate = {
  createdAt: '2018-01-01',
  defaultFor: [],
  id: 'id',
  name: 'name',
  permissions: []
};

it('should render one qualifier', () => {
  const sample = { ...SAMPLE, defaultFor: ['DEV'] };
  const output = shallow(<Defaults organization={undefined} template={sample} />);
  expect(output).toMatchSnapshot();
});

it('should render several qualifiers', () => {
  const sample = { ...SAMPLE, defaultFor: ['TRK', 'VW'] };
  const output = shallow(<Defaults organization={undefined} template={sample} />);
  expect(output).toMatchSnapshot();
});

it('should render several qualifiers for default organization', () => {
  const sample = { ...SAMPLE, defaultFor: ['TRK', 'VW'] };
  const organization: T.Organization = { isDefault: true, key: 'org', name: 'org' };
  const output = shallow(<Defaults organization={organization} template={sample} />);
  expect(output).toMatchSnapshot();
});

it('should render only projects for custom organization', () => {
  const sample = { ...SAMPLE, defaultFor: ['TRK', 'VW'] };
  const organization: T.Organization = { isDefault: false, key: 'org', name: 'org' };
  const output = shallow(<Defaults organization={organization} template={sample} />);
  expect(output).toMatchSnapshot();
});

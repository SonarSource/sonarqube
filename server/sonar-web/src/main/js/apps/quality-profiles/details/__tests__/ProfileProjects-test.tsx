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
import ProfileProjects from '../ProfileProjects';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/quality-profiles', () => ({
  getProfileProjects: jest.fn().mockResolvedValue({
    results: [
      {
        id: '633a5180-1ad7-4008-a5cb-e1d3cec4c816',
        key: 'org.sonarsource.xml:xml',
        name: 'SonarXML',
        selected: true
      }
    ],
    paging: { pageIndex: 1, pageSize: 2, total: 10 },
    more: true
  })
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<ProfileProjects['props']> = {}) {
  return shallow(
    <ProfileProjects
      organization="foo"
      profile={mockQualityProfile({ actions: { associateProjects: true } })}
      {...props}
    />
  );
}

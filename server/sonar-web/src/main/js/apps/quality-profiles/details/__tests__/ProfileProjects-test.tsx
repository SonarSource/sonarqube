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
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import ChangeProjectsForm from '../ChangeProjectsForm';
import ProfileProjects from '../ProfileProjects';

jest.mock('../../../../api/quality-profiles', () => ({
  getProfileProjects: jest.fn().mockResolvedValue({
    results: [
      {
        id: '633a5180-1ad7-4008-a5cb-e1d3cec4c816',
        key: 'org.sonarsource.xml:xml',
        name: 'SonarXML',
        selected: true,
      },
    ],
    paging: { pageIndex: 1, pageSize: 2, total: 10 },
    more: true,
  }),
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('default');
  wrapper.setProps({
    profile: mockQualityProfile({ actions: { associateProjects: false } }),
  });
  expect(wrapper).toMatchSnapshot('no rights');
  wrapper.setProps({
    profile: mockQualityProfile({
      projectCount: 0,
      activeRuleCount: 0,
      actions: { associateProjects: true },
    }),
  });
  expect(wrapper).toMatchSnapshot('no active rules, but associated projects');
  wrapper.setProps({
    profile: mockQualityProfile({ activeRuleCount: 0, actions: { associateProjects: true } }),
  });
  wrapper.setState({ projects: [] });
  expect(wrapper).toMatchSnapshot('no active rules, no associated projects');
});

it('should open and close the form', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleChangeClick();
  expect(wrapper.find(ChangeProjectsForm).exists()).toBe(true);

  wrapper.instance().closeForm();
  expect(wrapper.find(ChangeProjectsForm).exists()).toBe(false);
});

function shallowRender(props: Partial<ProfileProjects['props']> = {}) {
  return shallow<ProfileProjects>(
    <ProfileProjects
      profile={mockQualityProfile({ actions: { associateProjects: true } })}
      {...props}
    />
  );
}

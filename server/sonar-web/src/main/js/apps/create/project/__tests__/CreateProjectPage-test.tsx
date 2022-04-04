/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { getAlmSettings } from '../../../../api/alm-settings';
import { mockLocation, mockLoggedInUser, mockRouter } from '../../../../helpers/testMocks';
import { AlmKeys } from '../../../../types/alm-settings';
import { CreateProjectPage } from '../CreateProjectPage';
import { CreateProjectModes } from '../types';

jest.mock('../../../../api/alm-settings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([{ alm: AlmKeys.BitbucketServer, key: 'foo' }])
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(getAlmSettings).toBeCalled();
});

it('should render correctly if the manual method is selected', () => {
  expect(
    shallowRender({
      location: mockLocation({ query: { mode: CreateProjectModes.Manual } })
    })
  ).toMatchSnapshot();
});

it('should render correctly if the Azure method is selected', () => {
  expect(
    shallowRender({
      location: mockLocation({ query: { mode: CreateProjectModes.AzureDevOps } })
    })
  ).toMatchSnapshot();
});

it('should render correctly if the BBS method is selected', () => {
  expect(
    shallowRender({
      location: mockLocation({ query: { mode: CreateProjectModes.BitbucketServer } })
    })
  ).toMatchSnapshot();
});

it('should render correctly if the GitHub method is selected', () => {
  const wrapper = shallowRender({
    location: mockLocation({ query: { mode: CreateProjectModes.GitHub } })
  });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly if the GitLab method is selected', () => {
  const wrapper = shallowRender({
    location: mockLocation({ query: { mode: CreateProjectModes.GitLab } })
  });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<CreateProjectPage['props']> = {}) {
  return shallow<CreateProjectPage>(
    <CreateProjectPage
      appState={{}}
      currentUser={mockLoggedInUser()}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}

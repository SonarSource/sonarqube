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
import { getAlmSettings } from '../../../../api/alm-settings';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import { ProjectCreationMenu } from '../ProjectCreationMenu';

jest.mock('../../../../api/alm-settings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([]),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({ currentUser: mockLoggedInUser({ permissions: { global: [] } }) })
  ).toMatchSnapshot('not allowed');
});

it('should fetch alm bindings on mount', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getAlmSettings).toHaveBeenCalled();
});

it('should not fetch alm bindings if user cannot create projects', async () => {
  const wrapper = shallowRender({ currentUser: mockLoggedInUser({ permissions: { global: [] } }) });
  await waitAndUpdate(wrapper);
  expect(getAlmSettings).not.toHaveBeenCalled();
});

it('should filter alm bindings appropriately', async () => {
  (getAlmSettings as jest.Mock)
    .mockResolvedValueOnce([
      // Only faulty configs.
      { alm: AlmKeys.Azure }, // Missing some configuration; will be ignored.
      { alm: AlmKeys.GitLab }, // Missing some configuration; will be ignored.
    ])
    .mockResolvedValueOnce([
      // All correct configs.
      { alm: AlmKeys.Azure, url: 'http://ado.example.com' },
      { alm: AlmKeys.BitbucketServer, url: 'b1' },
      { alm: AlmKeys.GitHub },
      { alm: AlmKeys.GitLab, url: 'gitlab.com' },
    ])
    .mockResolvedValueOnce([
      // All correct configs.
      { alm: AlmKeys.Azure, url: 'http://ado.example.com' },
      { alm: AlmKeys.BitbucketCloud },
      { alm: AlmKeys.GitHub },
      { alm: AlmKeys.GitLab, url: 'gitlab.com' },
    ])
    .mockResolvedValueOnce([
      // Special case for BBS with BBC
      { alm: AlmKeys.Azure, url: 'http://ado.example.com' },
      { alm: AlmKeys.BitbucketServer, url: 'b1' },
      { alm: AlmKeys.BitbucketCloud },
      { alm: AlmKeys.GitHub },
      { alm: AlmKeys.GitLab, url: 'gitlab.com' },
    ])
    .mockResolvedValueOnce([
      // Only duplicate ALMs; should all be ignored.
      { alm: AlmKeys.Azure, url: 'http://ado.example.com' },
      { alm: AlmKeys.Azure, url: 'http://ado.example.com' },
      { alm: AlmKeys.BitbucketServer, url: 'b1' },
      { alm: AlmKeys.BitbucketServer, url: 'b1' },
      { alm: AlmKeys.GitHub },
      { alm: AlmKeys.GitHub },
      { alm: AlmKeys.GitLab, url: 'gitlab.com' },
      { alm: AlmKeys.GitLab, url: 'gitlab.com' },
    ]);

  let wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().boundAlms).toEqual([]);

  wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().boundAlms).toEqual([
    AlmKeys.Azure,
    AlmKeys.BitbucketServer,
    AlmKeys.GitHub,
    AlmKeys.GitLab,
  ]);

  wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().boundAlms).toEqual([
    AlmKeys.Azure,
    AlmKeys.BitbucketCloud,
    AlmKeys.GitHub,
    AlmKeys.GitLab,
  ]);

  wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().boundAlms).toEqual([
    AlmKeys.Azure,
    AlmKeys.BitbucketServer,
    AlmKeys.BitbucketCloud,
    AlmKeys.GitHub,
    AlmKeys.GitLab,
  ]);

  wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().boundAlms).toEqual([
    AlmKeys.Azure,
    AlmKeys.BitbucketServer,
    AlmKeys.GitHub,
    AlmKeys.GitLab,
  ]);
});

function shallowRender(overrides: Partial<ProjectCreationMenu['props']> = {}) {
  return shallow<ProjectCreationMenu>(
    <ProjectCreationMenu
      currentUser={mockLoggedInUser({ permissions: { global: ['provisioning'] } })}
      {...overrides}
    />
  );
}

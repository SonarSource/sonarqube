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
import {
  associateProject,
  dissociateProject,
  getProfileProjects,
  ProfileProject,
  searchQualityProfiles,
} from '../../../api/quality-profiles';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockComponent } from '../../../helpers/mocks/component';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { ProjectQualityProfilesApp } from '../ProjectQualityProfilesApp';

jest.mock('../../../api/quality-profiles', () => {
  const { mockQualityProfile } = jest.requireActual('../../../helpers/testMocks');

  return {
    associateProject: jest.fn().mockResolvedValue({}),
    dissociateProject: jest.fn().mockResolvedValue({}),
    searchQualityProfiles: jest.fn().mockResolvedValue({
      profiles: [
        mockQualityProfile({ key: 'css', language: 'css' }),
        mockQualityProfile({ key: 'css2', language: 'css' }),
        mockQualityProfile({ key: 'css_default', language: 'css', isDefault: true }),
        mockQualityProfile({ key: 'java', language: 'java' }),
        mockQualityProfile({ key: 'java_default', language: 'java', isDefault: true }),
        mockQualityProfile({ key: 'js', language: 'js' }),
        mockQualityProfile({ key: 'js_default', language: 'js', isDefault: true }),
        mockQualityProfile({ key: 'ts_default', language: 'ts', isDefault: true }),
        mockQualityProfile({ key: 'html', language: 'html' }),
        mockQualityProfile({ key: 'html_default', language: 'html', isDefault: true }),
      ],
    }),
    getProfileProjects: jest.fn(({ key }) => {
      const results: ProfileProject[] = [];
      if (key === 'js' || key === 'css' || key === 'html_default') {
        results.push({
          key: 'foo',
          name: 'Foo',
          selected: true,
        });
      } else if (key === 'html') {
        results.push({
          key: 'foobar',
          name: 'FooBar',
          selected: true,
        });
      }
      return Promise.resolve({ results });
    }),
  };
});

jest.mock('../../../helpers/globalMessages', () => ({
  addGlobalSuccessMessage: jest.fn(),
}));

jest.mock('../../../app/utils/handleRequiredAuthorization', () => jest.fn());

beforeEach(jest.clearAllMocks);

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('correctly checks permissions', () => {
  const wrapper = shallowRender({
    component: mockComponent({ configuration: { showQualityProfiles: false } }),
  });
  expect(wrapper.type()).toBeNull();
  expect(handleRequiredAuthorization).toHaveBeenCalled();
});

it('correctly fetches and treats profile data', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(searchQualityProfiles).toHaveBeenCalled();
  expect(getProfileProjects).toHaveBeenCalledTimes(10);

  expect(wrapper.state().projectProfiles).toEqual([
    expect.objectContaining({
      profile: expect.objectContaining({ key: 'css' }),
      selected: true,
    }),
    expect.objectContaining({
      profile: expect.objectContaining({ key: 'js' }),
      selected: true,
    }),
    expect.objectContaining({
      profile: expect.objectContaining({ key: 'html_default' }),
      selected: true,
    }),
    expect.objectContaining({
      profile: expect.objectContaining({ key: 'ts_default' }),
      selected: false,
    }),
  ]);
});

it('correctly sets a profile', async () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  // Dissociate a selected profile.
  instance.handleSetProfile(undefined, 'css');
  expect(dissociateProject).toHaveBeenLastCalledWith(
    expect.objectContaining({ key: 'css' }),
    'foo'
  );
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectProfiles).toEqual(
    expect.arrayContaining([
      {
        profile: expect.objectContaining({ key: 'css_default' }),
        // It's not explicitly selected, as we're inheriting the default.
        selected: false,
      },
    ])
  );

  // Associate a new profile.
  instance.handleSetProfile('css2', 'css_default');
  expect(associateProject).toHaveBeenLastCalledWith(
    expect.objectContaining({ key: 'css2' }),
    'foo'
  );
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectProfiles).toEqual(
    expect.arrayContaining([
      {
        profile: expect.objectContaining({ key: 'css2' }),
        // It's explicitly selected.
        selected: true,
      },
    ])
  );

  // Dissociate a default profile that was inherited.
  (dissociateProject as jest.Mock).mockClear();
  instance.handleSetProfile(undefined, 'ts_default');
  // It won't call the WS.
  expect(dissociateProject).not.toHaveBeenCalled();

  // Associate a default profile that was already inherited.
  instance.handleSetProfile('ts_default', 'ts_default');
  expect(associateProject).toHaveBeenLastCalledWith(
    expect.objectContaining({ key: 'ts_default' }),
    'foo'
  );
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectProfiles).toEqual(
    expect.arrayContaining([
      {
        profile: expect.objectContaining({ key: 'ts_default' }),
        // It's explicitly selected, even though it is the default profile.
        selected: true,
      },
    ])
  );
});

it('correctly adds a new language', async () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();
  await waitAndUpdate(wrapper);

  instance.handleAddLanguage('java');
  expect(associateProject).toHaveBeenLastCalledWith(
    expect.objectContaining({ key: 'java' }),
    'foo'
  );
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectProfiles).toEqual(
    expect.arrayContaining([
      {
        profile: expect.objectContaining({ key: 'java' }),
        // It must be explicitly selected. Adding an unanalyzed language can
        // only happen by explicitly choosing a profile.
        selected: true,
      },
    ])
  );
});

it('correctly handles WS errors', async () => {
  (searchQualityProfiles as jest.Mock).mockRejectedValueOnce(null);
  (getProfileProjects as jest.Mock).mockRejectedValueOnce(null);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().allProfiles).toHaveLength(0);
  expect(wrapper.state().projectProfiles).toHaveLength(0);
  expect(wrapper.state().loading).toBe(false);
});

function shallowRender(props: Partial<ProjectQualityProfilesApp['props']> = {}) {
  return shallow<ProjectQualityProfilesApp>(
    <ProjectQualityProfilesApp
      component={mockComponent({
        key: 'foo',
        configuration: { showQualityProfiles: true },
        qualityProfiles: [
          { key: 'css2', name: 'CSS 2', language: 'css' },
          { key: 'js', name: 'JS', language: 'js' },
          { key: 'ts_default', name: 'TS (default)', language: 'ts' },
          { key: 'html', name: 'HTML', language: 'html' },
        ],
      })}
      {...props}
    />
  );
}

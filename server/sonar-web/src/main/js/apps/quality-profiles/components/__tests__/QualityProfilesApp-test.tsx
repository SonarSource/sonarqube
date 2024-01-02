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
import { Actions, getExporters, searchQualityProfiles } from '../../../../api/quality-profiles';
import {
  mockLanguage,
  mockQualityProfile,
  mockQualityProfileExporter,
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { QualityProfilesApp } from '../QualityProfilesApp';

jest.mock('../../../../api/quality-profiles', () => ({
  getExporters: jest.fn().mockResolvedValue([]),
  searchQualityProfiles: jest.fn().mockResolvedValue({ profiles: [] }),
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');

  expect(getExporters).toHaveBeenCalled();
  expect(searchQualityProfiles).toHaveBeenCalled();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('full');
});

it('should render child with additional props', () => {
  const language = mockLanguage();
  const wrapper = shallowRender({ languages: { [language.key]: language } });

  const actions: Actions = { create: true };
  const profiles = [mockQualityProfile()];
  const exporters = [mockQualityProfileExporter()];

  wrapper.setState({ loading: false, actions, profiles, exporters });

  expect(wrapper.childAt(2).props()).toEqual({
    context: {
      actions,
      profiles,
      languages: [language],
      exporters,
      updateProfiles: wrapper.instance().updateProfiles,
    },
  });
});

it('should handle update', async () => {
  const profile1 = mockQualityProfile({ key: 'qp1', name: 'An amazing profile' });
  const profile2 = mockQualityProfile({ key: 'qp2', name: 'Quality Profile' });

  // Mock one call for the initial load, one for the update
  (searchQualityProfiles as jest.Mock)
    .mockResolvedValueOnce({ profiles: [] })
    .mockResolvedValueOnce({ profiles: [profile2, profile1] });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(wrapper.state().profiles).toHaveLength(0);

  wrapper.instance().updateProfiles();

  await waitAndUpdate(wrapper);
  expect(wrapper.state().profiles).toEqual([profile1, profile2]);
});

function shallowRender(props: Partial<QualityProfilesApp['props']> = {}) {
  return shallow<QualityProfilesApp>(
    <QualityProfilesApp languages={{}} {...props}>
      <div />
    </QualityProfilesApp>
  );
}

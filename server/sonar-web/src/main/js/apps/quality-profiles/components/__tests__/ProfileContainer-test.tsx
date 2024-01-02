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
import { render, screen } from '@testing-library/react';
import * as React from 'react';
import { HelmetProvider } from 'react-helmet-async';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { mockQualityProfile } from '../../../../helpers/testMocks';
import {
  QualityProfilesContextProps,
  withQualityProfilesContext,
} from '../../qualityProfilesContext';
import { Profile } from '../../types';
import { ProfileContainer } from '../ProfileContainer';

it('should render the header and child', () => {
  const targetProfile = mockQualityProfile({ name: 'profile1' });
  renderProfileContainer('/?language=js&name=profile1', {
    profiles: [mockQualityProfile({ language: 'Java', name: 'profile1' }), targetProfile],
  });

  expect(screen.getByText('profile1')).toBeInTheDocument();
});

it('should render "not found"', () => {
  renderProfileContainer('/?language=java&name=profile2', {
    profiles: [mockQualityProfile({ name: 'profile1' }), mockQualityProfile({ name: 'profile2' })],
  });

  expect(screen.getByText('quality_profiles.not_found')).toBeInTheDocument();
});

it('should render "not found" for wrong key', () => {
  renderProfileContainer('/?key=wrongKey', {
    profiles: [mockQualityProfile({ key: 'profileKey' })],
  });

  expect(screen.getByText('quality_profiles.not_found')).toBeInTheDocument();
});

it('should handle getting profile by key', () => {
  renderProfileContainer('/?key=profileKey', {
    profiles: [mockQualityProfile({ key: 'profileKey', name: 'found the profile' })],
  });

  expect(screen.getByText('found the profile')).toBeInTheDocument();
});

function Child(props: { profile?: Profile }) {
  return <div>{JSON.stringify(props.profile)}</div>;
}

const WrappedChild = withQualityProfilesContext(Child);

function renderProfileContainer(path: string, overrides: Partial<QualityProfilesContextProps>) {
  return render(
    <HelmetProvider context={{}}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route
            element={
              <ProfileContainer
                actions={{}}
                exporters={[]}
                languages={[]}
                profiles={[]}
                updateProfiles={jest.fn()}
                {...overrides}
              />
            }
          >
            <Route path="*" element={<WrappedChild />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </HelmetProvider>
  );
}

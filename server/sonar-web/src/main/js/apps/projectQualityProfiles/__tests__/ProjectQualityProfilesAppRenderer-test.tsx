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
import { mockComponent } from '../../../helpers/mocks/component';
import { mockQualityProfile } from '../../../helpers/testMocks';
import ProjectQualityProfilesAppRenderer, {
  ProjectQualityProfilesAppRendererProps,
} from '../ProjectQualityProfilesAppRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(
    shallowRender({
      showProjectProfileInModal: {
        profile: mockQualityProfile({ key: 'foo', language: 'js' }),
        selected: false,
      },
    })
  ).toMatchSnapshot('open profile');
  expect(shallowRender({ showAddLanguageModal: true })).toMatchSnapshot('add language');
});

function shallowRender(props: Partial<ProjectQualityProfilesAppRendererProps> = {}) {
  return shallow<ProjectQualityProfilesAppRendererProps>(
    <ProjectQualityProfilesAppRenderer
      allProfiles={[
        mockQualityProfile({ key: 'foo', language: 'js' }),
        mockQualityProfile({ key: 'bar', language: 'css' }),
        mockQualityProfile({ key: 'baz', language: 'html' }),
      ]}
      component={mockComponent()}
      loading={false}
      onAddLanguage={jest.fn()}
      onCloseModal={jest.fn()}
      onOpenAddLanguageModal={jest.fn()}
      onOpenSetProfileModal={jest.fn()}
      onSetProfile={jest.fn()}
      projectProfiles={[
        {
          profile: mockQualityProfile({
            key: 'foo',
            name: 'Foo',
            isDefault: true,
            language: 'js',
            languageName: 'JS',
          }),
          selected: false,
        },
        {
          profile: mockQualityProfile({
            key: 'bar',
            name: 'Bar',
            isDefault: true,
            language: 'css',
            languageName: 'CSS',
          }),
          selected: false,
        },
        {
          profile: mockQualityProfile({
            key: 'baz',
            name: 'Baz',
            language: 'html',
            languageName: 'HTML',
          }),
          selected: true,
        },
      ]}
      {...props}
    />
  );
}

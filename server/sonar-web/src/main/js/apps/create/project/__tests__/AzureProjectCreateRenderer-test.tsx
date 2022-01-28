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
import { mockAzureProject, mockAzureRepository } from '../../../../helpers/mocks/alm-integrations';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { AlmKeys } from '../../../../types/alm-settings';
import AzureProjectCreateRenderer, {
  AzureProjectCreateRendererProps
} from '../AzureProjectCreateRenderer';

it('should render correctly', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ settings: undefined })).toMatchSnapshot('no settings');
  expect(shallowRender({ showPersonalAccessTokenForm: true })).toMatchSnapshot('token form');
  expect(shallowRender()).toMatchSnapshot('project list');
  expect(
    shallowRender({
      settings: mockAlmSettingsInstance({ alm: AlmKeys.Azure }),
      showPersonalAccessTokenForm: true
    })
  ).toMatchSnapshot('setting missing url, admin');
  expect(
    shallowRender({
      canAdmin: false,
      settings: mockAlmSettingsInstance({ alm: AlmKeys.Azure })
    })
  ).toMatchSnapshot('setting missing url, not admin');
});

function shallowRender(overrides: Partial<AzureProjectCreateRendererProps> = {}) {
  const project = mockAzureProject();

  return shallow(
    <AzureProjectCreateRenderer
      canAdmin={true}
      importing={false}
      loading={false}
      loadingRepositories={{}}
      onImportRepository={jest.fn()}
      onOpenProject={jest.fn()}
      onPersonalAccessTokenCreate={jest.fn()}
      onSearch={jest.fn()}
      onSelectRepository={jest.fn()}
      projects={[project]}
      repositories={{ [project.name]: [mockAzureRepository()] }}
      tokenValidationFailed={false}
      settings={mockAlmSettingsInstance({ alm: AlmKeys.Azure, url: 'https://azure.company.com' })}
      showPersonalAccessTokenForm={false}
      submittingToken={false}
      {...overrides}
    />
  );
}

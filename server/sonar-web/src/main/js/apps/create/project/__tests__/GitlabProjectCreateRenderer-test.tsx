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
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { AlmKeys } from '../../../../types/alm-settings';
import GitlabProjectCreateRenderer, {
  GitlabProjectCreateRendererProps
} from '../GitlabProjectCreateRenderer';

it('should render correctly', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ settings: undefined })).toMatchSnapshot('invalid settings');
  expect(shallowRender({ canAdmin: true, settings: undefined })).toMatchSnapshot(
    'invalid settings, admin user'
  );
  expect(shallowRender()).toMatchSnapshot('pat form');
  expect(shallowRender({ showPersonalAccessTokenForm: false })).toMatchSnapshot(
    'project selection form'
  );
});

function shallowRender(props: Partial<GitlabProjectCreateRendererProps> = {}) {
  return shallow<GitlabProjectCreateRendererProps>(
    <GitlabProjectCreateRenderer
      canAdmin={false}
      loading={false}
      loadingMore={false}
      onImport={jest.fn()}
      onLoadMore={jest.fn()}
      onPersonalAccessTokenCreated={jest.fn()}
      onSearch={jest.fn()}
      projects={undefined}
      projectsPaging={{ pageIndex: 1, pageSize: 30, total: 0 }}
      searching={false}
      searchQuery=""
      resetPat={false}
      showPersonalAccessTokenForm={true}
      settings={mockAlmSettingsInstance({ alm: AlmKeys.GitLab })}
      {...props}
    />
  );
}

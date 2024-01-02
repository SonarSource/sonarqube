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
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingConfigurationErrors,
  ProjectAlmBindingConfigurationErrorScope,
} from '../../../../../types/alm-settings';
import PRDecorationBindingRenderer, {
  PRDecorationBindingRendererProps,
} from '../PRDecorationBindingRenderer';

const urls = ['http://github.enterprise.com', 'http://bbs.enterprise.com'];
const instances: AlmSettingsInstance[] = [
  {
    alm: AlmKeys.GitHub,
    key: 'i1',
    url: urls[0],
  },
  {
    alm: AlmKeys.GitHub,
    key: 'i2',
    url: urls[0],
  },
  {
    alm: AlmKeys.BitbucketServer,
    key: 'i3',
    url: urls[1],
  },
  {
    alm: AlmKeys.Azure,
    key: 'i4',
  },
];
const configurationErrors: ProjectAlmBindingConfigurationErrors = {
  scope: ProjectAlmBindingConfigurationErrorScope.Global,
  errors: [{ msg: 'Test' }, { msg: 'tesT' }],
};

it.each([
  ['when loading', { loading: true }],
  ['with no ALM instances (admin user)', { isSysAdmin: true }],
  ['with no ALM instances (non-admin user)', {}],
  ['with a single ALM instance', { instances: [instances[0]] }],
  ['with an empty form', { instances }],
  [
    'with a valid and saved form',
    {
      formData: {
        key: 'i1',
        repository: 'account/repo',
        monorepo: false,
      },
      isChanged: false,
      isConfigured: true,
      instances,
    },
  ],
  [
    'when there are configuration errors (non-admin user)',
    { instances, isConfigured: true, configurationErrors },
  ],
  [
    'when there are configuration errors (admin user)',
    {
      formData: {
        key: 'i1',
        repository: 'account/repo',
        monorepo: false,
      },
      instances,
      isConfigured: true,
      configurationErrors,
      isSysAdmin: true,
    },
  ],
  [
    'when there are configuration errors (admin user) and error are at PROJECT level',
    {
      instances,
      isConfigured: true,
      configurationErrors: {
        ...configurationErrors,
        scope: ProjectAlmBindingConfigurationErrorScope.Project,
      },
      isSysAdmin: true,
    },
  ],
])('should render correctly', (name: string, props: PRDecorationBindingRendererProps) => {
  expect(shallowRender(props)).toMatchSnapshot(name);
});

it.each([
  ['updating', { updating: true }],
  ['update is successfull', { successfullyUpdated: true }],
  ['form is valid', { isValid: true }],
  ['configuration is saved', { isConfigured: true }],
  ['configuration check is in progress', { isConfigured: true, checkingConfiguration: true }],
])(
  'should display action section correctly when',
  (name: string, props: PRDecorationBindingRendererProps) => {
    expect(shallowRender({ ...props, instances }).find('.action-section')).toMatchSnapshot(name);
  }
);

function shallowRender(props: Partial<PRDecorationBindingRendererProps> = {}) {
  return shallow(
    <PRDecorationBindingRenderer
      formData={{
        key: '',
        repository: '',
        monorepo: false,
      }}
      instances={[]}
      isChanged={false}
      isConfigured={false}
      isValid={false}
      loading={false}
      onFieldChange={jest.fn()}
      onReset={jest.fn()}
      onSubmit={jest.fn()}
      updating={false}
      successfullyUpdated={false}
      checkingConfiguration={false}
      onCheckConfiguration={jest.fn()}
      isSysAdmin={false}
      {...props}
    />
  );
}

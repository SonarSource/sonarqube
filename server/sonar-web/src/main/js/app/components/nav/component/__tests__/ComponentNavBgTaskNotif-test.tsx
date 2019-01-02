/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
/* eslint-disable import/first */
jest.mock('../../../../../helpers/l10n', () => {
  const l10n = require.requireActual('../../../../../helpers/l10n');
  l10n.hasMessage = jest.fn(() => true);
  return l10n;
});

import * as React from 'react';
import { shallow } from 'enzyme';
import ComponentNavBgTaskNotif from '../ComponentNavBgTaskNotif';

const component = {
  analysisDate: '2017-01-02T00:00:00.000Z',
  breadcrumbs: [],
  key: 'foo',
  name: 'Foo',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

it('renders background task error correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('renders background task error correctly for a different branch/PR', () => {
  expect(
    getWrapper({
      currentTask: { branch: 'my/branch', status: 'FAILED' } as T.Task,
      currentTaskOnSameBranch: false
    })
  ).toMatchSnapshot();
  expect(
    getWrapper({
      currentTask: {
        pullRequest: '650',
        pullRequestTitle: 'feature/my_pr',
        status: 'FAILED'
      } as T.Task,
      currentTaskOnSameBranch: false
    })
  ).toMatchSnapshot();
});

it('renders background task pending info correctly', () => {
  expect(getWrapper({ isPending: true })).toMatchSnapshot();
});

it('renders background task pending info correctly for admin', () => {
  expect(
    getWrapper({
      component: { ...component, configuration: { showBackgroundTasks: true } },
      isPending: true
    })
  ).toMatchSnapshot();
});

it('renders background task in progress info correctly', () => {
  expect(getWrapper({ isInProgress: true, isPending: true })).toMatchSnapshot();
});

it('renders background task license info correctly', () => {
  expect(
    getWrapper({ currentTask: { status: 'FAILED', errorType: 'LICENSING', errorMessage: 'Foo' } })
  ).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <ComponentNavBgTaskNotif
      component={component}
      currentTask={{ status: 'FAILED' } as T.Task}
      {...props}
    />
  );
}

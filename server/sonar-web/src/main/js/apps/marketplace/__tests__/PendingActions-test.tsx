/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
/* eslint-disable import/order */
import * as React from 'react';
import { shallow } from 'enzyme';
import { click } from '../../../helpers/testUtils';
import PendingActions from '../PendingActions';

jest.mock('../../../api/plugins', () => ({
  cancelPendingPlugins: jest.fn(() => Promise.resolve())
}));

const cancelPendingPlugins = require('../../../api/plugins').cancelPendingPlugins as jest.Mock<any>;

beforeEach(() => {
  cancelPendingPlugins.mockClear();
});

it('should display pending actions', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should not display anything', () => {
  expect(getWrapper({ pending: { installing: [], updating: [], removing: [] } }).type()).toBeNull();
});

it('should open the restart form', () => {
  const wrapper = getWrapper();
  click(wrapper.find('.js-restart'));
  expect(wrapper.find('RestartForm').exists()).toBeTruthy();
});

it('should cancel all pending and refresh them', async () => {
  const refreshPending = jest.fn();
  const wrapper = getWrapper({ refreshPending });
  click(wrapper.find('.js-cancel-all'));
  expect(cancelPendingPlugins).toHaveBeenCalled();
  await new Promise(setImmediate);

  expect(refreshPending).toHaveBeenCalled();
});

function getWrapper(props = {}) {
  return shallow(
    <PendingActions
      pending={{
        installing: [
          {
            key: 'foo',
            name: 'Foo',
            description: 'foo description',
            version: 'fooversion',
            implementationBuild: 'foobuild'
          },
          {
            key: 'bar',
            name: 'Bar',
            description: 'bar description',
            version: 'barversion',
            implementationBuild: 'barbuild'
          }
        ],
        updating: [],
        removing: [
          {
            key: 'baz',
            name: 'Baz',
            description: 'baz description',
            version: 'bazversion',
            implementationBuild: 'bazbuild'
          }
        ]
      }}
      refreshPending={() => {}}
      {...props}
    />
  );
}

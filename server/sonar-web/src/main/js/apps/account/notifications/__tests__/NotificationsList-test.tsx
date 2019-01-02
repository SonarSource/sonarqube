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
jest.mock('../../../../helpers/l10n', () => {
  const l10n = require.requireActual('../../../../helpers/l10n');
  l10n.hasMessage = jest.fn();
  return l10n;
});

import * as React from 'react';
import { shallow } from 'enzyme';
import NotificationsList from '../NotificationsList';
import Checkbox from '../../../../components/controls/Checkbox';
import { hasMessage } from '../../../../helpers/l10n';

const channels = ['channel1', 'channel2'];
const types = ['type1', 'type2'];
const notifications = [
  { channel: 'channel1', type: 'type1' },
  { channel: 'channel1', type: 'type2' },
  { channel: 'channel2', type: 'type2' }
];
const checkboxId = (t: string, c: string) => `checkbox-io-${t}-${c}`;

beforeEach(() => {
  (hasMessage as jest.Mock<any>).mockImplementation(() => false).mockClear();
});

it('should match snapshot', () => {
  expect(
    shallow(
      <NotificationsList
        channels={channels}
        checkboxId={checkboxId}
        notifications={notifications}
        onAdd={jest.fn()}
        onRemove={jest.fn()}
        types={types}
      />
    )
  ).toMatchSnapshot();
});

it('renders project-specific labels', () => {
  (hasMessage as jest.Mock<any>).mockImplementation(() => true);
  expect(
    shallow(
      <NotificationsList
        channels={channels}
        checkboxId={checkboxId}
        notifications={notifications}
        onAdd={jest.fn()}
        onRemove={jest.fn()}
        project={true}
        types={types}
      />
    )
  ).toMatchSnapshot();
  expect(hasMessage).toBeCalledWith('notification.dispatcher', 'type1', 'project');
  expect(hasMessage).toBeCalledWith('notification.dispatcher', 'type2', 'project');
});

it('should call `onAdd` and `onRemove`', () => {
  const onAdd = jest.fn();
  const onRemove = jest.fn();
  const wrapper = shallow(
    <NotificationsList
      channels={channels}
      checkboxId={checkboxId}
      notifications={notifications}
      onAdd={onAdd}
      onRemove={onRemove}
      types={types}
    />
  );
  const checkbox = wrapper.find(Checkbox).first();

  checkbox.prop('onCheck')(true);
  expect(onAdd).toHaveBeenCalledWith({ channel: 'channel1', type: 'type1' });

  jest.resetAllMocks();

  checkbox.prop('onCheck')(false);
  expect(onRemove).toHaveBeenCalledWith({ channel: 'channel1', type: 'type1' });
});

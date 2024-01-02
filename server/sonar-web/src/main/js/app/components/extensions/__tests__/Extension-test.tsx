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
import { mount } from 'enzyme';
import * as React from 'react';
import { IntlShape } from 'react-intl';
import { getExtensionStart } from '../../../../helpers/extensions';
import {
  mockAppState,
  mockCurrentUser,
  mockLocation,
  mockRouter,
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { Extension } from '../Extension';

jest.mock('../../../../helpers/extensions', () => ({
  getExtensionStart: jest.fn().mockResolvedValue({}),
}));

jest.mock('react-helmet-async', () => ({
  Helmet: () => null,
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render React extensions correctly', async () => {
  const start = jest.fn().mockReturnValue(<div className="extension" />);
  (getExtensionStart as jest.Mock).mockResolvedValue(start);

  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(getExtensionStart).toHaveBeenCalledWith('foo');
  await waitAndUpdate(wrapper);
  expect(start).toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
});

it('should handle Function extensions correctly', async () => {
  const stop = jest.fn();
  const start = jest.fn(() => {
    return stop;
  });
  (getExtensionStart as jest.Mock).mockResolvedValue(start);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(start).toHaveBeenCalled();

  wrapper.setProps({ extension: { key: 'bar', name: 'Bar' } });
  await waitAndUpdate(wrapper);
  expect(stop).toHaveBeenCalled();
});

it('should unmount an extension before starting a new one', async () => {
  const reactExtension = jest.fn().mockReturnValue(<div className="extension" />);
  (getExtensionStart as jest.Mock).mockResolvedValue(reactExtension);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('extensionElement')).not.toBeUndefined();

  const start = jest.fn();
  (getExtensionStart as jest.Mock).mockResolvedValue(start);

  wrapper.setProps({ extension: { key: 'bar', name: 'Bar' } });
  await waitAndUpdate(wrapper);
  expect(wrapper.state('extensionElement')).toBeUndefined();
  expect(start).toHaveBeenCalled();
});

function shallowRender(props: Partial<Extension['props']> = {}) {
  // We need to mount, as we rely on refs.
  return mount<Extension>(
    <Extension
      appState={mockAppState()}
      currentUser={mockCurrentUser()}
      extension={{ key: 'foo', name: 'Foo' }}
      intl={{} as IntlShape}
      location={mockLocation()}
      router={mockRouter()}
      updateCurrentUserHomepage={jest.fn()}
      {...props}
    />
  );
}

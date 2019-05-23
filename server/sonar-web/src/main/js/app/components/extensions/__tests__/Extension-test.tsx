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
import * as React from 'react';
import { shallow } from 'enzyme';
import { Extension } from '../Extension';
import { mockCurrentUser, mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { getExtensionStart } from '../../../../helpers/extensions';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../helpers/extensions', () => ({
  getExtensionStart: jest.fn().mockResolvedValue(jest.fn())
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render extension correctly', async () => {
  const start = jest.fn().mockReturnValue(<div className="extension" />);
  (getExtensionStart as jest.Mock).mockResolvedValue(start);

  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(getExtensionStart).toBeCalledWith('foo');
  await waitAndUpdate(wrapper);
  expect(start).toBeCalled();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<Extension['props']> = {}) {
  return shallow(
    <Extension
      currentUser={mockCurrentUser()}
      extension={{ key: 'foo', name: 'Foo' }}
      intl={{} as any}
      location={mockLocation()}
      onFail={jest.fn()}
      router={mockRouter()}
      {...props}
    />
  );
}

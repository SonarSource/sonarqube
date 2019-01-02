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
import { ComponentNavLicenseNotif } from '../ComponentNavLicenseNotif';
import { isValidLicense } from '../../../../../api/marketplace';
import { waitAndUpdate } from '../../../../../helpers/testUtils';

jest.mock('../../../../../helpers/l10n', () => {
  const l10n = require.requireActual('../../../../../helpers/l10n');
  l10n.hasMessage = jest.fn(() => true);
  return l10n;
});

jest.mock('../../../../../api/marketplace', () => ({
  isValidLicense: jest.fn().mockResolvedValue({ isValidLicense: false })
}));

beforeEach(() => {
  (isValidLicense as jest.Mock<any>).mockClear();
});

it('renders background task license info correctly', async () => {
  let wrapper = getWrapper({
    currentTask: { status: 'FAILED', errorType: 'LICENSING', errorMessage: 'Foo' } as T.Task
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper = getWrapper({
    appState: { canAdmin: false },
    currentTask: { status: 'FAILED', errorType: 'LICENSING', errorMessage: 'Foo' } as T.Task
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('renders a different message if the license is valid', async () => {
  (isValidLicense as jest.Mock<any>).mockResolvedValueOnce({ isValidLicense: true });
  const wrapper = getWrapper({
    currentTask: { status: 'FAILED', errorType: 'LICENSING', errorMessage: 'Foo' } as T.Task
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('renders correctly for LICENSING_LOC error', async () => {
  (isValidLicense as jest.Mock<any>).mockResolvedValueOnce({ isValidLicense: true });
  const wrapper = getWrapper({
    currentTask: { status: 'FAILED', errorType: 'LICENSING_LOC', errorMessage: 'Foo' } as T.Task
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function getWrapper(props: Partial<ComponentNavLicenseNotif['props']> = {}) {
  return shallow(
    <ComponentNavLicenseNotif
      appState={{ canAdmin: true }}
      currentTask={{ errorMessage: 'Foo', errorType: 'LICENSING' } as T.Task}
      {...props}
    />
  );
}

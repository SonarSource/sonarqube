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
import { isValidLicense } from '../../../../../api/editions';
import { mockTask } from '../../../../../helpers/mocks/tasks';
import { mockAppState } from '../../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../../helpers/testUtils';
import { TaskStatuses } from '../../../../../types/tasks';
import { ComponentNavLicenseNotif } from '../ComponentNavLicenseNotif';

jest.mock('../../../../../helpers/l10n', () => ({
  ...jest.requireActual('../../../../../helpers/l10n'),
  hasMessage: jest.fn().mockReturnValue(true),
}));

jest.mock('../../../../../api/editions', () => ({
  isValidLicense: jest.fn().mockResolvedValue({ isValidLicense: false }),
}));

beforeEach(() => {
  (isValidLicense as jest.Mock<any>).mockClear();
});

it('renders background task license info correctly', async () => {
  let wrapper = getWrapper({
    currentTask: mockTask({
      status: TaskStatuses.Failed,
      errorType: 'LICENSING',
      errorMessage: 'Foo',
    }),
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper = getWrapper({
    appState: mockAppState({ canAdmin: false }),
    currentTask: mockTask({
      status: TaskStatuses.Failed,
      errorType: 'LICENSING',
      errorMessage: 'Foo',
    }),
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('renders a different message if the license is valid', async () => {
  (isValidLicense as jest.Mock<any>).mockResolvedValueOnce({ isValidLicense: true });
  const wrapper = getWrapper({
    currentTask: mockTask({
      status: TaskStatuses.Failed,
      errorType: 'LICENSING',
      errorMessage: 'Foo',
    }),
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('renders correctly for LICENSING_LOC error', async () => {
  (isValidLicense as jest.Mock<any>).mockResolvedValueOnce({ isValidLicense: true });
  const wrapper = getWrapper({
    currentTask: mockTask({
      status: TaskStatuses.Failed,
      errorType: 'LICENSING_LOC',
      errorMessage: 'Foo',
    }),
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function getWrapper(props: Partial<ComponentNavLicenseNotif['props']> = {}) {
  return shallow(
    <ComponentNavLicenseNotif
      appState={mockAppState({ canAdmin: true })}
      currentTask={mockTask({ errorMessage: 'Foo', errorType: 'LICENSING' })}
      {...props}
    />
  );
}

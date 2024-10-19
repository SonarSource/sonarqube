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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { setSimpleSettingValue } from '../../../api/settings';
import { mockLoggedInUser, mockRouter } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { PluginRiskConsent, PluginRiskConsentProps } from '../PluginRiskConsent';

jest.mock('../../../api/settings', () => ({
  setSimpleSettingValue: jest.fn().mockResolvedValue({}),
}));

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useEffect: jest.fn().mockImplementation((f) => f()),
  };
});

const originalLocation = window.location;

beforeAll(() => {
  let href = '';
  const location = {
    ...window.location,
    get href() {
      return href;
    },
    set href(v: string) {
      href = v;
    },
  };
  Object.defineProperty(window, 'location', {
    writable: true,
    value: location,
  });
});

afterAll(() => {
  jest.clearAllMocks();

  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation,
  });
});

it('should redirect non-admin users', () => {
  const replace = jest.fn();
  renderPluginRiskConsent({
    currentUser: mockLoggedInUser(),
    router: mockRouter({ replace }),
  });
  expect(replace).toHaveBeenCalled();
});

it('should handle acknowledgement and redirect', async () => {
  const user = userEvent.setup();
  renderPluginRiskConsent();

  await user.click(ui.acknowledgeButton.get());

  await new Promise(setImmediate);

  expect(setSimpleSettingValue).toHaveBeenCalled();
  expect(window.location.href).toBe('/');
});

function renderPluginRiskConsent(props: Partial<PluginRiskConsentProps> = {}) {
  return renderComponent(
    <PluginRiskConsent
      currentUser={mockLoggedInUser({ permissions: { global: ['admin'] } })}
      router={mockRouter()}
      {...props}
    />,
  );
}

const ui = {
  acknowledgeButton: byRole('button', { name: 'plugin_risk_consent.action' }),
};

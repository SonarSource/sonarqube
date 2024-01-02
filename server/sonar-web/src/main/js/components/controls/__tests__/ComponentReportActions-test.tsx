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
  getReportStatus,
  subscribeToEmailReport,
  unsubscribeFromEmailReport,
} from '../../../api/component-report';
import { addGlobalSuccessMessage } from '../../../helpers/globalMessages';
import { mockBranch } from '../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockComponentReportStatus } from '../../../helpers/mocks/component-report';
import { mockAppState, mockCurrentUser } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { ComponentQualifier } from '../../../types/component';
import { ComponentReportActions } from '../ComponentReportActions';

jest.mock('../../../api/component-report', () => ({
  ...jest.requireActual('../../../api/component-report'),
  getReportStatus: jest
    .fn()
    .mockResolvedValue(
      jest.requireActual('../../../helpers/mocks/component-report').mockComponentReportStatus()
    ),
  subscribeToEmailReport: jest.fn().mockResolvedValue(undefined),
  unsubscribeFromEmailReport: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../../../helpers/system', () => ({
  ...jest.requireActual('../../../helpers/system'),
  getBaseUrl: jest.fn().mockReturnValue('baseUrl'),
}));

jest.mock('../../../helpers/globalMessages', () => ({
  addGlobalSuccessMessage: jest.fn(),
}));

beforeEach(jest.clearAllMocks);

it('should not render anything', async () => {
  // loading
  expect(shallowRender().type()).toBeNull();

  // No status
  (getReportStatus as jest.Mock).mockResolvedValueOnce(undefined);
  const w1 = shallowRender();
  await waitAndUpdate(w1);
  expect(w1.type()).toBeNull();

  // Branch purgeable
  const w2 = shallowRender({ branch: mockBranch({ excludedFromPurge: false }) });
  await waitAndUpdate(w2);
  expect(w2.type()).toBeNull();

  // no governance
  const w3 = shallowRender({ appState: mockAppState({ qualifiers: [] }) });
  await waitAndUpdate(w3);
  expect(w3.type()).toBeNull();
});

it('should call for status properly', async () => {
  const component = mockComponent();
  const branch = mockBranch();

  const wrapper = shallowRender({ component, branch });

  await waitAndUpdate(wrapper);

  expect(getReportStatus).toHaveBeenCalledWith(component.key, branch.name);
});

it('should handle subscription', async () => {
  const component = mockComponent();
  const branch = mockBranch();
  const wrapper = shallowRender({ component, branch });

  await wrapper.instance().handleSubscribe();

  expect(subscribeToEmailReport).toHaveBeenCalledWith(component.key, branch.name);
  expect(addGlobalSuccessMessage).toHaveBeenCalledWith(
    'component_report.subscribe_x_success.report.frequency..qualifier.trk'
  );
});

it('should handle unsubscription', async () => {
  const component = mockComponent();
  const branch = mockBranch();
  const wrapper = shallowRender({ component, branch });

  await waitAndUpdate(wrapper);

  wrapper.setState({ status: mockComponentReportStatus({ componentFrequency: 'compfreq' }) });

  await wrapper.instance().handleUnsubscribe();

  expect(unsubscribeFromEmailReport).toHaveBeenCalledWith(component.key, branch.name);
  expect(addGlobalSuccessMessage).toHaveBeenCalledWith(
    'component_report.unsubscribe_x_success.report.frequency.compfreq.qualifier.trk'
  );
});

function shallowRender(props: Partial<ComponentReportActions['props']> = {}) {
  return shallow<ComponentReportActions>(
    <ComponentReportActions
      appState={mockAppState({ qualifiers: [ComponentQualifier.Portfolio] })}
      component={mockComponent()}
      currentUser={mockCurrentUser()}
      {...props}
    />
  );
}

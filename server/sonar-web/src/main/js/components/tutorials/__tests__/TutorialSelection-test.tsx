/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { getAlmSettingsNoCatch } from '../../../api/alm-settings';
import { getValues } from '../../../api/settings';
import {
  mockAlmSettingsInstance,
  mockProjectBitbucketBindingResponse
} from '../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockLocation, mockLoggedInUser, mockRouter } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { getHostUrl } from '../../../helpers/urls';
import { SettingsKey } from '../../../types/settings';
import { TutorialSelection } from '../TutorialSelection';
import { TutorialModes } from '../types';

jest.mock('../../../helpers/urls', () => ({
  getHostUrl: jest.fn().mockReturnValue('http://host.url')
}));

jest.mock('../../../api/alm-settings', () => ({
  getAlmSettingsNoCatch: jest.fn().mockRejectedValue(null)
}));

jest.mock('../../../api/settings', () => ({
  getValues: jest.fn().mockResolvedValue([])
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly find the global ALM binding definition', async () => {
  const key = 'foo';
  const almBinding = mockAlmSettingsInstance({ key });
  (getAlmSettingsNoCatch as jest.Mock).mockResolvedValueOnce([
    almBinding,
    mockAlmSettingsInstance({ key: 'bar' })
  ]);
  const wrapper = shallowRender({ projectBinding: mockProjectBitbucketBindingResponse({ key }) });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().almBinding).toBe(almBinding);
});

it('should handle selection', () => {
  const push = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push }) });
  const instance = wrapper.instance();

  instance.handleSelectTutorial(TutorialModes.Manual);
  expect(push).toHaveBeenLastCalledWith(
    expect.objectContaining({
      query: { selectedTutorial: TutorialModes.Manual }
    })
  );

  instance.handleSelectTutorial(TutorialModes.Jenkins);
  expect(push).toHaveBeenLastCalledWith(
    expect.objectContaining({
      query: { selectedTutorial: TutorialModes.Jenkins }
    })
  );
});

it('should fetch the correct baseUrl', async () => {
  (getValues as jest.Mock)
    .mockResolvedValueOnce([{ key: SettingsKey.ServerBaseUrl, value: '' }])
    .mockResolvedValueOnce([{ key: SettingsKey.ServerBaseUrl, value: 'http://sq.example.com' }])
    .mockRejectedValueOnce(null);

  let wrapper = shallowRender();

  expect(getValues).toBeCalled();
  expect(getHostUrl).toBeCalled();

  // No baseURL, fallback to the URL in the browser.
  await waitAndUpdate(wrapper);
  expect(wrapper.state().baseUrl).toBe('http://host.url');

  // A baseURL was set.
  wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().baseUrl).toBe('http://sq.example.com');

  // Access denied, fallback to the URL in the browser.
  wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().baseUrl).toBe('http://host.url');
});

function shallowRender(props: Partial<TutorialSelection['props']> = {}) {
  return shallow<TutorialSelection>(
    <TutorialSelection
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}

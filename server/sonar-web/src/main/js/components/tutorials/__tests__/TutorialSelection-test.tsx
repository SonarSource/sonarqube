/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getAlmDefinitionsNoCatch, getProjectAlmBinding } from '../../../api/alm-settings';
import { mockBitbucketBindingDefinition } from '../../../helpers/mocks/alm-settings';
import {
  mockComponent,
  mockLocation,
  mockLoggedInUser,
  mockRouter
} from '../../../helpers/testMocks';
import { AlmKeys } from '../../../types/alm-settings';
import { TutorialSelection } from '../TutorialSelection';
import { TutorialModes } from '../types';

jest.mock('../../../api/alm-settings', () => ({
  getProjectAlmBinding: jest.fn().mockRejectedValue(null),
  getAlmDefinitionsNoCatch: jest.fn().mockRejectedValue(null)
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should select manual if project is not bound', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().forceManual).toBe(true);
});

it('should not select anything if project is bound to Bitbucket', async () => {
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce({ alm: AlmKeys.Bitbucket });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().forceManual).toBe(false);
});

it('should select manual if project is bound to unsupported ALM', async () => {
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce({ alm: AlmKeys.Azure });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().forceManual).toBe(true);
});

it('should correctly find the global ALM binding definition', async () => {
  const key = 'foo';
  const almBinding = mockBitbucketBindingDefinition({ key });
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce({ alm: AlmKeys.Bitbucket, key });
  (getAlmDefinitionsNoCatch as jest.Mock).mockResolvedValueOnce({
    [AlmKeys.Bitbucket]: [almBinding]
  });
  const wrapper = shallowRender();
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

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
import {
  countBindedProjects,
  deleteConfiguration,
  getAlmDefinitions
} from '../../../../../api/alm-settings';
import { AlmKeys } from '../../../../../types/alm-settings';
import { AlmIntegration } from '../AlmIntegration';

jest.mock('../../../../../api/alm-settings', () => ({
  countBindedProjects: jest.fn().mockResolvedValue(0),
  deleteConfiguration: jest.fn().mockResolvedValue(undefined),
  getAlmDefinitions: jest.fn().mockResolvedValue({ github: [] })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle alm selection', async () => {
  const wrapper = shallowRender();

  wrapper.setState({ currentAlm: AlmKeys.Azure });

  wrapper.instance().handleSelectAlm(AlmKeys.GitHub);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().currentAlm).toBe(AlmKeys.GitHub);
});

it('should handle delete', async () => {
  const toBeDeleted = '45672';
  (countBindedProjects as jest.Mock).mockResolvedValueOnce(7);
  const wrapper = shallowRender();

  wrapper.instance().handleDelete(toBeDeleted);
  await waitAndUpdate(wrapper);

  expect(wrapper.state().projectCount).toBe(7);
  expect(wrapper.state().definitionKeyForDeletion).toBe(toBeDeleted);
});

it('should delete configuration', async () => {
  (deleteConfiguration as jest.Mock).mockResolvedValueOnce(undefined);
  const wrapper = shallowRender();
  wrapper.instance().deleteConfiguration('8345678');

  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectCount).toBeUndefined();
  expect(wrapper.state().definitionKeyForDeletion).toBeUndefined();
});

it('should fetch settings', async () => {
  const wrapper = shallowRender();

  await wrapper
    .instance()
    .fetchPullRequestDecorationSetting()
    .then(() => {
      expect(getAlmDefinitions).toBeCalled();
      expect(wrapper.state().definitions).toEqual({ github: [] });
      expect(wrapper.state().loading).toBe(false);
    });
});

function shallowRender() {
  return shallow<AlmIntegration>(<AlmIntegration appState={{ branchesEnabled: true }} />);
}

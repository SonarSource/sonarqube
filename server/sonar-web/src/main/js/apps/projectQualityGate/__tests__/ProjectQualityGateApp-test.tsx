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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  associateGateWithProject,
  dissociateGateWithProject,
  fetchQualityGates,
  getGateForProject,
  searchProjects
} from '../../../api/quality-gates';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockQualityGate } from '../../../helpers/mocks/quality-gates';
import { mockComponent } from '../../../helpers/testMocks';
import { USE_SYSTEM_DEFAULT } from '../constants';
import ProjectQualityGateApp from '../ProjectQualityGateApp';

jest.mock('../../../api/quality-gates', () => {
  const { mockQualityGate } = jest.requireActual('../../../helpers/mocks/quality-gates');

  const gate1 = mockQualityGate();
  const gate2 = mockQualityGate({ id: '2', isBuiltIn: true });
  const gate3 = mockQualityGate({ id: '3', isDefault: true });

  return {
    associateGateWithProject: jest.fn().mockResolvedValue(null),
    dissociateGateWithProject: jest.fn().mockResolvedValue(null),
    fetchQualityGates: jest.fn().mockResolvedValue({
      qualitygates: [gate1, gate2, gate3]
    }),
    getGateForProject: jest.fn().mockResolvedValue(gate2),
    searchProjects: jest.fn().mockResolvedValue({ results: [] })
  };
});

jest.mock('../../../app/utils/addGlobalSuccessMessage', () => ({
  default: jest.fn()
}));

jest.mock('../../../app/utils/handleRequiredAuthorization', () => ({
  default: jest.fn()
}));

beforeEach(jest.clearAllMocks);

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('correctly checks user permissions', () => {
  shallowRender({ component: mockComponent({ configuration: { showQualityGates: false } }) });
  expect(handleRequiredAuthorization).toBeCalled();
});

it('correctly loads Quality Gate data', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(fetchQualityGates).toBeCalled();
  expect(getGateForProject).toBeCalledWith({ project: 'foo' });

  expect(wrapper.state().allQualityGates).toHaveLength(3);
  expect(wrapper.state().currentQualityGate?.id).toBe('2');
  expect(wrapper.state().selectedQualityGateId).toBe('2');
});

it('correctly fallbacks to the default Quality Gate', async () => {
  (getGateForProject as jest.Mock).mockResolvedValueOnce(
    mockQualityGate({ id: '3', isDefault: true })
  );
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(searchProjects).toBeCalled();

  expect(wrapper.state().currentQualityGate?.id).toBe('3');
  expect(wrapper.state().selectedQualityGateId).toBe(USE_SYSTEM_DEFAULT);
});

it('correctly detects if the default Quality Gate was explicitly selected', async () => {
  (getGateForProject as jest.Mock).mockResolvedValueOnce(
    mockQualityGate({ id: '3', isDefault: true })
  );
  (searchProjects as jest.Mock).mockResolvedValueOnce({
    results: [{ key: 'foo', selected: true }]
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(searchProjects).toBeCalled();

  expect(wrapper.state().currentQualityGate?.id).toBe('3');
  expect(wrapper.state().selectedQualityGateId).toBe('3');
});

it('correctly associates a selected Quality Gate', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleSelect('3');
  wrapper.instance().handleSubmit();

  expect(associateGateWithProject).toHaveBeenCalledWith({
    gateId: '3',
    projectKey: 'foo'
  });
});

it('correctly associates a project with the system default Quality Gate', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.setState({
    currentQualityGate: mockQualityGate({ id: '1' }),
    selectedQualityGateId: USE_SYSTEM_DEFAULT
  });
  wrapper.instance().handleSubmit();

  expect(dissociateGateWithProject).toHaveBeenCalledWith({
    gateId: '1',
    projectKey: 'foo'
  });
});

it("correctly doesn't do anything if the system default was selected, and the project had no prior Quality Gate associated with it", async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.setState({ currentQualityGate: undefined, selectedQualityGateId: USE_SYSTEM_DEFAULT });
  wrapper.instance().handleSubmit();

  expect(associateGateWithProject).not.toHaveBeenCalled();
  expect(dissociateGateWithProject).not.toHaveBeenCalled();
});

it('correctly handles WS errors', async () => {
  (fetchQualityGates as jest.Mock).mockRejectedValueOnce(null);
  (getGateForProject as jest.Mock).mockRejectedValueOnce(null);
  (searchProjects as jest.Mock).mockRejectedValueOnce(null);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().allQualityGates).toBeUndefined();
  expect(wrapper.state().currentQualityGate).toBeUndefined();
  expect(wrapper.state().loading).toBe(false);

  const result = await wrapper.instance().isUsingDefault(mockQualityGate());
  expect(result).toBe(false);
});

function shallowRender(props: Partial<ProjectQualityGateApp['props']> = {}) {
  return shallow<ProjectQualityGateApp>(
    <ProjectQualityGateApp
      component={mockComponent({ key: 'foo', configuration: { showQualityGates: true } })}
      onComponentChange={jest.fn()}
      {...props}
    />
  );
}

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
  associateGateWithProject,
  dissociateGateWithProject,
  fetchQualityGates,
  getGateForProject,
  searchProjects,
} from '../../../api/quality-gates';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockQualityGate } from '../../../helpers/mocks/quality-gates';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { USE_SYSTEM_DEFAULT } from '../constants';
import { ProjectQualityGateApp } from '../ProjectQualityGateApp';

jest.mock('../../../api/quality-gates', () => {
  const { mockQualityGate } = jest.requireActual('../../../helpers/mocks/quality-gates');
  const { mockCondition } = jest.requireActual('../../../helpers/testMocks');

  const conditions = [mockCondition(), mockCondition({ metric: 'new_bugs' })];
  const gates = {
    gate1: mockQualityGate({ id: 'gate1' }),
    gate2: mockQualityGate({ id: 'gate2', isBuiltIn: true }),
    gate3: mockQualityGate({ id: 'gate3', isDefault: true }),
    gate4: mockQualityGate({ id: 'gate4' }),
  };

  return {
    associateGateWithProject: jest.fn().mockResolvedValue(null),
    dissociateGateWithProject: jest.fn().mockResolvedValue(null),
    fetchQualityGates: jest.fn().mockResolvedValue({
      qualitygates: Object.values(gates),
    }),
    fetchQualityGate: jest.fn().mockImplementation((qg: { id: keyof typeof gates }) => {
      if (qg.id === 'gate4') {
        return Promise.reject();
      }
      return Promise.resolve({ conditions, ...gates[qg.id] });
    }),
    getGateForProject: jest.fn().mockResolvedValue(gates.gate2),
    searchProjects: jest.fn().mockResolvedValue({ results: [] }),
  };
});

jest.mock('../../../helpers/globalMessages', () => ({
  addGlobalSuccessMessage: jest.fn(),
}));

jest.mock('../../../app/utils/handleRequiredAuthorization', () => jest.fn());

beforeEach(jest.clearAllMocks);

it('renders correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('correctly checks user permissions', () => {
  shallowRender({ component: mockComponent({ configuration: { showQualityGates: false } }) });
  expect(handleRequiredAuthorization).toHaveBeenCalled();
});

it('correctly loads Quality Gate data', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(fetchQualityGates).toHaveBeenCalled();
  expect(getGateForProject).toHaveBeenCalledWith({ project: 'foo' });

  expect(wrapper.state().allQualityGates).toHaveLength(4);
  expect(wrapper.state().currentQualityGate?.id).toBe('gate2');
  expect(wrapper.state().selectedQualityGateId).toBe('gate2');
});

it('correctly fallbacks to the default Quality Gate', async () => {
  (getGateForProject as jest.Mock).mockResolvedValueOnce(
    mockQualityGate({ id: 'gate3', isDefault: true })
  );
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(searchProjects).toHaveBeenCalled();

  expect(wrapper.state().currentQualityGate?.id).toBe('gate3');
  expect(wrapper.state().selectedQualityGateId).toBe(USE_SYSTEM_DEFAULT);
});

it('correctly detects if the default Quality Gate was explicitly selected', async () => {
  (getGateForProject as jest.Mock).mockResolvedValueOnce(
    mockQualityGate({ id: 'gate3', isDefault: true })
  );
  (searchProjects as jest.Mock).mockResolvedValueOnce({
    results: [{ key: 'foo', selected: true }],
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(searchProjects).toHaveBeenCalled();

  expect(wrapper.state().currentQualityGate?.id).toBe('gate3');
  expect(wrapper.state().selectedQualityGateId).toBe('gate3');
});

it('correctly associates a selected Quality Gate', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleSelect('gate3');
  wrapper.instance().handleSubmit();

  expect(associateGateWithProject).toHaveBeenCalledWith({
    gateId: 'gate3',
    projectKey: 'foo',
  });
});

it('correctly associates a project with the system default Quality Gate', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.setState({
    currentQualityGate: mockQualityGate({ id: 'gate1' }),
    selectedQualityGateId: USE_SYSTEM_DEFAULT,
  });
  wrapper.instance().handleSubmit();

  expect(dissociateGateWithProject).toHaveBeenCalledWith({
    gateId: 'gate1',
    projectKey: 'foo',
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

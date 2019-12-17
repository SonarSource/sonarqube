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
import { fetchQualityGate } from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockCondition } from '../../../../helpers/testMocks';
import { addCondition, deleteCondition, replaceCondition } from '../../utils';
import { Details } from '../Details';

jest.mock('../../../../api/quality-gates', () => {
  const { mockQualityGate } = jest.requireActual('../../../../helpers/mocks/quality-gates');
  return {
    fetchQualityGate: jest.fn().mockResolvedValue(mockQualityGate())
  };
});

jest.mock('../../utils', () => ({
  checkIfDefault: jest.fn(() => false),
  addCondition: jest.fn(qg => qg),
  deleteCondition: jest.fn(qg => qg),
  replaceCondition: jest.fn(qg => qg)
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');

  await waitAndUpdate(wrapper);
  expect(fetchQualityGate).toBeCalledWith({ id: '1' });
  expect(wrapper).toMatchSnapshot('loaded');
});

it('should refresh if the QG id changes', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  jest.clearAllMocks();

  wrapper.setProps({ id: '2' });
  expect(fetchQualityGate).toBeCalledWith({ id: '2' });
});

it('should fetch metrics on mount', () => {
  const fetchMetrics = jest.fn();
  shallowRender({ fetchMetrics });
  expect(fetchMetrics).toBeCalled();
});

it('should correctly add/replace/remove conditions', async () => {
  const qualityGate = mockQualityGate();
  (fetchQualityGate as jest.Mock).mockResolvedValue(qualityGate);

  const wrapper = shallowRender();
  const instance = wrapper.instance();

  instance.handleAddCondition(mockCondition());
  expect(wrapper.state().qualityGate).toBeUndefined();

  instance.handleSaveCondition(mockCondition(), mockCondition());
  expect(wrapper.state().qualityGate).toBeUndefined();

  instance.handleRemoveCondition(mockCondition());
  expect(wrapper.state().qualityGate).toBeUndefined();

  await waitAndUpdate(wrapper);

  const newCondition = mockCondition({ metric: 'bugs', id: 2 });
  instance.handleAddCondition(newCondition);
  expect(addCondition).toBeCalledWith(qualityGate, newCondition);

  const updatedCondition = mockCondition({ metric: 'new_bugs' });
  instance.handleSaveCondition(newCondition, updatedCondition);
  expect(replaceCondition).toBeCalledWith(qualityGate, newCondition, updatedCondition);

  instance.handleRemoveCondition(newCondition);
  expect(deleteCondition).toBeCalledWith(qualityGate, newCondition);
});

it('should correctly handle setting default', async () => {
  const qualityGate = mockQualityGate();
  (fetchQualityGate as jest.Mock).mockResolvedValue(qualityGate);

  const onSetDefault = jest.fn();
  const wrapper = shallowRender({ onSetDefault });

  wrapper.instance().handleSetDefault();
  expect(wrapper.state().qualityGate).toBeUndefined();

  await waitAndUpdate(wrapper);

  wrapper.instance().handleSetDefault();
  expect(wrapper.state().qualityGate).toEqual(
    expect.objectContaining({
      id: qualityGate.id,
      actions: { delete: false, setAsDefault: false }
    })
  );
});

function shallowRender(props: Partial<Details['props']> = {}) {
  return shallow<Details>(
    <Details
      fetchMetrics={jest.fn()}
      id="1"
      metrics={{}}
      onSetDefault={jest.fn()}
      qualityGates={[mockQualityGate()]}
      refreshQualityGates={jest.fn()}
      {...props}
    />
  );
}

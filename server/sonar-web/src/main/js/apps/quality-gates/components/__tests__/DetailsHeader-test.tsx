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
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { setQualityGateAsDefault } from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import DetailsHeader from '../DetailsHeader';

jest.mock('../../../../api/quality-gates', () => ({
  setQualityGateAsDefault: jest.fn().mockResolvedValue(null)
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ qualityGate: mockQualityGate({ isBuiltIn: true }) })).toMatchSnapshot(
    'built-in'
  );
  expect(
    shallowRender({
      qualityGate: mockQualityGate({
        actions: {
          copy: true,
          delete: true,
          rename: true,
          setAsDefault: true
        }
      })
    })
  ).toMatchSnapshot('admin actions');
});

it('should allow the QG to be set as the default', async () => {
  const onSetDefault = jest.fn();
  const refreshItem = jest.fn();
  const refreshList = jest.fn();

  const qualityGate = mockQualityGate({ id: 1, actions: { setAsDefault: true } });
  const wrapper = shallowRender({ onSetDefault, qualityGate, refreshItem, refreshList });

  click(wrapper.find('Button#quality-gate-toggle-default'));
  expect(setQualityGateAsDefault).toBeCalledWith({ id: 1 });
  expect(onSetDefault).toBeCalled();
  await waitAndUpdate(wrapper);
  expect(refreshItem).toBeCalled();
  expect(refreshList).toBeCalled();

  jest.clearAllMocks();

  wrapper.setProps({ qualityGate: mockQualityGate({ ...qualityGate, isDefault: true }) });
  click(wrapper.find('Button#quality-gate-toggle-default'));
  expect(setQualityGateAsDefault).not.toBeCalled();
  expect(onSetDefault).not.toBeCalled();
  await waitAndUpdate(wrapper);
  expect(refreshItem).not.toBeCalled();
  expect(refreshList).not.toBeCalled();
});

function shallowRender(props: Partial<DetailsHeader['props']> = {}) {
  return shallow<DetailsHeader>(
    <DetailsHeader
      onSetDefault={jest.fn()}
      qualityGate={mockQualityGate()}
      refreshItem={jest.fn().mockResolvedValue(null)}
      refreshList={jest.fn().mockResolvedValue(null)}
      {...props}
    />
  );
}

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
import ScreenPositionHelper from '../../../../components/common/ScreenPositionHelper';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass
} from '../../../../helpers/pages';
import { mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import App from '../App';

jest.mock('../../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  addWhitePageClass: jest.fn(),
  removeSideBarClass: jest.fn(),
  removeWhitePageClass: jest.fn()
}));

jest.mock('../../../../api/quality-gates', () => {
  const { mockQualityGate } = jest.requireActual('../../../../helpers/mocks/quality-gates');
  return {
    fetchQualityGates: jest.fn().mockResolvedValue({
      actions: { create: true },
      qualitygates: [
        mockQualityGate(),
        mockQualityGate({ id: '2', name: 'qualitygate 2', isDefault: true })
      ]
    })
  };
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  const replace = jest.fn(() => wrapper.setProps({ params: { id: '2' } }));
  wrapper.setProps({ router: mockRouter({ replace }) });

  expect(wrapper).toMatchSnapshot('default');
  expect(wrapper.find(ScreenPositionHelper).dive()).toMatchSnapshot('ScreenPositionHelper');

  await waitAndUpdate(wrapper);

  // No ID parameter passed, it should redirect to the default gate.
  expect(replace).toBeCalledWith({ pathname: '/quality_gates/show/2' });
  expect(wrapper).toMatchSnapshot('default gate');

  // Pass an ID, show a specific gate.
  wrapper.setProps({ params: { id: '1' } });
  expect(wrapper).toMatchSnapshot('specific gate');

  expect(addSideBarClass).toBeCalled();
  expect(addWhitePageClass).toBeCalled();

  wrapper.unmount();
  expect(removeSideBarClass).toBeCalled();
  expect(removeWhitePageClass).toBeCalled();
});

it('should handle set default correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().qualityGates?.find(gate => gate.isDefault)?.id).toBe('2');
  wrapper.instance().handleSetDefault(mockQualityGate({ id: '1' }));
  expect(wrapper.state().qualityGates?.find(gate => gate.isDefault)?.id).toBe('1');
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(<App params={{}} router={mockRouter()} {...props} />);
}

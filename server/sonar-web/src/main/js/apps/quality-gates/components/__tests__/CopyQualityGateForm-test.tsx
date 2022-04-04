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
import { copyQualityGate } from '../../../../api/quality-gates';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { mockRouter } from '../../../../helpers/testMocks';
import { CopyQualityGateForm } from '../CopyQualityGateForm';

jest.mock('../../../../api/quality-gates', () => ({
  copyQualityGate: jest.fn().mockResolvedValue({})
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle copy', async () => {
  const onCopy = jest.fn();
  const router = mockRouter();
  const qualityGate = mockQualityGate();
  const wrapper = shallowRender({ onCopy, qualityGate, router });

  const name = 'name';
  wrapper.setState({ name });

  await wrapper.instance().handleCopy();

  expect(copyQualityGate).toBeCalledWith({ id: qualityGate.id, name });
  expect(onCopy).toBeCalled();
  expect(router.push).toBeCalled();

  jest.clearAllMocks();

  wrapper.setState({ name: '' });
  await wrapper.instance().handleCopy();

  expect(copyQualityGate).not.toBeCalled();
});

function shallowRender(overrides: Partial<CopyQualityGateForm['props']> = {}) {
  return shallow<CopyQualityGateForm>(
    <CopyQualityGateForm
      onClose={jest.fn()}
      onCopy={jest.fn()}
      qualityGate={mockQualityGate()}
      router={mockRouter()}
      {...overrides}
    />
  );
}

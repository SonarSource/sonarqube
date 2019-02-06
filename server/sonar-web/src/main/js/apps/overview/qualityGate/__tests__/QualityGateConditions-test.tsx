/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import QualityGateConditions from '../QualityGateConditions';
import { getMeasuresAndMeta } from '../../../../api/measures';
import { mockComponent, mockQualityGateStatusCondition } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/measures', () => {
  return {
    getMeasuresAndMeta: jest.fn().mockResolvedValue({
      component: { measures: [{ metric: 'foo' }] },
      metrics: [{ key: 'foo' }]
    })
  };
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getMeasuresAndMeta).toBeCalled();
  expect(wrapper.find('QualityGateCondition').length).toBe(10);
});

it('should be collapsible', async () => {
  const wrapper = shallowRender({ collapsible: true });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('QualityGateCondition').length).toBe(5);
  wrapper.setState({ collapsed: false });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('QualityGateCondition').length).toBe(10);
});

function shallowRender(props: Partial<QualityGateConditions['props']> = {}) {
  const conditions: T.QualityGateStatusCondition[] = [];
  for (let i = 10; i > 0; --i) {
    conditions.push(mockQualityGateStatusCondition());
  }

  return shallow(
    <QualityGateConditions component={mockComponent()} conditions={conditions} {...props} />
  );
}

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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockCondition, mockMetric, mockQualityGate } from '../../../../helpers/testMocks';
import { Conditions } from '../Conditions';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly with new code conditions', () => {
  const wrapper = shallowRender({
    conditions: [
      mockCondition(),
      mockCondition({ id: 2, metric: 'duplication' }),
      mockCondition({ id: 3, metric: 'new_coverage' }),
      mockCondition({ id: 4, metric: 'new_duplication' })
    ]
  });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for no conditions', () => {
  const wrapper = shallowRender({ conditions: [] });
  expect(wrapper).toMatchSnapshot();
});

it('should render the add conditions button and modal', () => {
  const wrapper = shallowRender({ canEdit: true });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<Conditions['props']> = {}) {
  return shallow<Conditions>(
    <Conditions
      appState={{ branchesEnabled: true }}
      canEdit={false}
      conditions={[mockCondition(), mockCondition({ id: 2, metric: 'duplication' })]}
      metrics={{
        coverage: mockMetric(),
        duplication: mockMetric({ id: 'duplication', key: 'duplication', name: 'Duplication' }),
        new_coverage: mockMetric({
          id: 'new_coverage',
          key: 'new_coverage',
          name: 'Coverage on New Code'
        }),
        new_duplication: mockMetric({
          id: 'new_duplication',
          key: 'new_duplication',
          name: 'Duplication on New Code'
        })
      }}
      onAddCondition={jest.fn()}
      onRemoveCondition={jest.fn()}
      onSaveCondition={jest.fn()}
      qualityGate={mockQualityGate()}
      {...props}
    />
  );
}

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
import Condition from '../Condition';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly with edit rights', () => {
  const wrapper = shallowRender({ canEdit: true });
  expect(wrapper).toMatchSnapshot();
});

it('should render the update modal correctly', () => {
  const wrapper = shallowRender({ canEdit: true });
  wrapper.instance().handleOpenUpdate();
  expect(wrapper).toMatchSnapshot();
});

it('should render the delete modal correctly', () => {
  const wrapper = shallowRender({ canEdit: true });
  wrapper.instance().handleDeleteClick();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<Condition['props']> = {}) {
  return shallow<Condition>(
    <Condition
      canEdit={false}
      condition={mockCondition()}
      metric={mockMetric()}
      onRemoveCondition={jest.fn()}
      onSaveCondition={jest.fn()}
      qualityGate={mockQualityGate()}
      {...props}
    />
  );
}

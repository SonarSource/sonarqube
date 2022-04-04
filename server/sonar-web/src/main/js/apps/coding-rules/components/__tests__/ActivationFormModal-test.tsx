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
import {
  mockQualityProfile,
  mockRule,
  mockRuleActivation,
  mockRuleDetails,
  mockRuleDetailsParameter
} from '../../../../helpers/testMocks';
import ActivationFormModal from '../ActivationFormModal';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      profiles: [
        mockQualityProfile(),
        mockQualityProfile({ depth: 2, actions: { edit: true }, language: 'js' })
      ]
    })
  ).toMatchSnapshot('with deep profiles');
  expect(shallowRender({ rule: mockRuleDetails({ templateKey: 'foobar' }) })).toMatchSnapshot(
    'custom rule'
  );
  expect(shallowRender({ activation: mockRuleActivation() })).toMatchSnapshot('update mode');
  const wrapper = shallowRender();
  wrapper.setState({ submitting: true });
  expect(wrapper).toMatchSnapshot('submitting');
});

function shallowRender(props: Partial<ActivationFormModal['props']> = {}) {
  return shallow<ActivationFormModal>(
    <ActivationFormModal
      modalHeader="title"
      onClose={jest.fn()}
      onDone={jest.fn()}
      profiles={[mockQualityProfile()]}
      rule={mockRule({
        params: [
          mockRuleDetailsParameter(),
          mockRuleDetailsParameter({ key: '2', type: 'TEXT', htmlDesc: undefined })
        ]
      })}
      {...props}
    />
  );
}

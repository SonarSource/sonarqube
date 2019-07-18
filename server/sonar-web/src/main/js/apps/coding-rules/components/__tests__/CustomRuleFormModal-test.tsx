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
import { submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { createRule } from '../../../../api/rules';
import { mockRule } from '../../../../helpers/testMocks';
import CustomRuleFormModal from '../CustomRuleFormModal';

jest.mock('../../../../api/rules', () => ({ createRule: jest.fn() }));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle re-activation', async () => {
  (createRule as jest.Mock).mockRejectedValue({ status: 409 });
  const wrapper = shallowRender();
  submit(wrapper.find('form'));
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<CustomRuleFormModal['props']> = {}) {
  return shallow(
    <CustomRuleFormModal
      onClose={jest.fn()}
      onDone={jest.fn()}
      organization={undefined}
      templateRule={{ ...mockRule(), createdAt: 'date', repo: 'squid' }}
      {...props}
    />
  );
}

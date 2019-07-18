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
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { Profile } from '../../../../api/quality-profiles';
import ComparisonResultActivation from '../ComparisonResultActivation';

jest.mock('../../../../api/rules', () => ({
  getRuleDetails: jest.fn().mockResolvedValue({ key: 'foo' })
}));

it('should activate', async () => {
  const profile = { actions: { edit: true }, key: 'profile-key' } as Profile;
  const wrapper = shallow(
    <ComparisonResultActivation onDone={jest.fn()} profile={profile} ruleKey="foo" />
  );
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('Button'));
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper.find('ActivationFormModal').prop<Function>('onClose')();
  expect(wrapper).toMatchSnapshot();
});

/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { mockQualityProfile } from '../../../../helpers/testMocks';
import { mockEvent } from '../../../../helpers/testUtils';
import BulkChange from '../BulkChange';

const profile = mockQualityProfile({
  actions: {
    edit: true,
    setAsDefault: true,
    copy: true,
    associateProjects: true,
    delete: false,
  },
});

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should not a disabled button when edition is not possible', () => {
  const wrapper = shallowRender({
    referencedProfiles: { key: { ...profile, actions: { ...profile.actions, edit: false } } },
  });
  expect(wrapper).toMatchSnapshot();
});

it('should display BulkChangeModal', () => {
  const wrapper = shallowRender();
  wrapper.instance().handleActivateClick(mockEvent());
  expect(wrapper.find('withLanguagesContext(BulkChangeModal)').exists()).toBe(true);
});

function shallowRender(props: Partial<BulkChange['props']> = {}) {
  return shallow<BulkChange>(
    <BulkChange
      query={{ activation: false, profile: 'key' } as BulkChange['props']['query']}
      referencedProfiles={{ key: profile }}
      total={2}
      {...props}
    />
  );
}

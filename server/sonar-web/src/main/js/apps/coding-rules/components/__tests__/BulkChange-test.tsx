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
import BulkChange from '../BulkChange';
import { mockEvent, mockQualityProfile } from '../../../../helpers/testMocks';

const profile = mockQualityProfile({
  actions: {
    edit: true,
    setAsDefault: true,
    copy: true,
    associateProjects: true,
    delete: false
  }
});

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should not render anything', () => {
  const wrapper = shallowRender({
    referencedProfiles: { key: { ...profile, actions: { ...profile.actions, edit: false } } }
  });
  expect(wrapper.type()).toBeNull();
});

it('should display BulkChangeModal', () => {
  const wrapper = shallowRender();
  wrapper.instance().handleActivateClick(mockEvent());
  expect(wrapper.find('BulkChangeModal')).toMatchSnapshot();
});

function shallowRender(props: Partial<BulkChange['props']> = {}) {
  return shallow<BulkChange>(
    <BulkChange
      languages={{ js: { key: 'js', name: 'JavaScript' } }}
      organization={undefined}
      query={{ activation: false, profile: 'key' } as BulkChange['props']['query']}
      referencedProfiles={{ key: profile }}
      total={2}
      {...props}
    />
  );
}

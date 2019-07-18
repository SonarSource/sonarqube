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
import OrganizationDetailsStep from '../OrganizationDetailsStep';

it('should render form', () => {
  const wrapper = shallow(
    <OrganizationDetailsStep finished={false} onOpen={jest.fn()} open={true}>
      <form />
    </OrganizationDetailsStep>
  );
  expect(wrapper).toMatchSnapshot();
  expect(
    wrapper
      .dive()
      .find('form')
      .exists()
  ).toBe(true);
});

it('should render result', () => {
  const wrapper = shallow(
    <OrganizationDetailsStep
      finished={true}
      onOpen={jest.fn()}
      open={false}
      organization={{ avatar: '', description: '', key: 'org', name: 'Organization', url: '' }}>
      <div />
    </OrganizationDetailsStep>
  );
  expect(wrapper.dive().find('.boxed-group-actions')).toMatchSnapshot();
  expect(
    wrapper
      .dive()
      .find('.hidden')
      .exists()
  ).toBe(true);
});

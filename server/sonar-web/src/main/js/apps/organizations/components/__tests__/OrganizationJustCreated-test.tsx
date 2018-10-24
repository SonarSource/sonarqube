/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { OrganizationJustCreated } from '../OrganizationJustCreated';
import { Organization } from '../../../../app/types';
import { click } from '../../../../helpers/testUtils';

const organization: Organization = { key: 'foo', name: 'Foo' };

it('should render', () => {
  // @ts-ignore
  expect(shallow(<OrganizationJustCreated organization={organization} />)).toMatchSnapshot();
});

it('should create new project', () => {
  const openProjectOnboarding = jest.fn();
  // @ts-ignore
  const wrapper = shallow(<OrganizationJustCreated organization={organization} />, {
    context: { openProjectOnboarding }
  });
  click(wrapper.find('Button').first());
  expect(openProjectOnboarding).toBeCalledWith({ key: 'foo', name: 'Foo' });
});

it('should add members', () => {
  const router = { push: jest.fn() };
  // @ts-ignore
  const wrapper = shallow(<OrganizationJustCreated organization={organization} router={router} />);
  click(wrapper.find('Button').last());
  expect(router.push).toBeCalledWith('/organizations/foo/members');
});

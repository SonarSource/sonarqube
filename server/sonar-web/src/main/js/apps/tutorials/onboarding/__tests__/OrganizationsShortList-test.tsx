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
import { mockOrganization } from '../../../../helpers/testMocks';
import OrganizationsShortList, { Props } from '../OrganizationsShortList';

it('should render null with no orgs', () => {
  expect(shallowRender().getElement()).toBe(null);
});

it('should render correctly', () => {
  const wrapper = shallowRender({
    organizations: [mockOrganization(), mockOrganization({ key: 'bar', name: 'Bar' })]
  });

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('li')).toHaveLength(2);
});

it('should limit displayed orgs to the first three', () => {
  const wrapper = shallowRender({
    organizations: [
      mockOrganization(),
      mockOrganization({ key: 'zoo', name: 'Zoological' }),
      mockOrganization({ key: 'bar', name: 'Bar' }),
      mockOrganization({ key: 'kor', name: 'Kor' })
    ]
  });

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('li')).toHaveLength(3);
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(<OrganizationsShortList onClick={jest.fn()} organizations={[]} {...props} />);
}

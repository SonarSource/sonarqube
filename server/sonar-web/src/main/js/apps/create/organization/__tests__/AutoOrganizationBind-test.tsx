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
import { submit } from 'sonar-ui-common/helpers/testUtils';
import { mockOrganization } from '../../../../helpers/testMocks';
import AutoOrganizationBind from '../AutoOrganizationBind';

it('should render correctly', () => {
  const onBindOrganization = jest.fn().mockResolvedValue({});
  const wrapper = shallowRender({ onBindOrganization });
  expect(wrapper).toMatchSnapshot();

  submit(wrapper.find('form'));
  expect(onBindOrganization).toHaveBeenCalled();
});

it('should not show member sync info box for Bitbucket', () => {
  expect(
    shallowRender({ almKey: 'bitbucket' })
      .find('Alert')
      .exists()
  ).toBe(false);
});

function shallowRender(props: Partial<AutoOrganizationBind['props']> = {}) {
  return shallow(
    <AutoOrganizationBind
      almKey="github"
      onBindOrganization={jest.fn()}
      unboundOrganizations={[mockOrganization()]}
      {...props}
    />
  );
}

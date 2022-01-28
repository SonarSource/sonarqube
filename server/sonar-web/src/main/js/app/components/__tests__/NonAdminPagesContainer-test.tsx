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
import { mockComponent } from '../../../helpers/mocks/component';
import { ComponentQualifier } from '../../../types/component';
import NonAdminPagesContainer, { NonAdminPagesContainerProps } from '../NonAdminPagesContainer';

function Child() {
  return <div />;
}

it('should render correctly', () => {
  expect(
    shallowRender()
      .find(Child)
      .exists()
  ).toBe(true);

  expect(
    shallowRender({
      component: mockComponent({
        qualifier: ComponentQualifier.Application,
        canBrowseAllChildProjects: true
      })
    })
      .find(Child)
      .exists()
  ).toBe(true);

  const wrapper = shallowRender({
    component: mockComponent({
      qualifier: ComponentQualifier.Application
    })
  });

  expect(wrapper.find(Child).exists()).toBe(false);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<NonAdminPagesContainerProps> = {}) {
  return shallow<NonAdminPagesContainerProps>(
    <NonAdminPagesContainer
      branchLikes={[]}
      component={mockComponent()}
      onBranchesChange={jest.fn()}
      onComponentChange={jest.fn()}
      {...props}>
      <Child />
    </NonAdminPagesContainer>
  );
}

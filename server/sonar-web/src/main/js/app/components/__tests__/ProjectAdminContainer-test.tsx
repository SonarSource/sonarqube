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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockComponent } from '../../../helpers/testMocks';
import ProjectAdminContainer from '../ProjectAdminContainer';

jest.mock('../../utils/handleRequiredAuthorization', () => {
  return { default: jest.fn() };
});

class ChildComponent extends React.Component {
  render() {
    return null;
  }
}

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should redirect for authorization if needed', () => {
  mountRender({ component: mockComponent({ configuration: { showSettings: false } }) });
  expect(handleRequiredAuthorization).toBeCalled();
});

it('should pass props to its children', () => {
  const child = shallowRender().find(ChildComponent);
  // No need to check all...
  expect(child.prop('component')).toBeDefined();
  expect(child.prop('onBranchesChange')).toBeDefined();
});

function mountRender(props: Partial<ProjectAdminContainer['props']> = {}) {
  return mount(createComponent(props));
}

function shallowRender(props: Partial<ProjectAdminContainer['props']> = {}) {
  return shallow(createComponent(props));
}

function createComponent(props: Partial<ProjectAdminContainer['props']> = {}) {
  return (
    <ProjectAdminContainer
      branchLikes={[]}
      component={mockComponent({ configuration: { showSettings: true } })}
      onBranchesChange={jest.fn()}
      onComponentChange={jest.fn()}
      {...props}>
      <ChildComponent />
    </ProjectAdminContainer>
  );
}

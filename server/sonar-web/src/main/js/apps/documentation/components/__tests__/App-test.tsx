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
import { addSideBarClass, removeSideBarClass } from '../../../../helpers/pages';
import App from '../App';

jest.mock('../../../../components/common/ScreenPositionHelper', () => ({
  default: class ScreenPositionHelper extends React.Component<{
    children: (pos: { top: number }) => React.ReactNode;
  }> {
    static displayName = 'ScreenPositionHelper';
    render() {
      return this.props.children({ top: 0 });
    }
  }
}));

jest.mock(
  'Docs/../static/SonarQubeNavigationTree.json',
  () => [
    {
      title: 'SonarQube',
      children: ['/lorem/ipsum/']
    }
  ],
  { virtual: true }
);

jest.mock(
  'Docs/../static/SonarCloudNavigationTree.json',
  () => [
    {
      title: 'SonarCloud',
      children: ['/lorem/ipsum/']
    }
  ],
  { virtual: true }
);

jest.mock('../../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  removeSideBarClass: jest.fn()
}));

jest.mock('../../pages', () => {
  const { mockDocumentationEntry } = require.requireActual('../../../../helpers/testMocks');
  return {
    default: () => [mockDocumentationEntry()]
  };
});

it('should render correctly', () => {
  const wrapper = shallowRender();

  expect(wrapper).toMatchSnapshot();
  expect(addSideBarClass).toBeCalled();

  expect(wrapper.find('ScreenPositionHelper').dive()).toMatchSnapshot();

  wrapper.unmount();
  expect(removeSideBarClass).toBeCalled();
});

it("should show a 404 if the page doesn't exist", () => {
  const wrapper = shallowRender({ params: { splat: 'unknown' } });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow(<App params={{ splat: 'lorem/ipsum' }} {...props} />);
}

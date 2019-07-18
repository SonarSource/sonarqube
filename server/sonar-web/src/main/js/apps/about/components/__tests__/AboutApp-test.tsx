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
import { addWhitePageClass, removeWhitePageClass } from 'sonar-ui-common/helpers/pages';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { searchProjects } from '../../../../api/components';
import { getFacet } from '../../../../api/issues';
import { mockAppState, mockCurrentUser, mockLocation } from '../../../../helpers/testMocks';
import { AboutApp } from '../AboutApp';

jest.mock('sonar-ui-common/helpers/pages', () => ({
  addWhitePageClass: jest.fn(),
  removeWhitePageClass: jest.fn()
}));

jest.mock('../../../../api/components', () => ({
  searchProjects: jest.fn().mockResolvedValue({ paging: { total: 5 } })
}));

jest.mock('../../../../api/issues', () => ({
  getFacet: jest.fn().mockResolvedValue({
    facet: [
      { count: 5, val: 'CODE_SMELL' },
      { count: 10, val: 'BUG' },
      { count: 0, val: 'VULNERABILITY' },
      { count: 5, val: 'SECURITY_HOTSPOT' }
    ]
  })
}));

jest.mock('../../../../app/components/GlobalContainer', () => ({
  default: class GlobalContainer extends React.Component {
    static displayName = 'GlobalContainer';
    render() {
      return this.props.children;
    }
  }
}));

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
  expect(addWhitePageClass).toBeCalled();

  wrapper.unmount();
  expect(removeWhitePageClass).toBeCalled();
});

it('should load issues, projects, and custom text upon shallowing', () => {
  const fetchAboutPageSettings = jest.fn();
  shallowRender({ fetchAboutPageSettings });
  expect(fetchAboutPageSettings).toBeCalled();
  expect(searchProjects).toBeCalled();
  expect(getFacet).toBeCalled();
});

function shallowRender(props: Partial<AboutApp['props']> = {}) {
  return shallow(
    <AboutApp
      appState={mockAppState()}
      currentUser={mockCurrentUser()}
      customText="Lorem ipsum"
      fetchAboutPageSettings={jest.fn().mockResolvedValue('')}
      location={mockLocation()}
      {...props}
    />
  );
}

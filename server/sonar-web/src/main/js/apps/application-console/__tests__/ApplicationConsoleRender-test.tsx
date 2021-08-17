/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { mockApplication } from '../../../helpers/mocks/application';
import { click } from '../../../sonar-ui-common/helpers/testUtils';
import ApplicationConsoleAppRenderer, {
  ApplicationConsoleAppRendererProps
} from '../ApplicationConsoleAppRenderer';
import EditForm from '../EditForm';

jest.mock('../../../app/utils/addGlobalSuccessMessage', () => ({
  default: jest.fn()
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      application: mockApplication({ description: 'Foo bar', key: 'foo' })
    })
  ).toMatchSnapshot('can recompute');
  expect(shallowRender({ loading: true })).toMatchSnapshot('is loading');
});

it('should handle editing', () => {
  const wrapper = shallowRender();
  click(wrapper.find('#view-details-edit'));
  expect(wrapper.find(EditForm)).toMatchSnapshot('edit form');
});

function shallowRender(props: Partial<ApplicationConsoleAppRendererProps> = {}) {
  return shallow(
    <ApplicationConsoleAppRenderer
      application={mockApplication({ key: 'foo' })}
      loading={false}
      onAddProject={jest.fn()}
      onEdit={jest.fn()}
      onRefresh={jest.fn()}
      onRemoveProject={jest.fn()}
      onUpdateBranches={jest.fn()}
      {...props}
    />
  );
}

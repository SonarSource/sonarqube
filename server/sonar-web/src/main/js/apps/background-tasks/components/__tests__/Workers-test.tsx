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
import { click } from 'sonar-ui-common/helpers/testUtils';
import Workers from '../Workers';

it('renders', () => {
  const wrapper = shallow(<Workers />);
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({
    canSetWorkerCount: true,
    loading: false,
    workerCount: 1
  });
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({ workerCount: 2 });
  expect(wrapper).toMatchSnapshot();

  wrapper.setState({ canSetWorkerCount: false });
  expect(wrapper).toMatchSnapshot();
});

it('opens form', () => {
  const wrapper = shallow(<Workers />);

  wrapper.setState({
    canSetWorkerCount: true,
    loading: false,
    workerCount: 1
  });
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.js-edit'));
  expect(wrapper).toMatchSnapshot();
});

it('updates worker count', () => {
  const wrapper = shallow(<Workers />);

  wrapper.setState({
    canSetWorkerCount: true,
    formOpen: true,
    loading: false,
    workerCount: 1
  });
  expect(wrapper).toMatchSnapshot();

  wrapper.find('WorkersForm').prop<Function>('onClose')(7);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

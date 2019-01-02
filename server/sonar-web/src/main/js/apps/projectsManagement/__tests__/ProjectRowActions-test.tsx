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
import ProjectRowActions, { Props } from '../ProjectRowActions';
import { click, waitAndUpdate } from '../../../helpers/testUtils';
import { Project } from '../../../api/components';

jest.mock('../../../api/components', () => ({
  getComponentShow: jest.fn(() => Promise.reject(undefined))
}));

jest.mock('../../../api/nav', () => ({
  getComponentNavigation: jest.fn(() => Promise.resolve())
}));

const project: Project = {
  id: '',
  key: 'project',
  name: 'Project',
  organization: 'org',
  qualifier: 'TRK',
  visibility: 'private'
};

it('restores access', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  wrapper.find('ActionsDropdown').prop<Function>('onOpen')();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.js-restore-access'));
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('applies permission template', () => {
  const wrapper = shallowRender();
  click(wrapper.find('.js-apply-template'));
  expect(wrapper.find('ApplyTemplate')).toMatchSnapshot();
});

function shallowRender(props: Partial<Props> = {}) {
  const wrapper = shallow(
    <ProjectRowActions
      currentUser={{ login: 'admin' }}
      organization="org"
      project={project}
      {...props}
    />
  );
  (wrapper.instance() as ProjectRowActions).mounted = true;
  return wrapper;
}

/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import CreateProjectModeSelection, {
  CreateProjectModeSelectionProps
} from '../CreateProjectModeSelection';
import { CreateProjectModes } from '../types';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loadingBindings: true })).toMatchSnapshot('loading bbs instances');
  expect(shallowRender({ bbsBindingCount: 0 })).toMatchSnapshot('no bbs instances');
  expect(shallowRender({ bbsBindingCount: 2 })).toMatchSnapshot('too many bbs instances');
});

it('should correctly pass the selected mode up', () => {
  const onSelectMode = jest.fn();
  const wrapper = shallowRender({ onSelectMode });

  click(wrapper.find('button.create-project-mode-type-manual'));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.Manual);

  click(wrapper.find('button.create-project-mode-type-bbs'));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.BitbucketServer);
});

function shallowRender(props: Partial<CreateProjectModeSelectionProps> = {}) {
  return shallow<CreateProjectModeSelectionProps>(
    <CreateProjectModeSelection
      bbsBindingCount={1}
      loadingBindings={false}
      onSelectMode={jest.fn()}
      {...props}
    />
  );
}

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
import { AlmKeys } from '../../../../types/alm-settings';
import CreateProjectModeSelection, {
  CreateProjectModeSelectionProps
} from '../CreateProjectModeSelection';
import { CreateProjectModes } from '../types';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loadingBindings: true })).toMatchSnapshot('loading instances');
  expect(shallowRender({}, { [AlmKeys.Bitbucket]: 0, [AlmKeys.GitHub]: 2 })).toMatchSnapshot(
    'invalid configs'
  );
});

it('should correctly pass the selected mode up', () => {
  const onSelectMode = jest.fn();
  const wrapper = shallowRender({ onSelectMode });

  click(wrapper.find('button.create-project-mode-type-manual'));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.Manual);

  click(wrapper.find('button.create-project-mode-type-alm').at(0));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.BitbucketServer);

  click(wrapper.find('button.create-project-mode-type-alm').at(1));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.GitHub);
});

function shallowRender(
  props: Partial<CreateProjectModeSelectionProps> = {},
  almCountOverrides = {}
) {
  const almCounts = {
    [AlmKeys.Azure]: 0,
    [AlmKeys.Bitbucket]: 1,
    [AlmKeys.GitHub]: 0,
    [AlmKeys.GitLab]: 0,
    ...almCountOverrides
  };
  return shallow<CreateProjectModeSelectionProps>(
    <CreateProjectModeSelection
      almCounts={almCounts}
      loadingBindings={false}
      onSelectMode={jest.fn()}
      {...props}
    />
  );
}

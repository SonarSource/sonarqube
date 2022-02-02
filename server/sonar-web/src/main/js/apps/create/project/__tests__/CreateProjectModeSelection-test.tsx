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
import { mockAppState } from '../../../../helpers/testMocks';
import { click } from '../../../../helpers/testUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import {
  CreateProjectModeSelection,
  CreateProjectModeSelectionProps
} from '../CreateProjectModeSelection';
import { CreateProjectModes } from '../types';

it('should render correctly', () => {
  expect(shallowRender({ loadingBindings: true })).toMatchSnapshot('loading instances');
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({}, { [AlmKeys.BitbucketServer]: 0, [AlmKeys.GitHub]: 2 })).toMatchSnapshot(
    'invalid configs, not admin'
  );
  expect(
    shallowRender(
      { appState: mockAppState({ canAdmin: true }) },
      { [AlmKeys.BitbucketServer]: 0, [AlmKeys.GitHub]: 2 }
    )
  ).toMatchSnapshot('invalid configs, admin');
  expect(
    shallowRender(
      { appState: mockAppState({ canAdmin: true }) },
      { [AlmKeys.BitbucketServer]: 0, [AlmKeys.BitbucketCloud]: 0, [AlmKeys.GitHub]: 2 }
    )
  ).toMatchSnapshot('invalid configs, admin');
  expect(
    shallowRender(
      { appState: mockAppState({ canAdmin: true }) },
      {
        [AlmKeys.Azure]: 0,
        [AlmKeys.BitbucketCloud]: 0,
        [AlmKeys.BitbucketServer]: 0,
        [AlmKeys.GitHub]: 0,
        [AlmKeys.GitLab]: 0
      }
    )
  ).toMatchSnapshot('no alm conf yet, admin');
});

it('should correctly pass the selected mode up', () => {
  const onSelectMode = jest.fn();
  let wrapper = shallowRender({ onSelectMode });

  const almButton = 'button.create-project-mode-type-alm';

  click(wrapper.find('button.create-project-mode-type-manual'));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.Manual);
  onSelectMode.mockClear();

  click(wrapper.find(almButton).at(0));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.AzureDevOps);
  onSelectMode.mockClear();

  click(wrapper.find(almButton).at(1));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.BitbucketServer);
  onSelectMode.mockClear();

  click(wrapper.find(almButton).at(2));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.GitHub);
  onSelectMode.mockClear();

  click(wrapper.find(almButton).at(3));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.GitLab);
  onSelectMode.mockClear();

  wrapper = shallowRender(
    { onSelectMode },
    { [AlmKeys.BitbucketCloud]: 1, [AlmKeys.BitbucketServer]: 0 }
  );

  click(wrapper.find(almButton).at(1));
  expect(onSelectMode).toBeCalledWith(CreateProjectModes.BitbucketCloud);
  onSelectMode.mockClear();
});

it('should call the proper click handler', () => {
  const almButton = 'button.create-project-mode-type-alm';

  const onSelectMode = jest.fn();
  const onConfigMode = jest.fn();

  let wrapper = shallowRender({ onSelectMode, onConfigMode }, { [AlmKeys.Azure]: 2 });

  click(wrapper.find(almButton).at(0));
  expect(onConfigMode).not.toHaveBeenCalled();
  expect(onSelectMode).not.toHaveBeenCalled();
  onConfigMode.mockClear();
  onSelectMode.mockClear();

  wrapper = shallowRender({ onSelectMode, onConfigMode });

  click(wrapper.find(almButton).at(0));
  expect(onConfigMode).not.toHaveBeenCalled();
  expect(onSelectMode).toHaveBeenCalledWith(CreateProjectModes.AzureDevOps);
  onConfigMode.mockClear();
  onSelectMode.mockClear();

  wrapper = shallowRender(
    { onSelectMode, onConfigMode, appState: mockAppState({ canAdmin: true }) },
    { [AlmKeys.Azure]: 0 }
  );

  click(wrapper.find(almButton).at(0));
  expect(onConfigMode).toHaveBeenCalledWith(CreateProjectModes.AzureDevOps);
  expect(onSelectMode).not.toHaveBeenCalled();
  onConfigMode.mockClear();
  onSelectMode.mockClear();
});

function shallowRender(
  props: Partial<CreateProjectModeSelectionProps> = {},
  almCountOverrides = {}
) {
  const almCounts = {
    [AlmKeys.Azure]: 1,
    [AlmKeys.BitbucketCloud]: 0,
    [AlmKeys.BitbucketServer]: 1,
    [AlmKeys.GitHub]: 1,
    [AlmKeys.GitLab]: 1,
    ...almCountOverrides
  };
  return shallow<CreateProjectModeSelectionProps>(
    <CreateProjectModeSelection
      almCounts={almCounts}
      appState={mockAppState({ canAdmin: false })}
      loadingBindings={false}
      onSelectMode={jest.fn()}
      onConfigMode={jest.fn()}
      {...props}
    />
  );
}

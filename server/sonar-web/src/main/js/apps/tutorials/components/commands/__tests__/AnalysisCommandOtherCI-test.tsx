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
import { mockComponent, mockLoggedInUser } from '../../../../../helpers/testMocks';
import { ProjectAnalysisModes } from '../../ProjectAnalysisStepFromBuildTool';
import AnalysisCommandOtherCI, {
  AnalysisCommandCommon,
  RenderCommandForClangOrGCC,
  RenderCommandForOther
} from '../AnalysisCommandOtherCI';

const organization = 'org';
const token = '123';

it('should render for Clang or GCC', () => {
  expect(shallowRender('make')).toMatchSnapshot();
});

it('should render for Gradle', () => {
  expect(shallowRender('gradle')).toMatchSnapshot();
});

it('should render for Maven', () => {
  expect(shallowRender('maven')).toMatchSnapshot();
});

it('should render for other', () => {
  expect(shallowRender('other')).toMatchSnapshot();
});

it('should render for unsupported build systems', () => {
  expect(shallowRender('whatever')).toMatchSnapshot();
});

it('should render AnalysisCommandCustom correctly', () => {
  const getBuildOptions = jest.fn().mockResolvedValue(null);
  const wrapper = shallow(
    <AnalysisCommandCommon
      buildType="maven"
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      getBuildOptions={getBuildOptions}
      mode={ProjectAnalysisModes.CI}
      onDone={jest.fn()}
      organization={organization}
      setToken={jest.fn()}
      token={token}
    />
  );

  expect(wrapper).toMatchSnapshot();
  expect(getBuildOptions).toHaveBeenCalled();
});

// TODO make it work
// it('should execute function when the user interacts with the component', async () => {
//   const getBuildOptions = jest.fn().mockReturnValue({
//     maven: RenderCommandForMaven
//   });
//   const onDone = jest.fn();
//   const setToken = jest.fn();
//   const wrapper = shallow(
//     <AnalysisCommandCommon
//       buildType={'maven'}
//       component={mockComponent()}
//       currentUser={mockLoggedInUser()}
//       getBuildOptions={getBuildOptions}
//       mode={ProjectAnalysisModes.CI}
//       onDone={onDone}
//       organization={organization}
//       setToken={setToken}
//       token={token}
//     />
//   );
//
//   (wrapper.find('RenderCommandForMaven').prop('toggleTokenModal') as Function)();
//   await waitAndUpdate(wrapper);
//   (wrapper.find('EditTokenModal').prop('onSave') as Function)();
//
//   expect(setToken).toHaveBeenCalledWith(token);
// });

it('should render RenderCommandForClangOrGCC', () => {
  const render = (token?: string) =>
    shallow(
      <RenderCommandForClangOrGCC
        currentUser={mockLoggedInUser()}
        mode={ProjectAnalysisModes.Custom}
        onDone={jest.fn()}
        toggleModal={jest.fn()}
        token={token}
      />
    );

  expect(render()).toMatchSnapshot();
  expect(render('123')).toMatchSnapshot();
});

it('should render RenderCommandForOther', () => {
  const render = (token?: string) =>
    shallow(
      <RenderCommandForOther
        currentUser={mockLoggedInUser()}
        mode={ProjectAnalysisModes.Custom}
        onDone={jest.fn()}
        toggleModal={jest.fn()}
        token={token}
      />
    );

  expect(render()).toMatchSnapshot();
  expect(render('123')).toMatchSnapshot();
});

function shallowRender(buildType: string) {
  return shallow(
    <AnalysisCommandOtherCI
      buildType={buildType}
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      mode={ProjectAnalysisModes.CI}
      onDone={jest.fn()}
      organization={organization}
      setToken={jest.fn()}
      token={token}
    />
  );
}

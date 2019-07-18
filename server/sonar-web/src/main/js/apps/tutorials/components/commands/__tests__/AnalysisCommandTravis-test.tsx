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
import AnalysisCommandTravis, {
  getSonarcloudAddonYml,
  getSonarcloudAddonYmlRender,
  RenderCommandForClangOrGCC,
  RenderCommandForGradle,
  RenderCommandForMaven,
  RenderCommandForOther,
  RequirementJavaBuild,
  RequirementOtherBuild
} from '../AnalysisCommandTravis';

const component = {
  key: 'foo',
  analysisDate: '2016-01-01',
  breadcrumbs: [],
  name: 'Foo',
  organization: 'org',
  qualifier: 'TRK',
  version: '0.0.1'
};

const organization = 'org';
const token = '123';

it('should render for Clang or GCC', () => {
  expect(
    shallowRender('make')
      .find(RenderCommandForClangOrGCC)
      .exists()
  ).toBeTruthy();
});

it('should render for Gradle', () => {
  expect(
    shallowRender('gradle')
      .find(RenderCommandForGradle)
      .exists()
  ).toBeTruthy();
});

it('should render for Maven', () => {
  expect(
    shallowRender('maven')
      .find(RenderCommandForMaven)
      .exists()
  ).toBeTruthy();
});

it('should render for other', () => {
  expect(
    shallowRender('other')
      .find(RenderCommandForOther)
      .exists()
  ).toBeTruthy();
});

it('should render nothing for unsupported build', () => {
  expect(
    shallowRender()
      .find(RenderCommandForOther)
      .exists()
  ).toBeFalsy();

  expect(
    shallowRender('anotherBuild')
      .find(RenderCommandForOther)
      .exists()
  ).toBeFalsy();
});

it('should render nothing when there is no project key', () => {
  expect(shallow(<RenderCommandForClangOrGCC />).html()).toBe(null);
  expect(shallow(<RenderCommandForGradle />).html()).toBe(null);
  expect(shallow(<RenderCommandForMaven />).html()).toBe(null);
  expect(shallow(<RenderCommandForOther />).html()).toBe(null);
});

it('should render the sonarcloud yaml for travis', () => {
  expect(getSonarcloudAddonYml()).toMatchSnapshot();
  expect(getSonarcloudAddonYml('SonarSource')).toMatchSnapshot();
});

it('should render the sonarcloud yaml for travis', () => {
  expect(getSonarcloudAddonYmlRender()).toMatchSnapshot();
  expect(getSonarcloudAddonYmlRender('SonarSource')).toMatchSnapshot();
});

it('should render the requirements for builds', () => {
  expect(shallow(<RequirementJavaBuild />)).toMatchSnapshot();
  expect(shallow(<RequirementOtherBuild />)).toMatchSnapshot();
});

function shallowRender(buildType?: string) {
  return shallow(
    <AnalysisCommandTravis
      buildType={buildType}
      component={component}
      organization={organization}
      token={token}
    />
  );
}

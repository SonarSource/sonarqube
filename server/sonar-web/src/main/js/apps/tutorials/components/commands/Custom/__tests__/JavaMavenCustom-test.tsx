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
import { ProjectAnalysisModes } from '../../../ProjectAnalysisStepFromBuildTool';
import JavaMavenCustom, { RenderCustomContent } from '../JavaMavenCustom';

const host = 'https://sonarcloud.io';
const organization = 'use-the-force';
const projectKey = 'luke-lightsaber';
const token = 'sonarsource123';

it('should render correctly', () => {
  expect(
    shallow(
      <JavaMavenCustom
        host={host}
        mode={ProjectAnalysisModes.Custom}
        onDone={jest.fn()}
        organization={organization}
        projectKey={projectKey}
        toggleModal={jest.fn()}
        token={token}
      />
    )
  ).toMatchSnapshot();
});

it('should render common elements correctly', () => {
  expect(
    shallow(
      <JavaMavenCustom
        host="sonarcloud"
        mode={ProjectAnalysisModes.Custom}
        onDone={jest.fn()}
        toggleModal={jest.fn()}
        token="123"
      />
    )
  ).toMatchSnapshot();
});

it('should render maven custom content', () => {
  const command = [
    'mvn sonar:sonar',
    projectKey && `-Dsonar.projectKey=${projectKey}`,
    organization && `-Dsonar.organization=${organization}`,
    `-Dsonar.host.url=${host}`,
    `-Dsonar.login=${token}`
  ];
  const onDone = jest.fn();
  const toggleModal = jest.fn();

  expect(
    <RenderCustomContent
      command={command}
      linkText="onboarding.analysis.java.maven.docs_link"
      linkUrl="http://redirect.sonarsource.com/doc/install-configure-scanner-maven.html"
      onDone={onDone}
      toggleModal={toggleModal}
    />
  ).toMatchSnapshot();
});

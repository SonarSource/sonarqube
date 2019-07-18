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
import ClangGCCCustom, { ClangGCCCommon } from '../ClangGCCCustom';

it('should render correctly', () => {
  expect(
    shallow(
      <ClangGCCCustom
        host="https://sonarcloud.io"
        mode={ProjectAnalysisModes.Custom}
        onDone={jest.fn()}
        organization="use-the-force"
        os="linux"
        projectKey="luke-lightsaber"
        small={true}
        toggleModal={jest.fn()}
        token="sonarsource123"
      />
    )
  ).toMatchSnapshot();
});

it('should render common elements correctly', () => {
  const command1 = `command1`;
  const command2 = [`command2`];
  const renderCommand2 = () => <>render command 2</>;
  expect(
    shallow(
      <ClangGCCCommon
        command1={command1}
        command2={command2}
        renderCommand2={renderCommand2}
        onDone={jest.fn()}
        os="linux"
      />
    )
  ).toMatchSnapshot();
});

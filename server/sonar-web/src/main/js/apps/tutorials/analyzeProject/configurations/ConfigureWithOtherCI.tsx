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
import { translate } from 'sonar-ui-common/helpers/l10n';
import ProjectAnalysisStepFromBuildTool, {
  ProjectAnalysisModes
} from '../../components/ProjectAnalysisStepFromBuildTool';
import { TutorialProps } from '../utils';

export default function ConfigureWithOtherCI({
  component,
  currentUser,
  onDone,
  setToken,
  token
}: TutorialProps) {
  return (
    <>
      <h1 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.yourci.title')}
      </h1>

      <ProjectAnalysisStepFromBuildTool
        component={component}
        currentUser={currentUser}
        displayRowLayout={true}
        mode={ProjectAnalysisModes.CI}
        onDone={onDone}
        open={true}
        organization={component.organization}
        setToken={setToken}
        stepNumber={1}
        token={token}
      />
    </>
  );
}

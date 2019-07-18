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
import { get, save } from 'sonar-ui-common/helpers/storage';
import { PROJECT_STEP_PROGRESS } from '../analyzeProject/utils';
import BuildSystemForm from './BuildSystemForm';
import AnalysisCommandCustom from './commands/AnalysisCommandCustom';
import AnalysisCommandOtherCI from './commands/AnalysisCommandOtherCI';
import Step from './Step';

export enum ProjectAnalysisModes {
  CI = 'CI',
  Custom = 'Custom'
}

export interface Props {
  component: T.Component;
  currentUser: T.LoggedInUser;
  displayRowLayout?: boolean;
  mode: ProjectAnalysisModes;
  onDone: VoidFunction;
  onReset?: VoidFunction;
  open: boolean;
  organization?: string;
  setToken: (token: string) => void;
  stepNumber: number;
  token?: string;
}

export default function ProjectAnalysisStepFromBuildTool({
  component,
  currentUser,
  displayRowLayout,
  mode,
  onDone,
  open,
  organization,
  setToken,
  stepNumber,
  token
}: Props) {
  const [build, setBuild] = React.useState<string | undefined>(undefined);
  const [os, setOS] = React.useState<string | undefined>(undefined);

  React.useEffect(() => {
    const value = get(PROJECT_STEP_PROGRESS, component.key);
    if (value) {
      try {
        const data = JSON.parse(value);
        setBuild(data.build);
        setOS(data.os);
      } catch (e) {
        // Let's start from scratch
      }
    }
  }, [component.key]);

  const saveAndFinish = (data: object) => {
    save(
      PROJECT_STEP_PROGRESS,
      JSON.stringify({
        ...data,
        build
      }),
      component.key
    );

    onDone();
  };

  const renderForm = () => {
    const languageComponent = <BuildSystemForm build={build} setBuild={setBuild} />;

    let AnalysisComponent = null;

    if (mode === ProjectAnalysisModes.Custom) {
      AnalysisComponent = AnalysisCommandCustom;
    } else if (mode === ProjectAnalysisModes.CI) {
      AnalysisComponent = AnalysisCommandOtherCI;
    }

    if (displayRowLayout) {
      return (
        <div className="boxed-group-inner">
          <div className="display-flex-column">
            {languageComponent}
            {AnalysisComponent && (
              <div className="huge-spacer-top">
                <AnalysisComponent
                  buildType={build}
                  component={component}
                  currentUser={currentUser}
                  onDone={saveAndFinish}
                  organization={organization}
                  os={os}
                  setToken={setToken}
                  small={true}
                  token={token}
                />
              </div>
            )}
          </div>
        </div>
      );
    }

    return (
      <div className="boxed-group-inner">
        <div className="flex-columns">
          <div className="flex-column flex-column-half bordered-right">{languageComponent}</div>
          <div className="flex-column flex-column-half">{AnalysisComponent}</div>
        </div>
      </div>
    );
  };

  const renderResult = () => null;

  return (
    <Step
      finished={false}
      onOpen={() => {}}
      open={open}
      renderForm={renderForm}
      renderResult={renderResult}
      stepNumber={stepNumber}
      stepTitle={translate('onboarding.analysis.header')}
    />
  );
}

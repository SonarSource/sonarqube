/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import Step from '../components/Step';
import BranchAnalysisStepContent from './BranchAnalysisStepContent';
import ExtensionInstallationStepContent from './ExtensionInstallationStepContent';
import SaveAndRunStepContent from './SaveAndRunStepContent';
import ServiceEndpointStepContent from './ServiceEndpointStepContent';

export interface AzurePipelinesTutorialProps {
  alm?: AlmKeys;
  baseUrl: string;
  component: T.Component;
  currentUser: T.LoggedInUser;
}

export enum Steps {
  ExtensionInstallation,
  ServiceEndpoint,
  BranchAnalysis,
  SaveAndRun
}

interface Step {
  step: Steps;
  content: JSX.Element;
  checkValidity?: boolean;
}

export default function AzurePipelinesTutorial(props: AzurePipelinesTutorialProps) {
  const { alm, baseUrl, component, currentUser } = props;

  const [currentStep, setCurrentStep] = React.useState(Steps.ExtensionInstallation);
  const [isCurrentStepValid, setIsCurrentStepValid] = React.useState(false);

  const steps: Array<Step> = [
    { step: Steps.ExtensionInstallation, content: <ExtensionInstallationStepContent /> },
    {
      step: Steps.ServiceEndpoint,
      content: (
        <ServiceEndpointStepContent
          baseUrl={baseUrl}
          component={component}
          currentUser={currentUser}
        />
      )
    },
    {
      step: Steps.BranchAnalysis,
      content: (
        <BranchAnalysisStepContent
          component={component}
          onStepValidationChange={isValid => setIsCurrentStepValid(isValid)}
        />
      ),
      checkValidity: true
    },
    { step: Steps.SaveAndRun, content: <SaveAndRunStepContent alm={alm} /> }
  ];

  const switchCurrentStep = (step: Steps) => {
    setCurrentStep(step);
    setIsCurrentStepValid(false);
  };

  const canContinue = (step: Step, i: number) =>
    i < steps.length - 1 && (!step.checkValidity || isCurrentStepValid);

  return (
    <>
      <div className="page-header big-spacer-bottom">
        <h1 className="page-title">
          {translate('onboarding.tutorial.with.azure_pipelines.title')}
        </h1>
      </div>

      {steps.map((step, i) => (
        <Step
          key={step.step}
          stepNumber={i + 1}
          stepTitle={translate(
            `onboarding.tutorial.with.azure_pipelines.${Steps[step.step]}.title`
          )}
          open={step.step === currentStep}
          finished={step.step < currentStep}
          onOpen={() => switchCurrentStep(step.step)}
          renderForm={() => (
            <div className="boxed-group-inner">
              <div>{step.content}</div>
              {canContinue(step, i) && (
                <Button
                  className="big-spacer-top spacer-bottom"
                  onClick={() => switchCurrentStep(step.step + 1)}>
                  {translate('continue')}
                </Button>
              )}
            </div>
          )}
        />
      ))}
    </>
  );
}

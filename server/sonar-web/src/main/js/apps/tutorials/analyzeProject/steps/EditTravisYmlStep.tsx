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
import { FormattedMessage } from 'react-intl';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import BuildSystemForm from '../../components/BuildSystemForm';
import AnalysisCommandTravis from '../../components/commands/AnalysisCommandTravis';
import Step from '../../components/Step';
import { StepProps } from '../../utils';

interface BuildProps {
  buildCallback: (build: string) => void;
  buildType?: string;
}

export default function EditTravisYmlStep({
  buildCallback,
  buildType,
  component,
  finished,
  hasStepAfter,
  onContinue,
  onOpen,
  open,
  organization,
  stepNumber,
  token
}: StepProps & BuildProps) {
  const [build, setBuild] = React.useState<string | undefined>(buildType || undefined);

  if (!build && buildType) {
    setBuild(buildType);
  }

  const isJavaBuild = build && ['gradle', 'maven'].includes(build);

  if (hasStepAfter && build) {
    hasStepAfter(!isJavaBuild);
  }

  const chooseBuild = (build: string) => {
    buildCallback(build);
    setBuild(build);
  };

  const renderForm = () => (
    <div className="boxed-group-inner">
      <div className="flex-columns">
        <div className="flex-column-full">
          <BuildSystemForm build={build} setBuild={chooseBuild} />

          {build && (
            <AnalysisCommandTravis
              buildType={build}
              component={component}
              organization={organization}
              token={token}
            />
          )}
        </div>
      </div>
      <div className="big-spacer-top">
        {build && (
          <Button className="js-continue" onClick={onContinue}>
            {translate(isJavaBuild ? 'onboarding.finish' : 'continue')}
          </Button>
        )}
      </div>
    </div>
  );

  const renderResult = () => null;

  return (
    <Step
      finished={Boolean(finished)}
      onOpen={onOpen}
      open={open}
      renderForm={renderForm}
      renderResult={renderResult}
      stepNumber={stepNumber}
      stepTitle={
        <FormattedMessage
          defaultMessage={translate('onboarding.analysis.travis.sonarcloud')}
          id="onboarding.analysis.travis.sonarcloud"
          values={{
            filename: <code className="rule">.travis.yml</code>
          }}
        />
      }
    />
  );
}

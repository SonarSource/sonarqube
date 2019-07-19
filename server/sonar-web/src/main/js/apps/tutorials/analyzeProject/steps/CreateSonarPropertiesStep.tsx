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
import CodeSnippet from '../../../../components/common/CodeSnippet';
import Step from '../../components/Step';
import { StepProps } from '../../utils';

export default function CreateSonarPropertiesStep({
  component,
  finished,
  onContinue,
  onOpen,
  open,
  stepNumber
}: StepProps) {
  const command = `sonar.projectKey=${component ? component.key : 'my:project'}
# this is the name and version displayed in the SonarCloud UI.
sonar.projectName=${component ? component.name : 'My project'}
sonar.projectVersion=1.0
 
# Path is relative to the sonar-project.properties file. Replace "\\" by "/" on Windows.
# This property is optional if sonar.modules is set. 
sonar.sources=.
 
# Encoding of the source code. Default is default system encoding
#sonar.sourceEncoding=UTF-8`;

  const renderForm = () => (
    <div className="boxed-group-inner">
      <div className="flex-columns">
        <div className="flex-column-full">
          <p>
            <FormattedMessage
              defaultMessage={translate('onboarding.analysis.with.travis.sonar.properties.text')}
              id="onboarding.analysis.with.travis.sonar.properties.text"
              values={{
                code: <code>sonar-project.properties</code>
              }}
            />
          </p>
          <CodeSnippet snippet={command} />
        </div>
      </div>
      <div className="big-spacer-top">
        <Button className="js-continue" onClick={onContinue}>
          {translate('onboarding.finish')}
        </Button>
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
          defaultMessage={translate('onboarding.analysis.with.travis.sonar.properties.title')}
          id="onboarding.analysis.with.travis.sonar.properties.title"
          values={{
            filename: <code className="rule">sonar-project.properties</code>
          }}
        />
      }
    />
  );
}

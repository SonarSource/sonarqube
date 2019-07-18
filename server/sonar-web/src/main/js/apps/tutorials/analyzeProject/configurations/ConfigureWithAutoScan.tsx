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
import { Button, ResetButtonLink } from 'sonar-ui-common/components/controls/buttons';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../components/common/CodeSnippet';
import Step from '../../components/Step';
import { TutorialProps } from '../utils';
import { AutoScanAlert } from './AutoScanAlert';

export default function ConfigureWithAutoScan({ onDone }: TutorialProps) {
  const [showCustomizationOptions, setCustomizationOptions] = React.useState<boolean>(false);

  const command = `# Path to sources
#sonar.sources=.
#sonar.exclusions=
#sonar.inclusions=

# Path to tests
#sonar.tests=
#sonar.test.exclusions=
#sonar.test.inclusions=

# Source encoding
#sonar.sourceEncoding=UTF-8

# Exclusions for copy-paste detection
#sonar.cpd.exclusions=`;

  const renderForm = () => (
    <div className="boxed-group-inner">
      <div className="flex-columns">
        <div className="flex-column-full">
          <p className="spacer-bottom">
            Add this file in the base directory of your default branch or on a PR.
          </p>
          <p className="spacer-bottom">
            You can push an empty <code>.sonarcloud.properties</code> file, this will work fine. In
            this case, every file in the repository will be considered as a source file.
          </p>

          <ResetButtonLink onClick={() => setCustomizationOptions(!showCustomizationOptions)}>
            {showCustomizationOptions ? 'Hide customization options' : 'Show customization options'}
            <DropdownIcon className="little-spacer-left" turned={showCustomizationOptions} />
          </ResetButtonLink>

          <div hidden={!showCustomizationOptions}>
            <p>
              Here are the supported optional settings for the <code>.sonarcloud.properties</code>{' '}
              file:
            </p>

            <CodeSnippet snippet={command} />

            <p>
              Please refer to the{' '}
              <a
                href="https://sieg.eu.ngrok.io/documentation/autoscan/"
                rel="noopener noreferrer"
                target="_blank">
                documentation
              </a>{' '}
              for more details.
            </p>
          </div>
        </div>
      </div>
      <div className="big-spacer-top">
        <Button className="js-continue" onClick={onDone}>
          {translate('onboarding.finish')}
        </Button>
      </div>
    </div>
  );

  const renderResult = () => null;

  return (
    <>
      <h1 className="spacer-bottom spacer-top">
        {translate('onboarding.analysis.with.autoscan.title')}
      </h1>

      <p className="spacer-bottom">{translate('onboarding.analysis.with.autoscan.text')}</p>

      <AutoScanAlert />

      <Step
        finished={false}
        onOpen={() => {}}
        open={true}
        renderForm={renderForm}
        renderResult={renderResult}
        stepNumber={1}
        stepTitle={
          <FormattedMessage
            defaultMessage={translate('onboarding.analysis.with.autoscan.filename')}
            id="onboarding.analysis.with.autoscan.filename"
            values={{
              filename: <code className="rule">.sonarcloud.properties</code>
            }}
          />
        }
      />
    </>
  );
}

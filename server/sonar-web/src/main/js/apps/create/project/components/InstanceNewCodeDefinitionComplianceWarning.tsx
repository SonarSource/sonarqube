/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { getNewCodePeriod } from '../../../../api/newCodePeriod';
import { AppStateContextProviderProps } from '../../../../app/components/app-state/AppStateContextProvider';
import withAppStateContext from '../../../../app/components/app-state/withAppStateContext';
import DocLink from '../../../../components/common/DocLink';
import Link from '../../../../components/common/Link';
import { Alert } from '../../../../components/ui/Alert';
import { translate } from '../../../../helpers/l10n';
import { isNewCodeDefinitionCompliant } from '../../../../helpers/periods';

export type InstanceNewCodeDefinitionComplianceWarningProps = AppStateContextProviderProps;

export function InstanceNewCodeDefinitionComplianceWarning({
  appState: { canAdmin },
}: InstanceNewCodeDefinitionComplianceWarningProps) {
  const [isCompliant, setIsCompliant] = React.useState(true);

  React.useEffect(() => {
    async function fetchInstanceNCDOptionCompliance() {
      const newCodeDefinition = await getNewCodePeriod();
      setIsCompliant(isNewCodeDefinitionCompliant(newCodeDefinition));
    }

    fetchInstanceNCDOptionCompliance();
  }, []);

  if (isCompliant) {
    return null;
  }

  return (
    <Alert className="huge-spacer-bottom sw-max-w-[700px]" variant="warning">
      <p className="sw-mb-2 sw-font-bold">
        {translate('onboarding.create_project.new_code_option.warning.title')}
      </p>
      <p className="sw-mb-2">
        <FormattedMessage
          id="onboarding.create_project.new_code_option.warning.explanation"
          defaultMessage={translate(
            'onboarding.create_project.new_code_option.warning.explanation'
          )}
          values={{
            action: canAdmin ? (
              <FormattedMessage
                id="onboarding.create_project.new_code_option.warning.explanation.action.admin"
                defaultMessage={translate(
                  'onboarding.create_project.new_code_option.warning.explanation.action.admin'
                )}
                values={{
                  link: (
                    <Link to="/admin/settings?category=new_code_period">
                      {translate(
                        'onboarding.create_project.new_code_option.warning.explanation.action.admin.link'
                      )}
                    </Link>
                  ),
                }}
              />
            ) : (
              translate('onboarding.create_project.new_code_option.warning.explanation.action')
            ),
          }}
        />
      </p>
      <p>
        {translate('learn_more')}:&nbsp;
        <DocLink to="/project-administration/defining-new-code/">
          {translate('onboarding.create_project.new_code_option.warning.learn_more.link')}
        </DocLink>
      </p>
    </Alert>
  );
}

export default withAppStateContext(InstanceNewCodeDefinitionComplianceWarning);

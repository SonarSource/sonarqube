/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Link } from 'react-router';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import {
  createBitbucketConfiguration,
  updateBitbucketConfiguration
} from '../../../../api/alm-settings';
import { AlmKeys, BitbucketBindingDefinition } from '../../../../types/alm-settings';
import AlmTab from './AlmTab';
import BitbucketForm from './BitbucketForm';

export interface BitbucketTabProps {
  definitions: BitbucketBindingDefinition[];
  loading: boolean;
  multipleAlmEnabled: boolean;
  onDelete: (definitionKey: string) => void;
  onUpdateDefinitions: () => void;
}

export default function BitbucketTab(props: BitbucketTabProps) {
  const { multipleAlmEnabled, definitions, loading } = props;

  return (
    <div className="bordered">
      <AlmTab
        additionalColumnsHeaders={[translate('settings.almintegration.table.column.bitbucket.url')]}
        additionalColumnsKeys={['url']}
        additionalTableInfo={
          <Alert className="big-spacer-bottom width-50" variant="info">
            <FormattedMessage
              defaultMessage={translate(
                'settings.almintegration.feature.alm_repo_import.disabled_if_multiple_bbs_instances'
              )}
              id="settings.almintegration.feature.alm_repo_import.disabled_if_multiple_bbs_instances"
              values={{
                feature: (
                  <em>{translate('settings.almintegration.feature.alm_repo_import.title')}</em>
                )
              }}
            />
          </Alert>
        }
        alm={AlmKeys.Bitbucket}
        createConfiguration={createBitbucketConfiguration}
        defaultBinding={{ key: '', url: '', personalAccessToken: '' }}
        definitions={definitions}
        features={[
          {
            name: translate('settings.almintegration.feature.pr_decoration.title'),
            active: definitions.length > 0,
            description: translate('settings.almintegration.feature.pr_decoration.description'),
            inactiveReason: translate('settings.almintegration.feature.need_at_least_1_binding')
          },
          {
            name: translate('settings.almintegration.feature.alm_repo_import.title'),
            active: definitions.length === 1,
            description: translate('settings.almintegration.feature.alm_repo_import.description'),
            inactiveReason: translateWithParameters(
              'onboarding.create_project.too_many_bbs_instances_X',
              definitions.length
            )
          }
        ]}
        form={childProps => <BitbucketForm {...childProps} />}
        help={
          <>
            <h3>{translate('onboarding.create_project.pat_help.title')}</h3>

            <p className="big-spacer-top">
              {translate('settings.almintegration.bitbucket.help_1')}
            </p>

            <ul className="big-spacer-top list-styled">
              <li>{translate('settings.almintegration.bitbucket.help_2')}</li>
              <li>{translate('settings.almintegration.bitbucket.help_3')}</li>
            </ul>

            <p className="big-spacer-top big-spacer-bottom">
              <Link target="_blank" to="/documentation/analysis/pr-decoration/">
                {translate('learn_more')}
              </Link>
            </p>
          </>
        }
        loading={loading}
        multipleAlmEnabled={multipleAlmEnabled}
        onDelete={props.onDelete}
        onUpdateDefinitions={props.onUpdateDefinitions}
        updateConfiguration={updateBitbucketConfiguration}
      />
    </div>
  );
}

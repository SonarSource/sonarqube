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
import { Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  createBitbucketConfiguration,
  updateBitbucketConfiguration
} from '../../../../api/alm-settings';
import {
  AlmKeys,
  AlmSettingsBindingStatus,
  BitbucketBindingDefinition
} from '../../../../types/alm-settings';
import AlmTab from './AlmTab';
import BitbucketForm from './BitbucketForm';

export interface BitbucketTabProps {
  definitions: BitbucketBindingDefinition[];
  definitionStatus: T.Dict<AlmSettingsBindingStatus>;
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCheck: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onUpdateDefinitions: () => void;
}

export default function BitbucketTab(props: BitbucketTabProps) {
  const {
    multipleAlmEnabled,
    definitions,
    definitionStatus,
    loadingAlmDefinitions,
    loadingProjectCount
  } = props;

  return (
    <div className="bordered">
      <AlmTab
        alm={AlmKeys.Bitbucket}
        createConfiguration={createBitbucketConfiguration}
        defaultBinding={{ key: '', url: '', personalAccessToken: '' }}
        definitions={definitions}
        definitionStatus={definitionStatus}
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
        loadingAlmDefinitions={loadingAlmDefinitions}
        loadingProjectCount={loadingProjectCount}
        multipleAlmEnabled={multipleAlmEnabled}
        onCheck={props.onCheck}
        onDelete={props.onDelete}
        onUpdateDefinitions={props.onUpdateDefinitions}
        updateConfiguration={updateBitbucketConfiguration}
      />
    </div>
  );
}

/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_DOCUMENTATION_PATHS } from '../../../../helpers/constants';
import {
  AlmKeys,
  AlmSettingsBindingStatus,
  BitbucketBindingDefinition,
  BitbucketCloudBindingDefinition
} from '../../../../types/alm-settings';
import AlmTabRenderer from './AlmTabRenderer';
import BitbucketForm from './BitbucketForm';

export interface BitbucketTabRendererProps {
  branchesEnabled: boolean;
  definitionStatus: T.Dict<AlmSettingsBindingStatus>;
  editedDefinition?: BitbucketBindingDefinition | BitbucketCloudBindingDefinition;
  definitions: Array<BitbucketBindingDefinition | BitbucketCloudBindingDefinition>;
  isCreating: boolean;
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCancel: () => void;
  onCheck: (definitionKey: string) => void;
  onCreate: () => void;
  onDelete: (definitionKey: string) => void;
  onEdit: (definitionKey: string) => void;
  onSelectVariant: (variant: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud) => void;
  onSubmit: (
    config: BitbucketBindingDefinition | BitbucketCloudBindingDefinition,
    originalKey: string
  ) => void;
  submitting: boolean;
  success: boolean;
  variant?: AlmKeys.BitbucketServer | AlmKeys.BitbucketCloud;
}

export default function BitbucketTabRenderer(props: BitbucketTabRendererProps) {
  const {
    branchesEnabled,
    editedDefinition,
    definitions,
    definitionStatus,
    isCreating,
    loadingAlmDefinitions,
    loadingProjectCount,
    multipleAlmEnabled,
    submitting,
    success,
    variant
  } = props;

  let help;
  if (variant === AlmKeys.BitbucketServer) {
    help = (
      <>
        <h3>{translate('onboarding.create_project.pat_help.title')}</h3>

        <p className="big-spacer-top">{translate('settings.almintegration.bitbucket.help_1')}</p>

        <ul className="big-spacer-top list-styled">
          <li>{translate('settings.almintegration.bitbucket.help_2')}</li>
          <li>{translate('settings.almintegration.bitbucket.help_3')}</li>
        </ul>

        <p className="big-spacer-top big-spacer-bottom">
          <Link target="_blank" to={ALM_DOCUMENTATION_PATHS[AlmKeys.BitbucketServer]}>
            {translate('learn_more')}
          </Link>
        </p>
      </>
    );
  } else if (variant === AlmKeys.BitbucketCloud) {
    help = (
      <FormattedMessage
        defaultMessage={translate(`settings.almintegration.bitbucketcloud.info`)}
        id="settings.almintegration.bitbucketcloud.info"
        values={{
          link: (
            <Link target="_blank" to={ALM_DOCUMENTATION_PATHS[AlmKeys.BitbucketCloud]}>
              {translate('learn_more')}
            </Link>
          )
        }}
      />
    );
  }

  return (
    <div className="bordered">
      <AlmTabRenderer
        branchesEnabled={branchesEnabled}
        alm={AlmKeys.BitbucketServer} // Always use Bitbucket Server for the translation keys.
        definitions={definitions}
        definitionStatus={definitionStatus}
        editedDefinition={editedDefinition}
        form={childProps => (
          <BitbucketForm
            isCreating={isCreating}
            onSelectVariant={props.onSelectVariant}
            variant={variant}
            {...childProps}
          />
        )}
        help={help}
        loadingAlmDefinitions={loadingAlmDefinitions}
        loadingProjectCount={loadingProjectCount}
        multipleAlmEnabled={multipleAlmEnabled}
        onCancel={props.onCancel}
        onCheck={props.onCheck}
        onCreate={props.onCreate}
        onDelete={props.onDelete}
        onEdit={props.onEdit}
        onSubmit={props.onSubmit}
        submitting={submitting}
        success={success}
      />
    </div>
  );
}

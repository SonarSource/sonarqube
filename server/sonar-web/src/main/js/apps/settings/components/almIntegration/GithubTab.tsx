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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { createGithubConfiguration, updateGithubConfiguration } from '../../../../api/alm-settings';
import { AlmKeys, GithubBindingDefinition } from '../../../../types/alm-settings';
import { ALM_INTEGRATION } from '../AdditionalCategoryKeys';
import CategoryDefinitionsList from '../CategoryDefinitionsList';
import AlmTab from './AlmTab';
import GithubForm from './GithubForm';

export interface GithubTabProps {
  branchesEnabled: boolean;
  component?: T.Component;
  definitions: GithubBindingDefinition[];
  loading: boolean;
  multipleAlmEnabled: boolean;
  onDelete: (definitionKey: string) => void;
  onUpdateDefinitions: () => void;
}

export default function GithubTab(props: GithubTabProps) {
  const { branchesEnabled, component, multipleAlmEnabled, definitions, loading } = props;

  return (
    <div className="bordered">
      {branchesEnabled && (
        <>
          <AlmTab
            additionalColumnsHeaders={[
              translate('settings.almintegration.table.column.github.url'),
              translate('settings.almintegration.table.column.app_id')
            ]}
            additionalColumnsKeys={['appId', 'url']}
            alm={AlmKeys.GitHub}
            createConfiguration={createGithubConfiguration}
            defaultBinding={{ key: '', appId: '', url: '', privateKey: '' }}
            definitions={definitions}
            features={[
              {
                name: translate('settings.almintegration.feature.pr_decoration.title'),
                active: definitions.length > 0,
                description: translate('settings.almintegration.feature.pr_decoration.description'),
                inactiveReason: translate('settings.almintegration.feature.need_at_least_1_binding')
              }
            ]}
            form={childProps => <GithubForm {...childProps} />}
            loading={loading}
            multipleAlmEnabled={multipleAlmEnabled}
            onDelete={props.onDelete}
            onUpdateDefinitions={props.onUpdateDefinitions}
            updateConfiguration={updateGithubConfiguration}
          />

          <div className="huge-spacer-top huge-spacer-bottom bordered-top" />
        </>
      )}

      <div className="big-padded">
        <CategoryDefinitionsList
          category={ALM_INTEGRATION}
          component={component}
          subCategory={AlmKeys.GitHub}
        />
      </div>
    </div>
  );
}

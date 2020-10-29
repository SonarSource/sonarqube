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
import { createGitlabConfiguration, updateGitlabConfiguration } from '../../../../api/alm-settings';
import {
  AlmKeys,
  AlmSettingsBindingStatus,
  GitlabBindingDefinition
} from '../../../../types/alm-settings';
import { ALM_INTEGRATION } from '../AdditionalCategoryKeys';
import CategoryDefinitionsList from '../CategoryDefinitionsList';
import AlmTab from './AlmTab';
import GitlabForm from './GitlabForm';

export interface GitlabTabProps {
  branchesEnabled: boolean;
  component?: T.Component;
  definitions: GitlabBindingDefinition[];
  definitionStatus: T.Dict<AlmSettingsBindingStatus>;
  loadingAlmDefinitions: boolean;
  loadingProjectCount: boolean;
  multipleAlmEnabled: boolean;
  onCheck: (definitionKey: string) => void;
  onDelete: (definitionKey: string) => void;
  onUpdateDefinitions: () => void;
}

export default function GitlabTab(props: GitlabTabProps) {
  const {
    branchesEnabled,
    component,
    multipleAlmEnabled,
    definitions,
    definitionStatus,
    loadingAlmDefinitions,
    loadingProjectCount
  } = props;

  return (
    <div className="bordered">
      {branchesEnabled && (
        <>
          <AlmTab
            alm={AlmKeys.GitLab}
            createConfiguration={createGitlabConfiguration}
            defaultBinding={{ key: '', personalAccessToken: '', url: '' }}
            definitions={definitions}
            definitionStatus={definitionStatus}
            form={childProps => <GitlabForm {...childProps} />}
            loadingAlmDefinitions={loadingAlmDefinitions}
            loadingProjectCount={loadingProjectCount}
            multipleAlmEnabled={multipleAlmEnabled}
            onCheck={props.onCheck}
            onDelete={props.onDelete}
            onUpdateDefinitions={props.onUpdateDefinitions}
            updateConfiguration={updateGitlabConfiguration}
          />

          <div className="huge-spacer-top huge-spacer-bottom bordered-top" />
        </>
      )}

      <div className="big-padded">
        <CategoryDefinitionsList
          category={ALM_INTEGRATION}
          component={component}
          subCategory={AlmKeys.GitLab}
        />
      </div>
    </div>
  );
}

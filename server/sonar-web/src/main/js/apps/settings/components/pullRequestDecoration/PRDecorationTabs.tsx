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
import BoxedTabs from 'sonar-ui-common/components/controls/BoxedTabs';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { almName, ALM_KEYS } from '../../utils';
import GithubTab from './GithubTab';

export interface PRDecorationTabsProps {
  currentAlm: ALM_KEYS;
  definitions: T.AlmSettingsBindingDefinitions;
  loading: boolean;
  onSelectAlm: (alm: ALM_KEYS) => void;
  onUpdateDefinitions: () => void;
}

export default function PRDecorationTabs(props: PRDecorationTabsProps) {
  const { definitions, currentAlm, loading } = props;

  if (loading) {
    return <DeferredSpinner />;
  }

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">{translate('settings.pr_decoration.title')}</h1>
      </header>

      <div className="markdown small spacer-top big-spacer-bottom">
        {translate('settings.pr_decoration.description')}
      </div>
      <BoxedTabs
        onSelect={props.onSelectAlm}
        selected={currentAlm}
        tabs={[
          {
            key: ALM_KEYS.GITHUB,
            label: almName[ALM_KEYS.GITHUB]
          }
        ]}
      />

      <div className="boxed-group boxed-group-inner">
        <GithubTab
          definitions={definitions.github}
          onUpdateDefinitions={props.onUpdateDefinitions}
        />
      </div>
    </>
  );
}

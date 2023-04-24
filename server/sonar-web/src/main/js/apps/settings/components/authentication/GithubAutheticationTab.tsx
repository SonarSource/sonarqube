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
import React from 'react';
import { setSettingValue } from '../../../../api/settings';
import { Button } from '../../../../components/controls/buttons';
import CheckIcon from '../../../../components/icons/CheckIcon';
import DeleteIcon from '../../../../components/icons/DeleteIcon';
import EditIcon from '../../../../components/icons/EditIcon';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys } from '../../../../types/alm-settings';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import ConfigurationForm from './ConfigurationForm';
import useGithubConfiguration, { GITHUB_ENABLED_FIELD } from './hook/useGithubConfiguration';

interface SamlAuthenticationProps {
  definitions: ExtendedSettingDefinition[];
}

const GITHUB_EXCLUDED_FIELD = [
  'sonar.auth.github.enabled',
  'sonar.auth.github.groupsSync',
  'sonar.auth.github.allowUsersToSignUp',
];

export default function GithubAithentication(props: SamlAuthenticationProps) {
  const [showEditModal, setShowEditModal] = React.useState(false);
  const {
    hasConfiguration,
    loading,
    values,
    setNewValue,
    canBeSave,
    reload,
    url,
    appId,
    enabled,
    deleteConfiguration,
  } = useGithubConfiguration(props.definitions);

  const handleCreateConfiguration = () => {
    setShowEditModal(true);
  };

  const handleCancelConfiguration = () => {
    setShowEditModal(false);
  };

  const handleToggleEnable = async () => {
    const value = values[GITHUB_ENABLED_FIELD];
    await setSettingValue(value.definition, !enabled);
    await reload();
  };

  return (
    <div className="saml-configuration">
      <div className="spacer-bottom display-flex-space-between display-flex-center">
        <h4>{translate('settings.authentication.github.configuration')}</h4>

        {!hasConfiguration && (
          <div>
            <Button onClick={handleCreateConfiguration}>
              {translate('settings.authentication.form.create')}
            </Button>
          </div>
        )}
      </div>
      {!hasConfiguration ? (
        <div className="big-padded text-center huge-spacer-bottom saml-no-config">
          {translate('settings.authentication.github.form.not_configured')}
        </div>
      ) : (
        <>
          <div className="spacer-bottom big-padded bordered display-flex-space-between">
            <div>
              <h5>{appId}</h5>
              <p>{url}</p>
              <p className="big-spacer-top big-spacer-bottom">
                {enabled ? (
                  <span className="saml-enabled spacer-left">
                    <CheckIcon className="spacer-right" />
                    {translate('settings.authentication.saml.form.enabled')}
                  </span>
                ) : (
                  translate('settings.authentication.saml.form.not_enabled')
                )}
              </p>
              <Button className="spacer-top" onClick={handleToggleEnable}>
                {enabled
                  ? translate('settings.authentication.saml.form.disable')
                  : translate('settings.authentication.saml.form.enable')}
              </Button>
            </div>
            <div>
              <Button className="spacer-right" onClick={handleCreateConfiguration}>
                <EditIcon />
                {translate('settings.authentication.form.edit')}
              </Button>
              <Button className="button-red" disabled={enabled} onClick={deleteConfiguration}>
                <DeleteIcon />
                {translate('settings.authentication.form.delete')}
              </Button>
            </div>
          </div>
          <div className="spacer-bottom big-padded bordered display-flex-space-between">
            Provisioning TODO
          </div>
        </>
      )}

      {showEditModal && (
        <ConfigurationForm
          tab={AlmKeys.GitHub}
          excludedField={GITHUB_EXCLUDED_FIELD}
          loading={loading}
          values={values}
          setNewValue={setNewValue}
          canBeSave={canBeSave}
          onClose={handleCancelConfiguration}
          create={!hasConfiguration}
          onReload={reload}
        />
      )}
    </div>
  );
}

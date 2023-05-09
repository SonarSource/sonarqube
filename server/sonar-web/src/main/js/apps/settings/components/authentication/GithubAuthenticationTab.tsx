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
import { isEmpty } from 'lodash';
import React, { useState } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  activateGithubProvisioning,
  deactivateGithubProvisioning,
  resetSettingValue,
  setSettingValue,
} from '../../../../api/settings';
import GitHubSynchronisationWarning from '../../../../app/components/GitHubSynchronisationWarning';
import DocLink from '../../../../components/common/DocLink';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import RadioCard from '../../../../components/controls/RadioCard';
import { Button, ResetButtonLink, SubmitButton } from '../../../../components/controls/buttons';
import { Provider } from '../../../../components/hooks/useManageProvider';
import CheckIcon from '../../../../components/icons/CheckIcon';
import DeleteIcon from '../../../../components/icons/DeleteIcon';
import EditIcon from '../../../../components/icons/EditIcon';
import { Alert } from '../../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { AlmKeys } from '../../../../types/alm-settings';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { DOCUMENTATION_LINK_SUFFIXES } from './Authentication';
import AuthenticationFormField from './AuthenticationFormField';
import ConfigurationForm from './ConfigurationForm';
import useGithubConfiguration, {
  GITHUB_ENABLED_FIELD,
  GITHUB_JIT_FIELDS,
} from './hook/useGithubConfiguration';

interface GithubAuthenticationProps {
  definitions: ExtendedSettingDefinition[];
  provider: string | undefined;
  onReload: () => void;
}

const GITHUB_EXCLUDED_FIELD = [
  'sonar.auth.github.enabled',
  'sonar.auth.github.groupsSync',
  'sonar.auth.github.allowUsersToSignUp',
  'sonar.auth.github.organizations',
];

export default function GithubAuthenticationTab(props: GithubAuthenticationProps) {
  const { definitions, provider } = props;
  const [showEditModal, setShowEditModal] = useState(false);
  const [showConfirmProvisioningModal, setShowConfirmProvisioningModal] = useState(false);

  const {
    hasConfiguration,
    hasGithubProvisioning,
    githubProvisioningStatus,
    loading,
    values,
    setNewValue,
    canBeSave,
    reload,
    url,
    appId,
    enabled,
    deleteConfiguration,
    newGithubProvisioningStatus,
    setNewGithubProvisioningStatus,
    hasGithubProvisioningConfigChange,
    resetJitSetting,
  } = useGithubConfiguration(definitions, props.onReload);

  const hasDifferentProvider = provider !== undefined && provider !== Provider.Github;

  const handleCreateConfiguration = () => {
    setShowEditModal(true);
  };

  const handleCancelConfiguration = () => {
    setShowEditModal(false);
  };

  const handleConfirmChangeProvisioning = async () => {
    if (newGithubProvisioningStatus && newGithubProvisioningStatus !== githubProvisioningStatus) {
      await activateGithubProvisioning();
      await reload();
    } else {
      if (newGithubProvisioningStatus !== githubProvisioningStatus) {
        await deactivateGithubProvisioning();
      }
      await handleSaveGroup();
    }
  };

  const handleSaveGroup = async () => {
    await Promise.all(
      GITHUB_JIT_FIELDS.map(async (settingKey) => {
        const value = values[settingKey];
        if (value.newValue !== undefined) {
          if (isEmpty(value.newValue) && typeof value.newValue !== 'boolean') {
            await resetSettingValue({ keys: value.definition.key });
          } else {
            await setSettingValue(value.definition, value.newValue);
          }
        }
      })
    );
    await reload();
  };

  const handleToggleEnable = async () => {
    const value = values[GITHUB_ENABLED_FIELD];
    await setSettingValue(value.definition, !enabled);
    await reload();
  };

  return (
    <div className="authentication-configuration">
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
        <div className="big-padded text-center huge-spacer-bottom authentication-no-config">
          {translate('settings.authentication.github.form.not_configured')}
        </div>
      ) : (
        <>
          <div className="spacer-bottom big-padded bordered display-flex-space-between">
            <div>
              <h5>{translateWithParameters('settings.authentication.github.appid_x', appId)}</h5>
              <p>{url}</p>
              <p className="big-spacer-top big-spacer-bottom">
                {enabled ? (
                  <span className="authentication-enabled spacer-left">
                    <CheckIcon className="spacer-right" />
                    {translate('settings.authentication.form.enabled')}
                  </span>
                ) : (
                  translate('settings.authentication.form.not_enabled')
                )}
              </p>
              <Button
                className="spacer-top"
                onClick={handleToggleEnable}
                disabled={githubProvisioningStatus}
              >
                {enabled
                  ? translate('settings.authentication.form.disable')
                  : translate('settings.authentication.form.enable')}
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
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                if (newGithubProvisioningStatus !== githubProvisioningStatus) {
                  setShowConfirmProvisioningModal(true);
                } else {
                  await handleSaveGroup();
                }
              }}
            >
              <fieldset className="display-flex-column big-spacer-bottom">
                <label className="h5">
                  {translate('settings.authentication.form.provisioning')}
                </label>

                {enabled ? (
                  <div className="display-flex-row spacer-top">
                    <RadioCard
                      label={translate(
                        'settings.authentication.github.form.provisioning_with_github'
                      )}
                      title={translate(
                        'settings.authentication.github.form.provisioning_with_github'
                      )}
                      selected={newGithubProvisioningStatus ?? githubProvisioningStatus}
                      onClick={() => setNewGithubProvisioningStatus(true)}
                      disabled={!hasGithubProvisioning || hasDifferentProvider}
                    >
                      {hasGithubProvisioning ? (
                        <>
                          {hasDifferentProvider && (
                            <p className="spacer-bottom text-bold">
                              {translate('settings.authentication.form.other_provisioning_enabled')}
                            </p>
                          )}
                          <p className="spacer-bottom">
                            {translate(
                              'settings.authentication.github.form.provisioning_with_github.description'
                            )}
                          </p>
                          <p className="spacer-bottom">
                            <FormattedMessage
                              id="settings.authentication.github.form.provisioning_with_github.description.doc"
                              defaultMessage={translate(
                                'settings.authentication.github.form.provisioning_with_github.description.doc'
                              )}
                              values={{
                                documentation: (
                                  <DocLink
                                    to={`/instance-administration/authentication/${
                                      DOCUMENTATION_LINK_SUFFIXES[AlmKeys.GitHub]
                                    }/`}
                                  >
                                    {translate('documentation')}
                                  </DocLink>
                                ),
                              }}
                            />
                          </p>
                          {githubProvisioningStatus && <GitHubSynchronisationWarning />}
                        </>
                      ) : (
                        <p>
                          <FormattedMessage
                            id="settings.authentication.github.form.provisioning.disabled"
                            defaultMessage={translate(
                              'settings.authentication.github.form.provisioning.disabled'
                            )}
                            values={{
                              documentation: (
                                // Documentation page not ready yet.
                                <DocLink to="/instance-administration/authentication/github">
                                  {translate('documentation')}
                                </DocLink>
                              ),
                            }}
                          />
                        </p>
                      )}
                    </RadioCard>
                    <RadioCard
                      label={translate('settings.authentication.form.provisioning_at_login')}
                      title={translate('settings.authentication.form.provisioning_at_login')}
                      selected={!(newGithubProvisioningStatus ?? githubProvisioningStatus)}
                      onClick={() => setNewGithubProvisioningStatus(false)}
                    >
                      {Object.values(values).map((val) => {
                        if (!GITHUB_JIT_FIELDS.includes(val.key)) {
                          return null;
                        }
                        return (
                          <div key={val.key}>
                            <AuthenticationFormField
                              settingValue={values[val.key]?.newValue ?? values[val.key]?.value}
                              definition={val.definition}
                              mandatory={val.mandatory}
                              onFieldChange={setNewValue}
                              isNotSet={val.isNotSet}
                            />
                          </div>
                        );
                      })}
                    </RadioCard>
                  </div>
                ) : (
                  <Alert className="big-spacer-top" variant="info">
                    {translate('settings.authentication.github.enable_first')}
                  </Alert>
                )}
              </fieldset>
              {enabled && (
                <>
                  <SubmitButton disabled={!hasGithubProvisioningConfigChange}>
                    {translate('save')}
                  </SubmitButton>
                  <ResetButtonLink
                    className="spacer-left"
                    onClick={() => {
                      setNewGithubProvisioningStatus(undefined);
                      resetJitSetting();
                    }}
                    disabled={!hasGithubProvisioningConfigChange}
                  >
                    {translate('cancel')}
                  </ResetButtonLink>
                </>
              )}
              {showConfirmProvisioningModal && (
                <ConfirmModal
                  onConfirm={() => handleConfirmChangeProvisioning()}
                  header={translate(
                    'settings.authentication.github.confirm',
                    newGithubProvisioningStatus ? 'auto' : 'jit'
                  )}
                  onClose={() => setShowConfirmProvisioningModal(false)}
                  isDestructive={!newGithubProvisioningStatus}
                  confirmButtonText={translate('yes')}
                >
                  {translate(
                    'settings.authentication.github.confirm',
                    newGithubProvisioningStatus ? 'auto' : 'jit',
                    'description'
                  )}
                </ConfirmModal>
              )}
            </form>
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

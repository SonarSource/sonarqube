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
import React, { useState } from 'react';
import { FormattedMessage } from 'react-intl';
import GitHubSynchronisationWarning from '../../../../app/components/GitHubSynchronisationWarning';
import DocLink from '../../../../components/common/DocLink';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import RadioCard from '../../../../components/controls/RadioCard';
import { Button, ResetButtonLink, SubmitButton } from '../../../../components/controls/buttons';
import { Provider } from '../../../../components/hooks/useManageProvider';
import DeleteIcon from '../../../../components/icons/DeleteIcon';
import EditIcon from '../../../../components/icons/EditIcon';
import { Alert } from '../../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { useSyncNow } from '../../../../queries/github-sync';
import { AlmKeys } from '../../../../types/alm-settings';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { DOCUMENTATION_LINK_SUFFIXES } from './Authentication';
import AuthenticationFormField from './AuthenticationFormField';
import ConfigurationForm from './ConfigurationForm';
import GitHubConfigurationValidity from './GitHubConfigurationValidity';
import useGithubConfiguration, { GITHUB_JIT_FIELDS } from './hook/useGithubConfiguration';
import { useCheckGitHubConfigQuery, useIdentityProviderQuery } from './queries/identity-provider';

interface GithubAuthenticationProps {
  definitions: ExtendedSettingDefinition[];
}

const GITHUB_EXCLUDED_FIELD = [
  'sonar.auth.github.enabled',
  'sonar.auth.github.groupsSync',
  'sonar.auth.github.allowUsersToSignUp',
];

export default function GithubAuthenticationTab(props: GithubAuthenticationProps) {
  const { definitions } = props;
  const { data } = useIdentityProviderQuery();
  const [showEditModal, setShowEditModal] = useState(false);
  const [showConfirmProvisioningModal, setShowConfirmProvisioningModal] = useState(false);

  const {
    hasConfiguration,
    hasGithubProvisioning,
    githubProvisioningStatus,
    isLoading,
    values,
    setNewValue,
    canBeSave,
    url,
    appId,
    enabled,
    newGithubProvisioningStatus,
    setNewGithubProvisioningStatus,
    hasGithubProvisioningTypeChange,
    hasGithubProvisioningConfigChange,
    resetJitSetting,
    saveGroup,
    changeProvisioning,
    toggleEnable,
    hasLegacyConfiguration,
    deleteMutation: { isLoading: isDeleting, mutate: deleteConfiguration },
  } = useGithubConfiguration(definitions);

  const hasDifferentProvider = data?.provider !== undefined && data.provider !== Provider.Github;
  const { canSyncNow, synchronizeNow } = useSyncNow();
  const { refetch } = useCheckGitHubConfigQuery(enabled);

  const handleCreateConfiguration = () => {
    setShowEditModal(true);
  };

  const handleCloseConfiguration = () => {
    refetch();
    setShowEditModal(false);
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
      {enabled && (
        <GitHubConfigurationValidity
          selectedOrganizations={
            (values['sonar.auth.github.organizations']?.value as string[]) ?? []
          }
          isAutoProvisioning={!!(newGithubProvisioningStatus ?? githubProvisioningStatus)}
        />
      )}
      {!hasConfiguration && !hasLegacyConfiguration && (
        <div className="big-padded text-center huge-spacer-bottom authentication-no-config">
          {translate('settings.authentication.github.form.not_configured')}
        </div>
      )}
      {!hasConfiguration && hasLegacyConfiguration && (
        <div className="big-padded">
          <Alert variant="warning">
            <FormattedMessage
              id="settings.authentication.github.form.legacy_configured"
              defaultMessage={translate('settings.authentication.github.form.legacy_configured')}
              values={{
                documentation: (
                  <DocLink to="/instance-administration/authentication/github">
                    {translate('documentation')}
                  </DocLink>
                ),
              }}
            />
          </Alert>
        </div>
      )}
      {hasConfiguration && (
        <>
          <div className="spacer-bottom big-padded bordered display-flex-space-between">
            <div>
              <h5>{translateWithParameters('settings.authentication.github.appid_x', appId)}</h5>
              <p>{url}</p>
              <Button
                className="spacer-top"
                onClick={toggleEnable}
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
              <Button
                className="button-red"
                disabled={enabled || isDeleting}
                onClick={deleteConfiguration}
              >
                <DeleteIcon />
                {translate('settings.authentication.form.delete')}
              </Button>
            </div>
          </div>
          <div className="spacer-bottom big-padded bordered display-flex-space-between">
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                if (hasGithubProvisioningTypeChange) {
                  setShowConfirmProvisioningModal(true);
                } else {
                  await saveGroup();
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
                          <div className="sw-flex sw-flex-1 sw-items-end">
                            <Button
                              className="spacer-top width-30"
                              onClick={synchronizeNow}
                              disabled={!canSyncNow}
                            >
                              {translate('settings.authentication.github.synchronize_now')}
                            </Button>
                          </div>
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
                  onConfirm={() => changeProvisioning()}
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
          loading={isLoading}
          values={values}
          setNewValue={setNewValue}
          canBeSave={canBeSave}
          onClose={handleCloseConfiguration}
          create={!hasConfiguration}
          hasLegacyConfiguration={hasLegacyConfiguration}
        />
      )}
    </div>
  );
}

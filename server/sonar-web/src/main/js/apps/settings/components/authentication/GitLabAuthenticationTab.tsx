/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { omitBy } from 'lodash';
import React, { FormEvent, useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import GitLabSynchronisationWarning from '../../../../app/components/GitLabSynchronisationWarning';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import DocLink from '../../../../components/common/DocLink';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import RadioCard from '../../../../components/controls/RadioCard';
import Tooltip from '../../../../components/controls/Tooltip';
import { Button, ResetButtonLink, SubmitButton } from '../../../../components/controls/buttons';
import DeleteIcon from '../../../../components/icons/DeleteIcon';
import EditIcon from '../../../../components/icons/EditIcon';
import { Alert } from '../../../../components/ui/Alert';
import Spinner from '../../../../components/ui/Spinner';
import { translate } from '../../../../helpers/l10n';
import { useIdentityProviderQuery } from '../../../../queries/identity-provider/common';
import {
  useDeleteGitLabConfigurationMutation,
  useGitLabConfigurationsQuery,
  useSyncWithGitLabNow,
  useUpdateGitLabConfigurationMutation,
} from '../../../../queries/identity-provider/gitlab';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { GitLabConfigurationUpdateBody, ProvisioningType } from '../../../../types/provisioning';
import { DefinitionV2, SettingType } from '../../../../types/settings';
import { Provider } from '../../../../types/types';
import { DOCUMENTATION_LINK_SUFFIXES } from './Authentication';
import AuthenticationFormField from './AuthenticationFormField';
import GitLabConfigurationForm from './GitLabConfigurationForm';
import GitLabConfigurationValidity from './GitLabConfigurationValidity';

interface ChangesForm {
  provisioningType?: GitLabConfigurationUpdateBody['provisioningType'];
  allowUsersToSignUp?: GitLabConfigurationUpdateBody['allowUsersToSignUp'];
  provisioningToken?: GitLabConfigurationUpdateBody['provisioningToken'];
}

const definitions: Record<keyof Omit<ChangesForm, 'provisioningType'>, DefinitionV2> = {
  allowUsersToSignUp: {
    name: translate('settings.authentication.gitlab.form.allowUsersToSignUp.name'),
    secured: false,
    key: 'allowUsersToSignUp',
    description: translate('settings.authentication.gitlab.form.allowUsersToSignUp.description'),
    type: SettingType.BOOLEAN,
  },
  provisioningToken: {
    name: translate('settings.authentication.gitlab.form.provisioningToken.name'),
    secured: true,
    key: 'provisioningToken',
    description: translate('settings.authentication.gitlab.form.provisioningToken.description'),
  },
};

export default function GitLabAuthenticationTab() {
  const [openForm, setOpenForm] = React.useState(false);
  const [changes, setChanges] = React.useState<ChangesForm | undefined>(undefined);
  const [tokenKey, setTokenKey] = React.useState<number>(0);
  const [showConfirmProvisioningModal, setShowConfirmProvisioningModal] = React.useState(false);

  const hasGitlabProvisioningFeature = useContext(AvailableFeaturesContext).includes(
    Feature.GitlabProvisioning,
  );

  const { data: identityProvider } = useIdentityProviderQuery();
  const {
    data: list,
    isLoading: isLoadingList,
    isFetching,
    refetch,
  } = useGitLabConfigurationsQuery();
  const configuration = list?.gitlabConfigurations[0];

  const { canSyncNow, synchronizeNow } = useSyncWithGitLabNow();

  const { mutate: updateConfig, isLoading: isUpdating } = useUpdateGitLabConfigurationMutation();
  const { mutate: deleteConfig, isLoading: isDeleting } = useDeleteGitLabConfigurationMutation();

  const toggleEnable = () => {
    if (!configuration) {
      return;
    }
    updateConfig({ id: configuration.id, data: { enabled: !configuration.enabled } });
  };

  const deleteConfiguration = () => {
    if (!configuration) {
      return;
    }
    deleteConfig(configuration.id);
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (changes?.provisioningType !== undefined) {
      setShowConfirmProvisioningModal(true);
    } else {
      updateProvisioning();
    }
  };

  const updateProvisioning = () => {
    if (!changes || !configuration) {
      return;
    }

    updateConfig(
      { id: configuration.id, data: omitBy(changes, (value) => value === undefined) },
      {
        onSuccess: () => {
          setChanges(undefined);
          setTokenKey(tokenKey + 1);
        },
      },
    );
  };

  const setJIT = () =>
    setChangesWithCheck({
      provisioningType: ProvisioningType.jit,
      provisioningToken: undefined,
    });

  const setAuto = () =>
    setChangesWithCheck({
      provisioningType: ProvisioningType.auto,
      allowUsersToSignUp: undefined,
    });

  const hasDifferentProvider =
    identityProvider?.provider !== undefined && identityProvider.provider !== Provider.Gitlab;
  const allowUsersToSignUpDefinition = definitions.allowUsersToSignUp;
  const provisioningTokenDefinition = definitions.provisioningToken;

  const provisioningType = changes?.provisioningType ?? configuration?.provisioningType;
  const allowUsersToSignUp = changes?.allowUsersToSignUp ?? configuration?.allowUsersToSignUp;
  const provisioningToken = changes?.provisioningToken;

  const canSave = () => {
    if (!configuration || changes === undefined || isUpdating) {
      return false;
    }
    const type = changes.provisioningType ?? configuration.provisioningType;
    if (type === ProvisioningType.auto) {
      const hasConfigGroups =
        configuration.provisioningGroups && configuration.provisioningGroups.length > 0;
      const hasToken = hasConfigGroups
        ? changes.provisioningToken !== ''
        : !!changes.provisioningToken;
      return hasToken;
    }
    return true;
  };

  const setChangesWithCheck = (newChanges: ChangesForm) => {
    const newValue = {
      provisioningType:
        configuration?.provisioningType === newChanges.provisioningType
          ? undefined
          : newChanges.provisioningType,
      allowUsersToSignUp:
        configuration?.allowUsersToSignUp === newChanges.allowUsersToSignUp
          ? undefined
          : newChanges.allowUsersToSignUp,
      provisioningToken: newChanges.provisioningToken,
    };
    if (Object.values(newValue).some((v) => v !== undefined)) {
      setChanges(newValue);
    } else {
      setChanges(undefined);
    }
  };

  return (
    <Spinner loading={isLoadingList}>
      <div className="authentication-configuration">
        <div className="spacer-bottom display-flex-space-between display-flex-center">
          <h4>{translate('settings.authentication.gitlab.configuration')}</h4>
          {!configuration && (
            <div>
              <Button onClick={() => setOpenForm(true)}>
                {translate('settings.authentication.form.create')}
              </Button>
            </div>
          )}
        </div>
        {!isLoadingList && configuration?.enabled && (
          <GitLabConfigurationValidity
            configuration={configuration}
            loading={isFetching}
            onRecheck={refetch}
          />
        )}
        {!configuration && (
          <div className="big-padded text-center huge-spacer-bottom authentication-no-config">
            {translate('settings.authentication.gitlab.form.not_configured')}
          </div>
        )}
        {configuration && (
          <div className="spacer-bottom big-padded bordered display-flex-space-between">
            <div>
              <p>{configuration.url}</p>
              <Tooltip
                overlay={
                  configuration.provisioningType === ProvisioningType.auto
                    ? translate('settings.authentication.form.disable.tooltip')
                    : null
                }
              >
                <Button
                  className="spacer-top"
                  onClick={toggleEnable}
                  disabled={isUpdating || configuration.provisioningType === ProvisioningType.auto}
                >
                  {configuration.enabled
                    ? translate('settings.authentication.form.disable')
                    : translate('settings.authentication.form.enable')}
                </Button>
              </Tooltip>
            </div>
            <div>
              <Button className="spacer-right" onClick={() => setOpenForm(true)}>
                <EditIcon />
                {translate('settings.authentication.form.edit')}
              </Button>
              <Tooltip
                overlay={
                  configuration.enabled
                    ? translate('settings.authentication.form.delete.tooltip')
                    : null
                }
              >
                <Button
                  className="button-red"
                  disabled={configuration.enabled || isDeleting}
                  onClick={deleteConfiguration}
                >
                  <DeleteIcon />
                  {translate('settings.authentication.form.delete')}
                </Button>
              </Tooltip>
            </div>
          </div>
        )}
        {configuration && (
          <div className="spacer-bottom big-padded bordered">
            <form onSubmit={handleSubmit}>
              <fieldset className="display-flex-column big-spacer-bottom">
                <label className="h5">
                  {translate('settings.authentication.form.provisioning')}
                </label>

                {configuration.enabled ? (
                  <div className="display-flex-column spacer-top">
                    <RadioCard
                      className="sw-min-h-0"
                      label={translate('settings.authentication.gitlab.provisioning_at_login')}
                      title={translate('settings.authentication.gitlab.provisioning_at_login')}
                      selected={provisioningType === ProvisioningType.jit}
                      onClick={setJIT}
                    >
                      <p className="spacer-bottom">
                        <FormattedMessage id="settings.authentication.gitlab.provisioning_at_login.description" />
                      </p>
                      <p className="spacer-bottom">
                        <DocLink
                          to={`/instance-administration/authentication/${
                            DOCUMENTATION_LINK_SUFFIXES[AlmKeys.GitLab]
                          }/#choosing-the-provisioning-method`}
                        >
                          {translate(
                            `settings.authentication.gitlab.description.${ProvisioningType.jit}.learn_more`,
                          )}
                        </DocLink>
                      </p>
                      {provisioningType === ProvisioningType.jit &&
                        allowUsersToSignUpDefinition !== undefined && (
                          <AuthenticationFormField
                            settingValue={allowUsersToSignUp}
                            definition={allowUsersToSignUpDefinition}
                            mandatory
                            onFieldChange={(_, value) =>
                              setChangesWithCheck({
                                ...changes,
                                allowUsersToSignUp: value as boolean,
                              })
                            }
                            isNotSet={configuration.provisioningType !== ProvisioningType.auto}
                          />
                        )}
                    </RadioCard>
                    <RadioCard
                      className="spacer-top sw-min-h-0"
                      label={translate(
                        'settings.authentication.gitlab.form.provisioning_with_gitlab',
                      )}
                      title={translate(
                        'settings.authentication.gitlab.form.provisioning_with_gitlab',
                      )}
                      selected={provisioningType === ProvisioningType.auto}
                      onClick={setAuto}
                      disabled={!hasGitlabProvisioningFeature || hasDifferentProvider}
                    >
                      {hasGitlabProvisioningFeature ? (
                        <>
                          {hasDifferentProvider && (
                            <p className="spacer-bottom text-bold ">
                              {translate('settings.authentication.form.other_provisioning_enabled')}
                            </p>
                          )}
                          <p className="spacer-bottom">
                            {translate(
                              'settings.authentication.gitlab.form.provisioning_with_gitlab.description',
                            )}
                          </p>
                          <p className="spacer-bottom">
                            <DocLink
                              to={`/instance-administration/authentication/${
                                DOCUMENTATION_LINK_SUFFIXES[AlmKeys.GitLab]
                              }/#choosing-the-provisioning-method`}
                            >
                              {translate(
                                `settings.authentication.gitlab.description.${ProvisioningType.auto}.learn_more`,
                              )}
                            </DocLink>
                          </p>

                          {configuration.provisioningType === ProvisioningType.auto && (
                            <GitLabSynchronisationWarning />
                          )}

                          {provisioningType === ProvisioningType.auto && (
                            <>
                              <div className="sw-flex sw-flex-1 spacer-bottom">
                                <Button
                                  className="spacer-top width-30"
                                  onClick={synchronizeNow}
                                  disabled={!canSyncNow}
                                >
                                  {translate('settings.authentication.github.synchronize_now')}
                                </Button>
                              </div>
                              <hr />
                              <AuthenticationFormField
                                settingValue={provisioningToken}
                                key={tokenKey}
                                definition={provisioningTokenDefinition}
                                mandatory
                                onFieldChange={(_, value) =>
                                  setChangesWithCheck({
                                    ...changes,
                                    provisioningToken: value as string,
                                  })
                                }
                                isNotSet={configuration.provisioningType !== ProvisioningType.auto}
                              />
                            </>
                          )}
                        </>
                      ) : (
                        <p>
                          <FormattedMessage
                            id="settings.authentication.gitlab.form.provisioning.disabled"
                            defaultMessage={translate(
                              'settings.authentication.gitlab.form.provisioning.disabled',
                            )}
                            values={{
                              documentation: (
                                <DocLink to="/instance-administration/authentication/gitlab">
                                  {translate('documentation')}
                                </DocLink>
                              ),
                            }}
                          />
                        </p>
                      )}
                    </RadioCard>
                  </div>
                ) : (
                  <Alert className="big-spacer-top" variant="info">
                    {translate('settings.authentication.gitlab.enable_first')}
                  </Alert>
                )}
              </fieldset>
              {configuration.enabled && (
                <div className="sw-flex sw-gap-2 sw-h-8 sw-items-center">
                  <SubmitButton disabled={!canSave()}>{translate('save')}</SubmitButton>
                  <ResetButtonLink
                    onClick={() => {
                      setChanges(undefined);
                      setTokenKey(tokenKey + 1);
                    }}
                    disabled={false}
                  >
                    {translate('cancel')}
                  </ResetButtonLink>
                  <Alert variant="warning" className="sw-mb-0">
                    {canSave() &&
                      translate('settings.authentication.gitlab.configuration.unsaved_changes')}
                  </Alert>
                  <Spinner loading={isUpdating} />
                </div>
              )}
              {showConfirmProvisioningModal && provisioningType && (
                <ConfirmModal
                  onConfirm={updateProvisioning}
                  header={translate('settings.authentication.gitlab.confirm', provisioningType)}
                  onClose={() => setShowConfirmProvisioningModal(false)}
                  confirmButtonText={translate(
                    'settings.authentication.gitlab.provisioning_change.confirm_changes',
                  )}
                >
                  {translate(
                    'settings.authentication.gitlab.confirm',
                    provisioningType,
                    'description',
                  )}
                </ConfirmModal>
              )}
            </form>
          </div>
        )}
      </div>
      {openForm && (
        <GitLabConfigurationForm data={configuration ?? null} onClose={() => setOpenForm(false)} />
      )}
    </Spinner>
  );
}

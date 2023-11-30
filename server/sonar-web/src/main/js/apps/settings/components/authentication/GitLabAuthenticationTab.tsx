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
import { isEqual, omitBy } from 'lodash';
import React, { FormEvent, useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import {
  GITLAB_SETTING_ALLOW_SIGNUP,
  GITLAB_SETTING_GROUPS,
  GITLAB_SETTING_GROUP_TOKEN,
} from '../../../../api/provisioning';
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
import {
  useDeleteGitLabConfigurationMutation,
  useGitLabConfigurationsQuery,
  useIdentityProviderQuery,
  useUpdateGitLabConfigurationMutation,
} from '../../../../queries/identity-provider';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import { GitLabConfigurationUpdateBody, ProvisioningType } from '../../../../types/provisioning';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { Provider } from '../../../../types/types';
import { DOCUMENTATION_LINK_SUFFIXES } from './Authentication';
import AuthenticationFormField from './AuthenticationFormField';
import GitLabConfigurationForm from './GitLabConfigurationForm';

interface GitLabAuthenticationTab {
  definitions: ExtendedSettingDefinition[];
}

interface ChangesForm {
  type?: GitLabConfigurationUpdateBody['type'];
  allowUsersToSignUp?: GitLabConfigurationUpdateBody['allowUsersToSignUp'];
  provisioningToken?: GitLabConfigurationUpdateBody['provisioningToken'];
  groups?: GitLabConfigurationUpdateBody['groups'];
}

export default function GitLabAuthenticationTab(props: Readonly<GitLabAuthenticationTab>) {
  const { definitions } = props;

  const [openForm, setOpenForm] = React.useState(false);
  const [changes, setChanges] = React.useState<ChangesForm | undefined>(undefined);
  const [tokenKey, setTokenKey] = React.useState<number>(0);
  const [showConfirmProvisioningModal, setShowConfirmProvisioningModal] = React.useState(false);

  const hasGitlabProvisioningFeature = useContext(AvailableFeaturesContext).includes(
    Feature.GitlabProvisioning,
  );

  const { data: identityProvider } = useIdentityProviderQuery();
  const { data: list, isLoading: isLoadingList } = useGitLabConfigurationsQuery();
  const configuration = list?.configurations[0];

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
    if (changes?.type !== undefined) {
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
      type: ProvisioningType.jit,
      provisioningToken: undefined,
      groups: undefined,
    });

  const setAuto = () =>
    setChangesWithCheck({
      type: ProvisioningType.auto,
      allowUsersToSignUp: undefined,
    });

  const hasDifferentProvider =
    identityProvider?.provider !== undefined && identityProvider.provider !== Provider.Gitlab;
  const allowUsersToSignUpDefinition = definitions.find(
    (d) => d.key === GITLAB_SETTING_ALLOW_SIGNUP,
  );
  const provisioningTokenDefinition = definitions.find((d) => d.key === GITLAB_SETTING_GROUP_TOKEN);
  const provisioningGroupDefinition = definitions.find((d) => d.key === GITLAB_SETTING_GROUPS);

  const provisioningType = changes?.type ?? configuration?.type;
  const allowUsersToSignUp = changes?.allowUsersToSignUp ?? configuration?.allowUsersToSignUp;
  const provisioningToken = changes?.provisioningToken;
  const groups = changes?.groups ?? configuration?.groups;

  const canSave = () => {
    if (!configuration || changes === undefined) {
      return false;
    }
    const type = changes.type ?? configuration.type;
    if (type === ProvisioningType.auto) {
      const hasConfigGroups = configuration.groups && configuration.groups.length > 0;
      const hasGroups = changes.groups ? changes.groups.length > 0 : hasConfigGroups;
      const hasToken = hasConfigGroups
        ? changes.provisioningToken !== ''
        : !!changes.provisioningToken;
      return hasGroups && hasToken;
    }
    return true;
  };

  const setChangesWithCheck = (newChanges: ChangesForm) => {
    const newValue = {
      type: configuration?.type === newChanges.type ? undefined : newChanges.type,
      allowUsersToSignUp:
        configuration?.allowUsersToSignUp === newChanges.allowUsersToSignUp
          ? undefined
          : newChanges.allowUsersToSignUp,
      provisioningToken: newChanges.provisioningToken,
      groups: isEqual(configuration?.groups, newChanges.groups) ? undefined : newChanges.groups,
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
                  configuration.type === ProvisioningType.auto
                    ? translate('settings.authentication.form.disable.tooltip')
                    : null
                }
              >
                <Button
                  className="spacer-top"
                  onClick={toggleEnable}
                  disabled={isUpdating || configuration.type === ProvisioningType.auto}
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
        <div className="spacer-bottom big-padded bordered">
          <form onSubmit={handleSubmit}>
            <fieldset className="display-flex-column big-spacer-bottom">
              <label className="h5">{translate('settings.authentication.form.provisioning')}</label>

              {configuration?.enabled ? (
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
                      <FormattedMessage
                        id="settings.authentication.gitlab.description.doc"
                        values={{
                          documentation: (
                            <DocLink
                              to={`/instance-administration/authentication/${
                                DOCUMENTATION_LINK_SUFFIXES[AlmKeys.GitLab]
                              }/`}
                            >
                              {translate('documentation')}
                            </DocLink>
                          ),
                        }}
                      />
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
                          isNotSet={configuration.type !== ProvisioningType.auto}
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
                          <FormattedMessage
                            id="settings.authentication.gitlab.description.doc"
                            values={{
                              documentation: (
                                <DocLink
                                  to={`/instance-administration/authentication/${
                                    DOCUMENTATION_LINK_SUFFIXES[AlmKeys.GitLab]
                                  }/`}
                                >
                                  {translate('documentation')}
                                </DocLink>
                              ),
                            }}
                          />
                        </p>

                        {configuration?.type === ProvisioningType.auto && (
                          <>
                            <GitLabSynchronisationWarning />
                            <hr className="spacer-top" />
                          </>
                        )}

                        {provisioningType === ProvisioningType.auto &&
                          provisioningTokenDefinition !== undefined &&
                          provisioningGroupDefinition !== undefined && (
                            <>
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
                                isNotSet={
                                  configuration.type !== ProvisioningType.auto &&
                                  configuration.groups?.length === 0
                                }
                              />
                              <AuthenticationFormField
                                settingValue={groups}
                                definition={provisioningGroupDefinition}
                                mandatory
                                onFieldChange={(_, values) =>
                                  setChangesWithCheck({ ...changes, groups: values as string[] })
                                }
                                isNotSet={configuration.type !== ProvisioningType.auto}
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
                  {translate('settings.authentication.github.enable_first')}
                </Alert>
              )}
            </fieldset>
            {configuration?.enabled && (
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
      </div>
      {openForm && (
        <GitLabConfigurationForm data={configuration ?? null} onClose={() => setOpenForm(false)} />
      )}
    </Spinner>
  );
}

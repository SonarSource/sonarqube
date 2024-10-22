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

import { isEmpty, omitBy } from 'lodash';
import { FormEvent, useContext, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { ButtonSecondary, Highlight, Note, Spinner } from '~design-system';
import GitLabSynchronisationWarning from '../../../../app/components/GitLabSynchronisationWarning';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import DocumentationLink from '../../../../components/common/DocumentationLink';
import { DocLink } from '../../../../helpers/doc-links';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { useIdentityProviderQuery } from '../../../../queries/identity-provider/common';
import {
  useDeleteGitLabConfigurationMutation,
  useGitLabConfigurationsQuery,
  useGitlabRolesMappingMutation,
  useSyncWithGitLabNow,
  useUpdateGitLabConfigurationMutation,
} from '../../../../queries/identity-provider/gitlab';
import { Feature } from '../../../../types/features';
import {
  DevopsRolesMapping,
  GitLabConfigurationUpdateBody,
  ProvisioningType,
} from '../../../../types/provisioning';
import { DefinitionV2, SettingType } from '../../../../types/settings';
import { Provider } from '../../../../types/types';
import AuthenticationFormField from './AuthenticationFormField';
import AutoProvisioningConsent from './AutoProvisionningConsent';
import ConfigurationDetails from './ConfigurationDetails';
import ConfirmProvisioningModal from './ConfirmProvisioningModal';
import GitLabConfigurationForm from './GitLabConfigurationForm';
import GitLabConfigurationValidity from './GitLabConfigurationValidity';
import GitLabMappingModal from './GitLabMappingModal';
import ProvisioningSection from './ProvisioningSection';
import TabHeader from './TabHeader';

interface ChangesForm {
  allowUsersToSignUp?: GitLabConfigurationUpdateBody['allowUsersToSignUp'];
  allowedGroups?: GitLabConfigurationUpdateBody['allowedGroups'];
  provisioningToken?: GitLabConfigurationUpdateBody['provisioningToken'];
  provisioningType?: GitLabConfigurationUpdateBody['provisioningType'];
}

const getDefinitions = (
  provisioningType: ProvisioningType,
): Record<keyof Omit<ChangesForm, 'provisioningType'>, DefinitionV2> => {
  return {
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
    allowedGroups: {
      name: translate('settings.authentication.gitlab.form.allowedGroups.name'),
      secured: false,
      key: 'allowedGroups',
      description: translate(
        `settings.authentication.gitlab.form.allowedGroups.description.${provisioningType}`,
      ),
      multiValues: true,
    },
  };
};

export default function GitLabAuthenticationTab() {
  const [openForm, setOpenForm] = useState(false);
  const [changes, setChanges] = useState<ChangesForm | undefined>();
  const [tokenKey, setTokenKey] = useState<number>(0);
  const [showConfirmProvisioningModal, setShowConfirmProvisioningModal] = useState(false);
  const [isMappingModalOpen, setIsMappingModalOpen] = useState(false);
  const [rolesMapping, setRolesMapping] = useState<DevopsRolesMapping[] | null>(null);
  const { mutateAsync: updateMapping } = useGitlabRolesMappingMutation();

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

  const { mutate: updateConfig, isPending: isUpdating } = useUpdateGitLabConfigurationMutation();
  const { mutate: deleteConfig, isPending: isDeleting } = useDeleteGitLabConfigurationMutation();

  const definitions = getDefinitions(
    changes?.provisioningType ?? configuration?.provisioningType ?? ProvisioningType.jit,
  );
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
    if (
      changes?.provisioningType !== undefined ||
      (provisioningType === ProvisioningType.jit && allowUsersToSignUp && isEmpty(allowedGroups))
    ) {
      setShowConfirmProvisioningModal(true);
    } else {
      updateProvisioning();
    }
  };

  const updateProvisioning = () => {
    if ((!changes && !rolesMapping) || !configuration) {
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

    if (provisioningType === ProvisioningType.auto && rolesMapping) {
      updateMapping(rolesMapping)
        .then(() => {
          setRolesMapping(null);
        })
        .catch(() => {});
    }
  };

  const setJIT = () =>
    setChangesWithCheck({
      provisioningType: ProvisioningType.jit,
      allowedGroups: changes?.allowedGroups,
      provisioningToken: undefined,
    });

  const setAuto = () =>
    setChangesWithCheck({
      provisioningType: ProvisioningType.auto,
      allowedGroups: changes?.allowedGroups,
      allowUsersToSignUp: undefined,
    });

  const hasDifferentProvider =
    identityProvider?.provider !== undefined && identityProvider.provider !== Provider.Gitlab;
  const allowUsersToSignUpDefinition = definitions.allowUsersToSignUp;
  const allowedGroupsDefinition = definitions.allowedGroups;
  const provisioningTokenDefinition = definitions.provisioningToken;

  const provisioningType = changes?.provisioningType ?? configuration?.provisioningType;
  const allowUsersToSignUp = changes?.allowUsersToSignUp ?? configuration?.allowUsersToSignUp;
  const allowedGroups = changes?.allowedGroups ?? configuration?.allowedGroups;
  const provisioningToken = changes?.provisioningToken;

  const canSave = () => {
    if (!configuration || (changes === undefined && rolesMapping === null) || isUpdating) {
      return false;
    }
    const type = changes?.provisioningType ?? configuration.provisioningType;
    if (type === ProvisioningType.auto && rolesMapping !== null) {
      return true;
    }
    if (changes && type === ProvisioningType.auto) {
      const areGroupsDefined =
        changes.allowedGroups?.some((val) => val !== '') ??
        configuration.allowedGroups?.some((val) => val !== '');
      return (
        (configuration.isProvisioningTokenSet || !!changes.provisioningToken) && areGroupsDefined
      );
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
      allowedGroups:
        configuration?.allowedGroups === newChanges.allowedGroups
          ? undefined
          : newChanges.allowedGroups,
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
      <div>
        <TabHeader
          title={translate('settings.authentication.gitlab.configuration')}
          showCreate={!configuration}
          onCreate={() => setOpenForm(true)}
          configurationValidity={
            <>
              {!isLoadingList && configuration?.enabled && (
                <GitLabConfigurationValidity
                  configuration={configuration}
                  loading={isFetching}
                  onRecheck={refetch}
                />
              )}
            </>
          }
        />
        {!configuration && (
          <div>{translate('settings.authentication.gitlab.form.not_configured')}</div>
        )}
        {configuration && (
          <>
            <ConfigurationDetails
              title={translateWithParameters(
                'settings.authentication.gitlab.applicationId.name',
                configuration.applicationId,
              )}
              url={configuration.url}
              canDisable={!isUpdating && configuration.provisioningType !== ProvisioningType.auto}
              enabled={configuration.enabled}
              isDeleting={isDeleting}
              onEdit={() => setOpenForm(true)}
              onDelete={deleteConfiguration}
              onToggle={toggleEnable}
            />
            <ProvisioningSection
              isLoading={isUpdating}
              provisioningType={provisioningType ?? ProvisioningType.jit}
              onChangeProvisioningType={(val: ProvisioningType) =>
                val === ProvisioningType.auto ? setAuto() : setJIT()
              }
              disabledConfigText={translate('settings.authentication.gitlab.enable_first')}
              enabled={configuration.enabled}
              hasUnsavedChanges={changes !== undefined || rolesMapping !== null}
              canSave={canSave()}
              onSave={handleSubmit}
              onCancel={() => {
                setChanges(undefined);
                setTokenKey(tokenKey + 1);
                setRolesMapping(null);
              }}
              jitTitle={translate('settings.authentication.gitlab.provisioning_at_login')}
              jitDescription={
                <FormattedMessage
                  id="settings.authentication.gitlab.provisioning_at_login.description"
                  values={{
                    documentation: (
                      <DocumentationLink to={DocLink.AlmGitLabAuthJITProvisioningMethod}>
                        {translate(`learn_more`)}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              jitSettings={
                <>
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
                  <AuthenticationFormField
                    className="sw-mt-8"
                    settingValue={allowedGroups}
                    definition={allowedGroupsDefinition}
                    onFieldChange={(_, values) =>
                      setChangesWithCheck({
                        ...changes,
                        allowedGroups: values as string[],
                      })
                    }
                    isNotSet={configuration.provisioningType !== ProvisioningType.auto}
                  />
                </>
              }
              autoTitle={translate('settings.authentication.gitlab.form.provisioning_with_gitlab')}
              hasDifferentProvider={hasDifferentProvider}
              hasFeatureEnabled={hasGitlabProvisioningFeature}
              autoFeatureDisabledText={
                <FormattedMessage
                  id="settings.authentication.gitlab.form.provisioning.disabled"
                  defaultMessage={translate(
                    'settings.authentication.gitlab.form.provisioning.disabled',
                  )}
                  values={{
                    documentation: (
                      <DocumentationLink to={DocLink.AlmGitLabAuth}>
                        {translate('documentation')}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              autoDescription={
                <FormattedMessage
                  id="settings.authentication.gitlab.form.provisioning_with_gitlab.description"
                  values={{
                    documentation: (
                      <DocumentationLink to={DocLink.AlmGitLabAuthAutoProvisioningMethod}>
                        {translate(`learn_more`)}
                      </DocumentationLink>
                    ),
                  }}
                />
              }
              onSyncNow={synchronizeNow}
              canSync={canSyncNow}
              synchronizationDetails={<GitLabSynchronisationWarning />}
              autoSettings={
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
                    isNotSet={!configuration.isProvisioningTokenSet}
                  />
                  <AuthenticationFormField
                    className="sw-mt-8"
                    settingValue={allowedGroups}
                    definition={allowedGroupsDefinition}
                    mandatory
                    onFieldChange={(_, values) =>
                      setChangesWithCheck({
                        ...changes,
                        allowedGroups: values as string[],
                      })
                    }
                    isNotSet={configuration.provisioningType !== ProvisioningType.auto}
                  />
                  <div className="sw-mt-6">
                    <div className="sw-flex">
                      <Highlight className="sw-mb-4 sw-mr-4 sw-flex sw-items-center sw-gap-2">
                        <FormattedMessage id="settings.authentication.configuration.roles_mapping.title" />
                      </Highlight>
                      <ButtonSecondary
                        className="sw--mt-2"
                        onClick={() => setIsMappingModalOpen(true)}
                      >
                        <FormattedMessage id="settings.authentication.configuration.roles_mapping.button_label" />
                      </ButtonSecondary>
                    </div>
                    <Note className="sw-mt-2">
                      <FormattedMessage id="settings.authentication.gitlab.configuration.roles_mapping.description" />
                    </Note>
                  </div>
                </>
              }
            />
          </>
        )}
      </div>
      {provisioningType && (
        <ConfirmProvisioningModal
          allowUsersToSignUp={allowUsersToSignUp}
          hasProvisioningTypeChange={Boolean(changes?.provisioningType)}
          isAllowListEmpty={isEmpty(allowedGroups)}
          isOpen={showConfirmProvisioningModal}
          onClose={() => setShowConfirmProvisioningModal(false)}
          onConfirm={updateProvisioning}
          provider={Provider.Gitlab}
          provisioningStatus={provisioningType}
        />
      )}
      {isMappingModalOpen && (
        <GitLabMappingModal
          mapping={rolesMapping}
          setMapping={setRolesMapping}
          onClose={() => setIsMappingModalOpen(false)}
        />
      )}
      {openForm && (
        <GitLabConfigurationForm
          gitlabConfiguration={configuration ?? null}
          onClose={() => setOpenForm(false)}
        />
      )}
      <AutoProvisioningConsent gitlabConfiguration={configuration} />
    </Spinner>
  );
}

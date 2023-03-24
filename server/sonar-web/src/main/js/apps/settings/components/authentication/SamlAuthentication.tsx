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
import React from 'react';
import { FormattedMessage } from 'react-intl';
import {
  activateScim,
  deactivateScim,
  resetSettingValue,
  setSettingValue,
} from '../../../../api/settings';
import DocLink from '../../../../components/common/DocLink';
import Link from '../../../../components/common/Link';
import { Button, ResetButtonLink, SubmitButton } from '../../../../components/controls/buttons';
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import RadioCard from '../../../../components/controls/RadioCard';
import CheckIcon from '../../../../components/icons/CheckIcon';
import DeleteIcon from '../../../../components/icons/DeleteIcon';
import EditIcon from '../../../../components/icons/EditIcon';
import { Alert } from '../../../../components/ui/Alert';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { getPropertyName } from '../../utils';
import DefinitionDescription from '../DefinitionDescription';
import useSamlConfiguration, { SAML_ENABLED_FIELD } from './hook/useLoadSamlSettings';
import SamlConfigurationForm from './SamlConfigurationForm';

interface SamlAuthenticationProps {
  definitions: ExtendedSettingDefinition[];
}

export const SAML = 'saml';

const CONFIG_TEST_PATH = '/saml/validation_init';

export default function SamlAuthentication(props: SamlAuthenticationProps) {
  const { definitions } = props;
  const [showEditModal, setShowEditModal] = React.useState(false);
  const [showConfirmProvisioningModal, setShowConfirmProvisioningModal] = React.useState(false);
  const {
    hasScim,
    scimStatus,
    loading,
    samlEnabled,
    name,
    groupValue,
    url,
    hasConfiguration,
    values,
    setNewValue,
    canBeSave,
    hasScimConfigChange,
    newScimStatus,
    setNewScimStatus,
    setNewGroupSetting,
    onReload,
  } = useSamlConfiguration(definitions);

  const handleDeleteConfiguration = async () => {
    await resetSettingValue({ keys: Object.keys(values).join(',') });
    await onReload();
  };

  const handleCreateConfiguration = () => {
    setShowEditModal(true);
  };

  const handleCancelConfiguration = () => {
    setShowEditModal(false);
  };

  const handleToggleEnable = async () => {
    const value = values[SAML_ENABLED_FIELD];
    await setSettingValue(value.definition, !samlEnabled);
    await onReload();
  };

  const handleSaveGroup = async () => {
    if (groupValue.newValue !== undefined) {
      if (isEmpty(groupValue.newValue)) {
        await resetSettingValue({ keys: groupValue.definition.key });
      } else {
        await setSettingValue(groupValue.definition, groupValue.newValue);
      }
      await onReload();
    }
  };

  const handleConfirmChangeProvisioning = async () => {
    if (newScimStatus) {
      await activateScim();
    } else {
      await deactivateScim();
      await handleSaveGroup();
    }
    await onReload();
  };

  return (
    <div className="saml-configuration">
      <div className="spacer-bottom display-flex-space-between display-flex-center">
        <h4>{translate('settings.authentication.saml.configuration')}</h4>

        {!hasConfiguration && (
          <div>
            <Button onClick={handleCreateConfiguration}>
              {translate('settings.authentication.form.create')}
            </Button>
          </div>
        )}
      </div>
      {!hasConfiguration && (
        <div className="big-padded text-center huge-spacer-bottom saml-no-config">
          {translate('settings.authentication.saml.form.not_configured')}
        </div>
      )}

      {hasConfiguration && (
        <>
          <div className="spacer-bottom big-padded bordered display-flex-space-between">
            <div>
              <h5>{name}</h5>
              <p>{url}</p>
              <p className="big-spacer-top big-spacer-bottom">
                {samlEnabled ? (
                  <span className="saml-enabled spacer-left">
                    <CheckIcon className="spacer-right" />
                    {translate('settings.authentication.saml.form.enabled')}
                  </span>
                ) : (
                  translate('settings.authentication.saml.form.not_enabled')
                )}
              </p>
              <Button className="spacer-top" disabled={scimStatus} onClick={handleToggleEnable}>
                {samlEnabled
                  ? translate('settings.authentication.saml.form.disable')
                  : translate('settings.authentication.saml.form.enable')}
              </Button>
            </div>
            <div>
              <Link
                className="button spacer-right"
                target="_blank"
                to={`${getBaseUrl()}${CONFIG_TEST_PATH}`}
              >
                {translate('settings.authentication.saml.form.test')}
              </Link>
              <Button className="spacer-right" onClick={handleCreateConfiguration}>
                <EditIcon />
                {translate('settings.authentication.form.edit')}
              </Button>
              <Button
                className="button-red"
                disabled={samlEnabled}
                onClick={handleDeleteConfiguration}
              >
                <DeleteIcon />
                {translate('settings.authentication.form.delete')}
              </Button>
            </div>
          </div>
          {hasScim && (
            <div className="spacer-bottom big-padded bordered display-flex-space-between">
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  if (newScimStatus !== scimStatus) {
                    setShowConfirmProvisioningModal(true);
                  } else {
                    handleSaveGroup();
                  }
                }}
              >
                <fieldset className="display-flex-column big-spacer-bottom">
                  <label className="h5">
                    {translate('settings.authentication.saml.form.provisioning')}
                  </label>
                  {samlEnabled ? (
                    <div className="display-flex-row spacer-top">
                      <RadioCard
                        label={translate(
                          'settings.authentication.saml.form.provisioning_with_scim'
                        )}
                        title={translate(
                          'settings.authentication.saml.form.provisioning_with_scim'
                        )}
                        selected={newScimStatus ?? scimStatus}
                        onClick={() => setNewScimStatus(true)}
                      >
                        <p className="spacer-bottom">
                          {translate(
                            'settings.authentication.saml.form.provisioning_with_scim.sub'
                          )}
                        </p>
                        <p className="spacer-bottom">
                          {translate(
                            'settings.authentication.saml.form.provisioning_with_scim.description'
                          )}
                        </p>
                        <p>
                          <FormattedMessage
                            id="settings.authentication.saml.form.provisioning_with_scim.description.doc"
                            defaultMessage={translate(
                              'settings.authentication.saml.form.provisioning_with_scim.description.doc'
                            )}
                            values={{
                              documentation: (
                                <DocLink to="/instance-administration/authentication/saml/scim/overview">
                                  {translate('documentation')}
                                </DocLink>
                              ),
                            }}
                          />
                        </p>
                      </RadioCard>
                      <RadioCard
                        label={translate('settings.authentication.saml.form.provisioning_at_login')}
                        title={translate('settings.authentication.saml.form.provisioning_at_login')}
                        selected={!(newScimStatus ?? scimStatus)}
                        onClick={() => setNewScimStatus(false)}
                      >
                        <p>
                          {translate('settings.authentication.saml.form.provisioning_at_login.sub')}
                        </p>
                        {groupValue && (
                          <div className="settings-definition">
                            <DefinitionDescription definition={groupValue.definition} />
                            <div className="settings-definition-right">
                              <input
                                id={groupValue.definition.key}
                                maxLength={4000}
                                name={groupValue.definition.key}
                                onChange={(e) => setNewGroupSetting(e.currentTarget.value)}
                                type="text"
                                value={String(groupValue.newValue ?? groupValue.value ?? '')}
                                aria-label={getPropertyName(groupValue.definition)}
                              />
                            </div>
                          </div>
                        )}
                      </RadioCard>
                    </div>
                  ) : (
                    <Alert className="big-spacer-top" variant="info">
                      {translate('settings.authentication.saml.enable_first')}
                    </Alert>
                  )}
                </fieldset>
                {samlEnabled && (
                  <>
                    <SubmitButton disabled={!hasScimConfigChange}>{translate('save')}</SubmitButton>
                    <ResetButtonLink
                      className="spacer-left"
                      onClick={() => {
                        setNewScimStatus(undefined);
                        setNewGroupSetting();
                      }}
                      disabled={!hasScimConfigChange}
                    >
                      {translate('cancel')}
                    </ResetButtonLink>
                  </>
                )}
                {showConfirmProvisioningModal && (
                  <ConfirmModal
                    onConfirm={() => handleConfirmChangeProvisioning()}
                    header={translate(
                      'settings.authentication.saml.confirm',
                      newScimStatus ? 'scim' : 'jit'
                    )}
                    onClose={() => setShowConfirmProvisioningModal(false)}
                    isDestructive={!newScimStatus}
                    confirmButtonText={translate('yes')}
                  >
                    {translate(
                      'settings.authentication.saml.confirm',
                      newScimStatus ? 'scim' : 'jit',
                      'description'
                    )}
                  </ConfirmModal>
                )}
              </form>
            </div>
          )}
        </>
      )}
      {showEditModal && (
        <SamlConfigurationForm
          loading={loading}
          values={values}
          setNewValue={setNewValue}
          canBeSave={canBeSave}
          onClose={handleCancelConfiguration}
          create={!hasConfiguration}
          onReload={onReload}
        />
      )}
    </div>
  );
}

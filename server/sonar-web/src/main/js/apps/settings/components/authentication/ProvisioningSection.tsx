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
import {
  BasicSeparator,
  ButtonPrimary,
  ButtonSecondary,
  FlagMessage,
  RadioButton,
  Spinner,
  SubHeading,
} from 'design-system';
import React, { FormEvent, ReactElement } from 'react';
import { translate } from '../../../../helpers/l10n';
import { ProvisioningType } from '../../../../types/provisioning';

interface Props {
  autoDescription: ReactElement;
  autoFeatureDisabledText: string | ReactElement;
  autoSettings?: ReactElement;
  autoTitle: string;
  canSave?: boolean;
  canSync?: boolean;
  disabledConfigText: string;
  enabled: boolean;
  hasDifferentProvider: boolean;
  hasFeatureEnabled: boolean;
  hasUnsavedChanges: boolean;
  isLoading?: boolean;
  jitDescription: string | ReactElement;
  jitSettings?: ReactElement;
  jitTitle: string;
  onCancel: () => void;
  onChangeProvisioningType: (val: ProvisioningType) => void;
  onSave: (e: FormEvent) => void;
  onSyncNow?: () => void;
  provisioningType: ProvisioningType;
  synchronizationDetails?: ReactElement;
}

export default function ProvisioningSection(props: Readonly<Props>) {
  const {
    isLoading,
    provisioningType,
    jitTitle,
    jitDescription,
    jitSettings,
    autoTitle,
    autoDescription,
    autoSettings,
    hasFeatureEnabled,
    hasDifferentProvider,
    autoFeatureDisabledText,
    synchronizationDetails,
    onChangeProvisioningType,
    onSave,
    onSyncNow,
    onCancel,
    hasUnsavedChanges,
    enabled,
    disabledConfigText,
    canSave = true,
    canSync,
  } = props;
  return (
    <div className="sw-mb-2">
      <form onSubmit={onSave}>
        <SubHeading as="h5">{translate('settings.authentication.form.provisioning')}</SubHeading>
        {enabled ? (
          <>
            <ul>
              <li>
                <RadioButton
                  id="jit"
                  checked={provisioningType === ProvisioningType.jit}
                  onCheck={onChangeProvisioningType}
                  className="sw-items-start"
                  value={ProvisioningType.jit}
                >
                  <div>
                    <div className="sw-body-sm-highlight">{jitTitle}</div>

                    <div className="sw-mt-1">{jitDescription}</div>
                  </div>
                </RadioButton>
                {provisioningType === ProvisioningType.jit && jitSettings && (
                  <div className="sw-ml-16 sw-mt-6 sw-max-w-[435px]">{jitSettings}</div>
                )}
                <BasicSeparator className="sw-my-4" />
              </li>
              <li>
                <RadioButton
                  id="github-auto"
                  className="sw-items-start"
                  checked={provisioningType === ProvisioningType.auto}
                  onCheck={onChangeProvisioningType}
                  value={ProvisioningType.auto}
                  disabled={!hasFeatureEnabled || hasDifferentProvider}
                >
                  <div>
                    <div className="sw-body-sm-highlight">{autoTitle}</div>
                    <div className="sw-mt-1">
                      {hasFeatureEnabled ? (
                        <>
                          {hasDifferentProvider && (
                            <p className="sw-mb-2 sw-body-sm-highlight">
                              {translate('settings.authentication.form.other_provisioning_enabled')}
                            </p>
                          )}
                          {autoDescription}
                        </>
                      ) : (
                        autoFeatureDisabledText
                      )}
                    </div>
                  </div>
                </RadioButton>
                {provisioningType === ProvisioningType.auto && (
                  <div className="sw-ml-6 sw-mt-6">
                    {synchronizationDetails}
                    {onSyncNow && (
                      <div className="sw-mb-4 sw-mt-6">
                        <ButtonPrimary onClick={onSyncNow} disabled={!canSync}>
                          {translate('settings.authentication.github.synchronize_now')}
                        </ButtonPrimary>
                      </div>
                    )}
                    <div className="sw-ml-10 sw-mt-8 sw-max-w-[435px]">{autoSettings}</div>
                  </div>
                )}
                <BasicSeparator className="sw-my-4" />
              </li>
            </ul>
            <div className="sw-flex sw-gap-2 sw-h-8 sw-items-center">
              <ButtonPrimary type="submit" disabled={!hasUnsavedChanges || !canSave}>
                {translate('save')}
              </ButtonPrimary>
              <ButtonSecondary onClick={onCancel} disabled={!hasUnsavedChanges}>
                {translate('cancel')}
              </ButtonSecondary>
              <Spinner loading={!!isLoading} />
              <FlagMessage variant="warning" className="sw-mb-0">
                {hasUnsavedChanges &&
                  !isLoading &&
                  translate('settings.authentication.github.configuration.unsaved_changes')}
              </FlagMessage>
            </div>
          </>
        ) : (
          <FlagMessage className="sw-mt-4" variant="info">
            {disabledConfigText}
          </FlagMessage>
        )}
      </form>
    </div>
  );
}

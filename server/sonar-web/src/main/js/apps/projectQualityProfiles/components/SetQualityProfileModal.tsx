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
  ButtonPrimary,
  FlagMessage,
  InputSelect,
  LightLabel,
  Modal,
  RadioButton,
} from 'design-system';
import * as React from 'react';
import { Profile } from '../../../api/quality-profiles';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import BuiltInQualityProfileBadge from '../../quality-profiles/components/BuiltInQualityProfileBadge';
import { USE_SYSTEM_DEFAULT } from '../constants';
import LanguageProfileSelectOption, { ProfileOption } from './LanguageProfileSelectOption';

export interface SetQualityProfileModalProps {
  availableProfiles: Profile[];
  component: Component;
  currentProfile: Profile;
  onClose: () => void;
  onSubmit: (newKey: string | undefined, oldKey: string) => Promise<void>;
  usesDefault: boolean;
}

export default function SetQualityProfileModal(props: SetQualityProfileModalProps) {
  const { availableProfiles, component, currentProfile, usesDefault } = props;
  const [selected, setSelected] = React.useState(
    usesDefault ? USE_SYSTEM_DEFAULT : currentProfile.key,
  );

  const defaultProfile = availableProfiles.find((p) => p.isDefault);

  if (defaultProfile === undefined) {
    // Cannot be undefined
    return null;
  }

  const header = translateWithParameters(
    'project_quality_profile.change_lang_X_profile',
    currentProfile.languageName,
  );
  const profileOptions: ProfileOption[] = availableProfiles.map((p) => ({
    value: p.key,
    label: p.name,
    language: currentProfile.language,
    isDisabled: p.activeRuleCount === 0,
  }));
  const hasSelectedSysDefault = selected === USE_SYSTEM_DEFAULT;
  const hasChanged = usesDefault ? !hasSelectedSysDefault : selected !== currentProfile.key;
  const needsReanalysis = !component.qualityProfiles?.some((p) =>
    hasSelectedSysDefault ? p.key === defaultProfile.key : p.key === selected,
  );

  const handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    props.onSubmit(hasSelectedSysDefault ? undefined : selected, currentProfile.key);
  };

  const renderForm = (
    <form id="change-quality-profile" onSubmit={handleFormSubmit}>
      <div>
        <RadioButton
          className="sw-mb-4"
          checked={hasSelectedSysDefault}
          onCheck={() => setSelected(USE_SYSTEM_DEFAULT)}
          value={USE_SYSTEM_DEFAULT}
        >
          <div className="sw-ml-2">
            <div>{translate('project_quality_profile.always_use_default')}</div>
            <LightLabel>
              <span>
                {translate('current_noun')}: {defaultProfile?.name}
              </span>
              {defaultProfile?.isBuiltIn && <BuiltInQualityProfileBadge className="sw-ml-2" />}
            </LightLabel>
          </div>
        </RadioButton>

        <RadioButton
          className="sw-mb-2"
          checked={!hasSelectedSysDefault}
          onCheck={(value) => {
            if (hasSelectedSysDefault) {
              setSelected(value);
            }
          }}
          value={currentProfile.key}
        >
          <div className="sw-ml-2">{translate('project_quality_profile.always_use_specific')}</div>
        </RadioButton>

        <InputSelect
          className="sw-ml-8"
          aria-label={translate('project_quality_profile.always_use_specific')}
          isDisabled={hasSelectedSysDefault}
          onChange={({ value }: ProfileOption) => setSelected(value)}
          options={profileOptions}
          components={{
            Option: LanguageProfileSelectOption,
          }}
          value={profileOptions.find(
            (option) => option.value === (!hasSelectedSysDefault ? selected : currentProfile.key),
          )}
        />

        {needsReanalysis && (
          <FlagMessage className="sw-w-full sw-mt-4" variant="warning">
            {translate('project_quality_profile.requires_new_analysis')}
          </FlagMessage>
        )}
      </div>
    </form>
  );

  return (
    <Modal
      onClose={props.onClose}
      headerTitle={header}
      isOverflowVisible
      body={renderForm}
      primaryButton={
        <ButtonPrimary disabled={!hasChanged} form="change-quality-profile" type="submit">
          {translate('save')}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}

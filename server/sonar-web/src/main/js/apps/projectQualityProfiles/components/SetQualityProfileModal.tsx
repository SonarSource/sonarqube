/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Link } from 'react-router';
import { Profile } from '../../../api/quality-profiles';
import DisableableSelectOption from '../../../components/common/DisableableSelectOption';
import { ButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Radio from '../../../components/controls/Radio';
import SelectLegacy from '../../../components/controls/SelectLegacy';
import SimpleModal from '../../../components/controls/SimpleModal';
import { Alert } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../helpers/urls';
import { Component } from '../../../types/types';
import BuiltInQualityProfileBadge from '../../quality-profiles/components/BuiltInQualityProfileBadge';
import { USE_SYSTEM_DEFAULT } from '../constants';

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
    usesDefault ? USE_SYSTEM_DEFAULT : currentProfile.key
  );

  const defaultProfile = availableProfiles.find(p => p.isDefault);

  if (defaultProfile === undefined) {
    // Cannot be undefined
    return null;
  }

  const header = translateWithParameters(
    'project_quality_profile.change_lang_X_profile',
    currentProfile.languageName
  );
  const profileOptions = availableProfiles.map(p => ({
    value: p.key,
    label: p.name,
    disabled: p.activeRuleCount === 0
  }));
  const hasSelectedSysDefault = selected === USE_SYSTEM_DEFAULT;
  const hasChanged = usesDefault ? !hasSelectedSysDefault : selected !== currentProfile.key;
  const needsReanalysis = !component.qualityProfiles?.some(p =>
    hasSelectedSysDefault ? p.key === defaultProfile.key : p.key === selected
  );

  return (
    <SimpleModal
      header={header}
      onClose={props.onClose}
      onSubmit={() =>
        props.onSubmit(hasSelectedSysDefault ? undefined : selected, currentProfile.key)
      }>
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <form onSubmit={onFormSubmit}>
            <div className="modal-body">
              <div className="big-spacer-bottom">
                <Radio
                  className="display-flex-start"
                  checked={hasSelectedSysDefault}
                  disabled={submitting}
                  onCheck={() => setSelected(USE_SYSTEM_DEFAULT)}
                  value={USE_SYSTEM_DEFAULT}>
                  <div className="spacer-left">
                    <div className="little-spacer-bottom">
                      {translate('project_quality_profile.always_use_default')}
                    </div>
                    <div className="display-flex-center">
                      <span className="text-muted spacer-right">{translate('current_noun')}:</span>
                      {defaultProfile.name}
                      {defaultProfile.isBuiltIn && (
                        <BuiltInQualityProfileBadge className="spacer-left" />
                      )}
                    </div>
                  </div>
                </Radio>
              </div>

              <div className="big-spacer-bottom">
                <Radio
                  className="display-flex-start"
                  checked={!hasSelectedSysDefault}
                  disabled={submitting}
                  onCheck={() =>
                    setSelected(!hasSelectedSysDefault ? selected : currentProfile.key)
                  }
                  value={currentProfile.key}>
                  <div className="spacer-left">
                    <div className="little-spacer-bottom">
                      {translate('project_quality_profile.always_use_specific')}
                    </div>
                    <div className="display-flex-center">
                      <SelectLegacy
                        className="abs-width-300"
                        clearable={false}
                        disabled={submitting || hasSelectedSysDefault}
                        onChange={({ value }: { value: string }) => setSelected(value)}
                        options={profileOptions}
                        optionRenderer={option => (
                          <DisableableSelectOption
                            option={option}
                            disabledReason={translate(
                              'project_quality_profile.add_language_modal.no_active_rules'
                            )}
                            disableTooltipOverlay={() => (
                              <>
                                <p>
                                  {translate(
                                    'project_quality_profile.add_language_modal.profile_unavailable_no_active_rules'
                                  )}
                                </p>
                                {option.label && (
                                  <Link
                                    to={getQualityProfileUrl(
                                      option.label,
                                      currentProfile.language
                                    )}>
                                    {translate(
                                      'project_quality_profile.add_language_modal.go_to_profile'
                                    )}
                                  </Link>
                                )}
                              </>
                            )}
                          />
                        )}
                        value={!hasSelectedSysDefault ? selected : currentProfile.key}
                      />
                    </div>
                  </div>
                </Radio>
              </div>

              {needsReanalysis && (
                <Alert variant="warning">
                  {translate('project_quality_profile.requires_new_analysis')}
                </Alert>
              )}
            </div>

            <div className="modal-foot">
              {submitting && <i className="spinner spacer-right" />}
              <SubmitButton disabled={submitting || !hasChanged}>{translate('save')}</SubmitButton>
              <ButtonLink disabled={submitting} onClick={onCloseClick}>
                {translate('cancel')}
              </ButtonLink>
            </div>
          </form>
        </>
      )}
    </SimpleModal>
  );
}

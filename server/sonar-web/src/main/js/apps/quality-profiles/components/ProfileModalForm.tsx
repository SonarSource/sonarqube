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
import { ButtonPrimary, FormField, InputField, Modal } from 'design-system';
import * as React from 'react';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { Profile, ProfileActionModals } from '../types';

export interface ProfileModalFormProps {
  action: ProfileActionModals.Copy | ProfileActionModals.Extend | ProfileActionModals.Rename;
  loading: boolean;
  onClose: () => void;
  onSubmit: (name: string) => void;
  profile: Profile;
}

const LABELS_FOR_ACTION: Dict<{ button: string; header: string }> = {
  [ProfileActionModals.Copy]: { button: 'copy', header: 'quality_profiles.copy_x_title' },
  [ProfileActionModals.Rename]: { button: 'rename', header: 'quality_profiles.rename_x_title' },
  [ProfileActionModals.Extend]: { button: 'extend', header: 'quality_profiles.extend_x_title' },
};

export default function ProfileModalForm(props: ProfileModalFormProps) {
  const { action, loading, profile } = props;
  const [name, setName] = React.useState('');

  const submitDisabled = loading || !name || name === profile.name;
  const labels = LABELS_FOR_ACTION[action];

  return (
    <Modal
      headerTitle={translateWithParameters(labels.header, profile.name, profile.languageName)}
      onClose={props.onClose}
      loading={loading}
      body={
        <>
          {action === ProfileActionModals.Copy && (
            <p className="sw-mb-8">
              {translateWithParameters('quality_profiles.copy_help', profile.name)}
            </p>
          )}
          {action === ProfileActionModals.Extend && (
            <p className="sw-mb-8">
              {translateWithParameters('quality_profiles.extend_help', profile.name)}
            </p>
          )}

          <MandatoryFieldsExplanation />

          <FormField
            className="sw-mt-2"
            htmlFor="quality-profile-new-name"
            label={translate('quality_profiles.new_name')}
            required
          >
            <InputField
              id="quality-profile-new-name"
              name="name"
              onChange={(event) => setName(event.target.value)}
              required
              size="full"
              type="text"
              value={name}
            />
          </FormField>
        </>
      }
      primaryButton={
        <ButtonPrimary
          onClick={() => {
            if (name) {
              props.onSubmit(name);
            }
          }}
          disabled={submitDisabled}
        >
          {translate(labels.button)}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}

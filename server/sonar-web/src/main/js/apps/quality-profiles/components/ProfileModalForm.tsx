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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { FlagMessage, FormField, InputField, Modal } from 'design-system';
import * as React from 'react';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import useKeyDown from '../../../hooks/useKeydown';
import { useProfileInheritanceQuery } from '../../../queries/quality-profiles';
import { Dict } from '../../../types/types';
import { Profile, ProfileActionModals } from '../types';

export interface ProfileModalFormProps {
  action: ProfileActionModals.Copy | ProfileActionModals.Extend | ProfileActionModals.Rename;
  loading: boolean;
  onClose: () => void;
  onSubmit: (name: string) => void;
  profile: Profile;
  organization: string;
}

const LABELS_FOR_ACTION: Dict<{ button: string; header: string }> = {
  [ProfileActionModals.Copy]: { button: 'copy', header: 'quality_profiles.copy_x_title' },
  [ProfileActionModals.Rename]: { button: 'rename', header: 'quality_profiles.rename_x_title' },
  [ProfileActionModals.Extend]: { button: 'extend', header: 'quality_profiles.extend_x_title' },
};

export default function ProfileModalForm(props: ProfileModalFormProps) {
  const { action, loading, profile, onSubmit } = props;
  const [name, setName] = React.useState('');

  const submitDisabled = loading || !name || name === profile.name;
  const labels = LABELS_FOR_ACTION[action];

  const { data: { ancestors } = {} } = useProfileInheritanceQuery(props.profile);

  const handleSubmit = React.useCallback(() => {
    if (name) {
      onSubmit(name);
    }
  }, [name, onSubmit]);

  useKeyDown(handleSubmit, [KeyboardKeys.Enter]);

  const extendsBuiltIn = ancestors?.some((profile) => profile.isBuiltIn);
  const showBuiltInWarning =
    (action === ProfileActionModals.Copy && !extendsBuiltIn) ||
    (action === ProfileActionModals.Extend && !profile.isBuiltIn && !extendsBuiltIn);

  return (
    <Modal
      headerTitle={translateWithParameters(labels.header, profile.name, profile.languageName)}
      onClose={props.onClose}
      isOverflowVisible
      loading={loading}
      body={
        <>
          {showBuiltInWarning && (
            <FlagMessage variant="info" className="sw-mb-4">
              <div className="sw-flex sw-flex-col">
                {translate('quality_profiles.no_built_in_updates_warning.new_profile')}
                <span className="sw-mt-2">
                  {translate('quality_profiles.no_built_in_updates_warning.new_profile.2')}
                </span>
              </div>
            </FlagMessage>
          )}

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
              onChange={(event: React.ChangeEvent<HTMLInputElement>) => setName(event.target.value)}
              required
              size="full"
              type="text"
              value={name}
            />
          </FormField>
        </>
      }
      primaryButton={
        <Button onClick={handleSubmit} isDisabled={submitDisabled} variety={ButtonVariety.Primary}>
          {translate(labels.button)}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}

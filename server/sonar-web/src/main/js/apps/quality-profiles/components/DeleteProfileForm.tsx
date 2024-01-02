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
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Modal from '../../../components/controls/Modal';
import { Alert } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Profile } from '../types';

export interface DeleteProfileFormProps {
  loading: boolean;
  onClose: () => void;
  onDelete: () => void;
  profile: Profile;
}

export default function DeleteProfileForm(props: DeleteProfileFormProps) {
  const { loading, profile } = props;
  const header = translate('quality_profiles.delete_confirm_title');

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <form
        onSubmit={(e: React.SyntheticEvent<HTMLFormElement>) => {
          e.preventDefault();
          props.onDelete();
        }}
      >
        <div className="modal-head">
          <h2>{header}</h2>
        </div>
        <div className="modal-body">
          {profile.childrenCount > 0 ? (
            <div>
              <Alert variant="warning">
                {translate('quality_profiles.this_profile_has_descendants')}
              </Alert>
              <p>
                {translateWithParameters(
                  'quality_profiles.are_you_sure_want_delete_profile_x_and_descendants',
                  profile.name,
                  profile.languageName
                )}
              </p>
            </div>
          ) : (
            <p>
              {translateWithParameters(
                'quality_profiles.are_you_sure_want_delete_profile_x',
                profile.name,
                profile.languageName
              )}
            </p>
          )}
        </div>
        <div className="modal-foot">
          {loading && <i className="spinner spacer-right" />}
          <SubmitButton className="button-red" disabled={loading}>
            {translate('delete')}
          </SubmitButton>
          <ResetButtonLink onClick={props.onClose}>{translate('cancel')}</ResetButtonLink>
        </div>
      </form>
    </Modal>
  );
}

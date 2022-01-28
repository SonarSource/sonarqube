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
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Modal from '../../../components/controls/Modal';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Profile } from '../types';

export interface ProfileModalFormProps {
  btnLabelKey: string;
  headerKey: string;
  loading: boolean;
  onClose: () => void;
  onSubmit: (name: string) => void;
  profile: Profile;
}

export default function ProfileModalForm(props: ProfileModalFormProps) {
  const { btnLabelKey, headerKey, loading, profile } = props;
  const [name, setName] = React.useState<string | undefined>(undefined);

  const submitDisabled = loading || !name || name === profile.name;
  const header = translateWithParameters(headerKey, profile.name, profile.languageName);

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose} size="small">
      <form
        onSubmit={(e: React.SyntheticEvent<HTMLFormElement>) => {
          e.preventDefault();
          if (name) {
            props.onSubmit(name);
          }
        }}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>
        <div className="modal-body">
          <MandatoryFieldsExplanation className="modal-field" />
          <div className="modal-field">
            <label htmlFor="profile-name">
              {translate('quality_profiles.new_name')}
              <MandatoryFieldMarker />
            </label>
            <input
              autoFocus={true}
              id="profile-name"
              maxLength={100}
              name="name"
              onChange={(e: React.SyntheticEvent<HTMLInputElement>) => {
                setName(e.currentTarget.value);
              }}
              required={true}
              size={50}
              type="text"
              value={name ?? profile.name}
            />
          </div>
        </div>
        <div className="modal-foot">
          {loading && <i className="spinner spacer-right" />}
          <SubmitButton disabled={submitDisabled}>{translate(btnLabelKey)}</SubmitButton>
          <ResetButtonLink onClick={props.onClose}>{translate('cancel')}</ResetButtonLink>
        </div>
      </form>
    </Modal>
  );
}

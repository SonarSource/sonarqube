/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import SimpleModal from '../../../components/controls/SimpleModal';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  onCancel: () => void;
  onSubmit: () => void;
}

export default function RemoveExtendedDescriptionModal({ onCancel, onSubmit }: Props) {
  const header = translate('coding_rules.remove_extended_description');
  return (
    <SimpleModal header={header} onClose={onCancel} onSubmit={onSubmit}>
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <form onSubmit={onFormSubmit}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>

          <div className="modal-body">
            {translate('coding_rules.remove_extended_description.confirm')}
          </div>

          <footer className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <SubmitButton
              className="button-red"
              disabled={submitting}
              id="coding-rules-detail-extend-description-remove-submit">
              {translate('remove')}
            </SubmitButton>
            <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      )}
    </SimpleModal>
  );
}

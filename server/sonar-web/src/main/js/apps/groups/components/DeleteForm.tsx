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
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';

interface Props {
  group: T.Group;
  onClose: () => void;
  onSubmit: () => Promise<void>;
}

export default function DeleteForm({ group, onClose, onSubmit }: Props) {
  const header = translate('groups.delete_group');

  return (
    <SimpleModal header={header} onClose={onClose} onSubmit={onSubmit}>
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <form onSubmit={onFormSubmit}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>

          <div className="modal-body">
            {translateWithParameters('groups.delete_group.confirmation', group.name)}
          </div>

          <footer className="modal-foot">
            <DeferredSpinner className="spacer-right" loading={submitting} />
            <SubmitButton className="button-red" disabled={submitting}>
              {translate('delete')}
            </SubmitButton>
            <ResetButtonLink disabled={submitting} onClick={onCloseClick}>
              {translate('cancel')}
            </ResetButtonLink>
          </footer>
        </form>
      )}
    </SimpleModal>
  );
}

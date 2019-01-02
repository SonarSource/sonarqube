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
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onSubmit: () => Promise<void>;
  webhook: T.Webhook;
}

export default function DeleteWebhookForm({ onClose, onSubmit, webhook }: Props) {
  const header = translate('webhooks.delete');

  return (
    <SimpleModal header={header} onClose={onClose} onSubmit={onSubmit}>
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <form onSubmit={onFormSubmit}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>

          <div className="modal-body">
            {translateWithParameters('webhooks.delete.confirm', webhook.name)}
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

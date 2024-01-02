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
import { FormattedMessage } from 'react-intl';
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import { RestUserDetailed } from '../../../types/users';
import TokensForm from './TokensForm';

interface Props {
  user: RestUserDetailed;
  onClose: () => void;
}

export default function TokensFormModal(props: Props) {
  const { user } = props;

  return (
    <Modal size="large" contentLabel={translate('users.tokens')} onRequestClose={props.onClose}>
      <header className="modal-head">
        <h2>
          <FormattedMessage
            defaultMessage={translate('users.user_X_tokens')}
            id="users.user_X_tokens"
            values={{ user: <em>{user.name}</em> }}
          />
        </h2>
      </header>
      <div className="modal-body modal-container">
        <TokensForm deleteConfirmation="inline" login={user.login} displayTokenTypeInput={false} />
      </div>
      <footer className="modal-foot">
        <ResetButtonLink onClick={props.onClose}>{translate('done')}</ResetButtonLink>
      </footer>
    </Modal>
  );
}

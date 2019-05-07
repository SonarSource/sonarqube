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
import TokensForm from './TokensForm';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';
import { ResetButtonLink } from '../../../components/ui/buttons';

interface Props {
  user: T.User;
  onClose: () => void;
  updateTokensCount: (login: string, tokensCount: number) => void;
}

export default function TokensFormModal(props: Props) {
  const header = translate('users.tokens');
  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <header className="modal-head">
        <h2>{header}</h2>
      </header>
      <div className="modal-body modal-container">
        <TokensForm login={props.user.login} updateTokensCount={props.updateTokensCount} />
      </div>
      <footer className="modal-foot">
        <ResetButtonLink onClick={props.onClose}>{translate('Done')}</ResetButtonLink>
      </footer>
    </Modal>
  );
}

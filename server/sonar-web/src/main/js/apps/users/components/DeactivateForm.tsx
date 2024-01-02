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
import DocLink from '../../../components/common/DocLink';
import Checkbox from '../../../components/controls/Checkbox';
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import { Alert } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useDeactivateUserMutation } from '../../../queries/users';
import { RestUserDetailed } from '../../../types/users';

export interface Props {
  onClose: () => void;
  user: RestUserDetailed;
}

export default function DeactivateForm(props: Props) {
  const { user } = props;
  const [anonymize, setAnonymize] = React.useState(false);

  const { mutate: deactivateUser, isLoading } = useDeactivateUserMutation();

  const handleDeactivate = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    deactivateUser(
      { id: user.id, anonymize },
      {
        onSuccess: props.onClose,
      },
    );
  };

  const header = translate('users.deactivate_user');
  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <form autoComplete="off" id="deactivate-user-form" onSubmit={handleDeactivate}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <div className="modal-body display-flex-column">
          {translateWithParameters('users.deactivate_user.confirmation', user.name, user.login)}
          <Checkbox
            id="delete-user"
            className="big-spacer-top"
            checked={anonymize}
            onCheck={(checked) => setAnonymize(checked)}
          >
            <label className="little-spacer-left" htmlFor="delete-user">
              {translate('users.delete_user')}
            </label>
          </Checkbox>
          {anonymize && (
            <Alert variant="warning" className="big-spacer-top">
              <FormattedMessage
                defaultMessage={translate('users.delete_user.help')}
                id="delete-user-warning"
                values={{
                  link: (
                    <DocLink to="/instance-administration/authentication/overview/">
                      {translate('users.delete_user.help.link')}
                    </DocLink>
                  ),
                }}
              />
            </Alert>
          )}
        </div>
        <footer className="modal-foot">
          {isLoading && <i className="spinner spacer-right" />}
          <SubmitButton className="js-confirm button-red" disabled={isLoading}>
            {translate('users.deactivate')}
          </SubmitButton>
          <ResetButtonLink className="js-modal-close" onClick={props.onClose}>
            {translate('cancel')}
          </ResetButtonLink>
        </footer>
      </form>
    </Modal>
  );
}

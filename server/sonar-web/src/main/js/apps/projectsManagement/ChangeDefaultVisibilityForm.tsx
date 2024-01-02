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
import { Button, ResetButtonLink } from '../../components/controls/buttons';
import Modal from '../../components/controls/Modal';
import Radio from '../../components/controls/Radio';
import { Alert } from '../../components/ui/Alert';
import { translate } from '../../helpers/l10n';
import { Visibility } from '../../types/types';

export interface Props {
  defaultVisibility: Visibility;
  onClose: () => void;
  onConfirm: (visiblity: Visibility) => void;
}

interface State {
  visibility: Visibility;
}

export default class ChangeDefaultVisibilityForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { visibility: props.defaultVisibility };
  }

  handleConfirmClick = () => {
    this.props.onConfirm(this.state.visibility);
    this.props.onClose();
  };

  handleVisibilityChange = (visibility: Visibility) => {
    this.setState({ visibility });
  };

  render() {
    return (
      <Modal contentLabel="modal form" onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{translate('settings.projects.change_visibility_form.header')}</h2>
        </header>

        <div className="modal-body">
          {['public', 'private'].map((visibility) => (
            <div className="big-spacer-bottom" key={visibility}>
              <Radio
                value={visibility}
                checked={this.state.visibility === visibility}
                onCheck={this.handleVisibilityChange}
              >
                <div>
                  {translate('visibility', visibility)}
                  <p className="text-muted spacer-top">
                    {translate('visibility', visibility, 'description.short')}
                  </p>
                </div>
              </Radio>
            </div>
          ))}

          <Alert variant="warning">
            {translate('settings.projects.change_visibility_form.warning')}
          </Alert>
        </div>

        <footer className="modal-foot">
          <Button className="js-confirm" onClick={this.handleConfirmClick}>
            {translate('settings.projects.change_visibility_form.submit')}
          </Button>
          <ResetButtonLink className="js-modal-close" onClick={this.props.onClose}>
            {translate('cancel')}
          </ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}

/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { ResetButtonLink, SubmitButton } from '../../components/controls/buttons';
import SimpleModal from '../../components/controls/SimpleModal';
import DeferredSpinner from '../../components/ui/DeferredSpinner';
import { translate } from '../../helpers/l10n';
import { Application } from '../../types/application';

interface Props {
  header: string;
  onClose: () => void;
  onEdit: (name: string, description: string) => Promise<void>;
  application: Application;
}

interface State {
  description: string;
  name: string;
}

export default class EditForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      description: props.application.description || '',
      name: props.application.name
    };
  }

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleFormSubmit = async () => {
    await this.props.onEdit(this.state.name, this.state.description);
    this.props.onClose();
  };

  render() {
    return (
      <SimpleModal
        header={this.props.header}
        onClose={this.props.onClose}
        onSubmit={this.handleFormSubmit}
        size="small">
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form onSubmit={onFormSubmit}>
            <div className="modal-head">
              <h2>{this.props.header}</h2>
            </div>

            <div className="modal-body">
              <div className="modal-field">
                <label htmlFor="view-edit-name">{translate('name')}</label>
                <input
                  autoFocus={true}
                  id="view-edit-name"
                  maxLength={100}
                  name="name"
                  onChange={this.handleNameChange}
                  size={50}
                  type="text"
                  value={this.state.name}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="view-edit-description">{translate('description')}</label>
                <textarea
                  id="view-edit-description"
                  name="description"
                  onChange={this.handleDescriptionChange}
                  value={this.state.description}
                />
              </div>
            </div>

            <div className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton disabled={submitting || !this.state.name.length}>
                {translate('save')}
              </SubmitButton>
              <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
            </div>
          </form>
        )}
      </SimpleModal>
    );
  }
}

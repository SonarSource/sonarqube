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
import { translate } from 'sonar-ui-common/helpers/l10n';

interface Props {
  confirmButtonText: string;
  group?: T.Group;
  header: string;
  onClose: () => void;
  onSubmit: (data: { description: string; name: string }) => Promise<void>;
}

interface State {
  description: string;
  name: string;
}

export default class Form extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      description: (props.group && props.group.description) || '',
      name: (props.group && props.group.name) || ''
    };
  }

  handleSubmit = () => {
    return this.props
      .onSubmit({ description: this.state.description, name: this.state.name })
      .then(this.props.onClose);
  };

  handleDescriptionChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  render() {
    return (
      <SimpleModal
        header={this.props.header}
        onClose={this.props.onClose}
        onSubmit={this.handleSubmit}
        size="small">
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form onSubmit={onFormSubmit}>
            <header className="modal-head">
              <h2>{this.props.header}</h2>
            </header>

            <div className="modal-body">
              <div className="modal-field">
                <label htmlFor="create-group-name">
                  {translate('name')}
                  <em className="mandatory">*</em>
                </label>
                <input
                  autoFocus={true}
                  id="create-group-name"
                  maxLength={255}
                  name="name"
                  onChange={this.handleNameChange}
                  required={true}
                  size={50}
                  type="text"
                  value={this.state.name}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="create-group-description">{translate('description')}</label>
                <textarea
                  id="create-group-description"
                  name="description"
                  onChange={this.handleDescriptionChange}
                  value={this.state.description}
                />
              </div>
            </div>

            <footer className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton disabled={submitting}>{this.props.confirmButtonText}</SubmitButton>
              <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
            </footer>
          </form>
        )}
      </SimpleModal>
    );
  }
}

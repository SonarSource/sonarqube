/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

interface Commons {
  desc?: string;
  description?: string;
  key: string;
  name: string;
}

interface Props<T extends Commons> {
  header: string;
  onChange: (key: string, name: string, description: string) => Promise<void>;
  onClose: () => void;
  onEdit: (key: string, name: string, description: string) => void;
  application: T;
}

interface State {
  description: string;
  name: string;
}

export default class EditForm<T extends Commons> extends React.PureComponent<Props<T>, State> {
  constructor(props: Props<T>) {
    super(props);
    this.state = {
      description: props.application.desc || props.application.description || '',
      name: props.application.name
    };
  }

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleFormSubmit = () => {
    return this.props
      .onChange(this.props.application.key, this.state.name, this.state.description)
      .then(() => {
        this.props.onEdit(this.props.application.key, this.state.name, this.state.description);
        this.props.onClose();
      });
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

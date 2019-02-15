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
import DeferredSpinner from '../../components/common/DeferredSpinner';
import SimpleModal from '../../components/controls/SimpleModal';
import { SubmitButton, ResetButtonLink } from '../../components/ui/buttons';
import { translate } from '../../helpers/l10n';

interface Props {
  onClose: () => void;
  onSubmit: (name: string, url: string) => Promise<void>;
}

interface State {
  name: string;
  url: string;
}

export default class CreationModal extends React.PureComponent<Props, State> {
  state: State = { name: '', url: '' };

  handleSubmit = () => {
    return this.props.onSubmit(this.state.name, this.state.url).then(this.props.onClose);
  };

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleUrlChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ url: event.currentTarget.value });
  };

  render() {
    const header = translate('project_links.create_new_project_link');

    return (
      <SimpleModal
        header={header}
        onClose={this.props.onClose}
        onSubmit={this.handleSubmit}
        size="small">
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form onSubmit={onFormSubmit}>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>

            <div className="modal-body">
              <div className="modal-field">
                <label htmlFor="create-link-name">
                  {translate('project_links.name')}
                  <em className="mandatory">*</em>
                </label>
                <input
                  autoFocus={true}
                  id="create-link-name"
                  maxLength={128}
                  name="name"
                  onChange={this.handleNameChange}
                  required={true}
                  type="text"
                  value={this.state.name}
                />
              </div>

              <div className="modal-field">
                <label htmlFor="create-link-url">
                  {translate('project_links.url')}
                  <em className="mandatory">*</em>
                </label>
                <input
                  id="create-link-url"
                  maxLength={128}
                  name="url"
                  onChange={this.handleUrlChange}
                  required={true}
                  type="text"
                  value={this.state.url}
                />
              </div>
            </div>

            <footer className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton disabled={submitting} id="create-link-confirm">
                {translate('create')}
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
}

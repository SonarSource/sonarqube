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
  header: string;
  onClose: () => void;
  onSubmit: (data: {
    description: string;
    name: string;
    projectKeyPattern: string;
  }) => Promise<void>;
  permissionTemplate?: { description?: string; name: string; projectKeyPattern?: string };
}

interface State {
  description: string;
  name: string;
  projectKeyPattern: string;
}

export default class Form extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      description: (props.permissionTemplate && props.permissionTemplate.description) || '',
      name: (props.permissionTemplate && props.permissionTemplate.name) || '',
      projectKeyPattern:
        (props.permissionTemplate && props.permissionTemplate.projectKeyPattern) || ''
    };
  }

  handleSubmit = () => {
    return this.props
      .onSubmit({
        description: this.state.description,
        name: this.state.name,
        projectKeyPattern: this.state.projectKeyPattern
      })
      .then(this.props.onClose);
  };

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleProjectKeyPatternChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ projectKeyPattern: event.currentTarget.value });
  };

  render() {
    return (
      <SimpleModal
        header={this.props.header}
        onClose={this.props.onClose}
        onSubmit={this.handleSubmit}
        size="small">
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form id="permission-template-form" onSubmit={onFormSubmit}>
            <header className="modal-head">
              <h2>{this.props.header}</h2>
            </header>

            <div className="modal-body">
              <div className="modal-field">
                <label htmlFor="permission-template-name">
                  {translate('name')}
                  <em className="mandatory">*</em>
                </label>
                <input
                  autoFocus={true}
                  id="permission-template-name"
                  maxLength={256}
                  name="name"
                  onChange={this.handleNameChange}
                  required={true}
                  type="text"
                  value={this.state.name}
                />
                <div className="modal-field-description">{translate('should_be_unique')}</div>
              </div>

              <div className="modal-field">
                <label htmlFor="permission-template-description">{translate('description')}</label>
                <textarea
                  id="permission-template-description"
                  name="description"
                  onChange={this.handleDescriptionChange}
                  value={this.state.description}
                />
              </div>

              <div className="modal-field">
                <label htmlFor="permission-template-project-key-pattern">
                  {translate('permission_template.key_pattern')}
                </label>
                <input
                  id="permission-template-project-key-pattern"
                  maxLength={500}
                  name="projectKeyPattern"
                  onChange={this.handleProjectKeyPatternChange}
                  type="text"
                  value={this.state.projectKeyPattern}
                />
                <div className="modal-field-description">
                  {translate('permission_template.key_pattern.description')}
                </div>
              </div>
            </div>

            <footer className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton disabled={submitting} id="permission-template-submit">
                {this.props.confirmButtonText}
              </SubmitButton>
              <ResetButtonLink
                disabled={submitting}
                id="permission-template-cancel"
                onClick={onCloseClick}>
                {translate('cancel')}
              </ResetButtonLink>
            </footer>
          </form>
        )}
      </SimpleModal>
    );
  }
}

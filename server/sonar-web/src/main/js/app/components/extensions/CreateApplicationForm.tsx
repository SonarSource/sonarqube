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
import { createApplication } from '../../../api/application';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Radio from '../../../components/controls/Radio';
import SimpleModal from '../../../components/controls/SimpleModal';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { ComponentQualifier, Visibility } from '../../../types/component';

interface Props {
  onClose: () => void;
  onCreate: (application: { key: string; qualifier: ComponentQualifier }) => Promise<void>;
}

interface State {
  description: string;
  key: string;
  name: string;
  visibility: Visibility;
}

export default class CreateApplicationForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      description: '',
      key: '',
      name: '',
      visibility: Visibility.Public,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleKeyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ key: event.currentTarget.value });
  };

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleVisibilityChange = (visibility: Visibility) => {
    this.setState({ visibility });
  };

  handleFormSubmit = () => {
    const { name, description, key, visibility } = this.state;
    return createApplication(name, description, key.length > 0 ? key : undefined, visibility).then(
      ({ application }) => {
        if (this.mounted) {
          this.props.onCreate({
            key: application.key,
            qualifier: ComponentQualifier.Application,
          });
        }
      }
    );
  };

  render() {
    const { name, description, key, visibility } = this.state;
    const header = translate('qualifiers.create.APP');
    const submitDisabled = !this.state.name.length;

    return (
      <SimpleModal
        header={header}
        onClose={this.props.onClose}
        onSubmit={this.handleFormSubmit}
        size="small"
      >
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form className="views-form" onSubmit={onFormSubmit}>
            <div className="modal-head">
              <h2>{header}</h2>
            </div>

            <div className="modal-body">
              <MandatoryFieldsExplanation className="modal-field" />

              <div className="modal-field">
                <label htmlFor="view-edit-name">
                  {translate('name')}
                  <MandatoryFieldMarker />
                </label>
                <input
                  autoFocus={true}
                  id="view-edit-name"
                  maxLength={100}
                  name="name"
                  onChange={this.handleNameChange}
                  size={50}
                  type="text"
                  value={name}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="view-edit-description">{translate('description')}</label>
                <textarea
                  id="view-edit-description"
                  name="description"
                  onChange={this.handleDescriptionChange}
                  value={description}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="view-edit-key">{translate('key')}</label>
                <input
                  autoComplete="off"
                  id="view-edit-key"
                  maxLength={256}
                  name="key"
                  onChange={this.handleKeyChange}
                  size={256}
                  type="text"
                  value={key}
                />
                <p className="modal-field-description">
                  {translate('onboarding.create_application.key.description')}
                </p>
              </div>

              <div className="modal-field">
                <label>{translate('visibility')}</label>
                <div className="little-spacer-top">
                  {[Visibility.Public, Visibility.Private].map((v) => (
                    <Radio
                      className={`big-spacer-right visibility-${v}`}
                      key={v}
                      checked={visibility === v}
                      value={v}
                      onCheck={this.handleVisibilityChange}
                    >
                      {translate('visibility', v)}
                    </Radio>
                  ))}
                </div>
              </div>
            </div>

            <div className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton disabled={submitting || submitDisabled}>
                {translate('create')}
              </SubmitButton>
              <ResetButtonLink
                className="js-modal-close"
                id="view-edit-cancel"
                onClick={onCloseClick}
              >
                {translate('cancel')}
              </ResetButtonLink>
            </div>
          </form>
        )}
      </SimpleModal>
    );
  }
}

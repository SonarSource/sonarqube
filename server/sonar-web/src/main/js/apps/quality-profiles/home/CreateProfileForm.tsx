/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { sortBy } from 'lodash';
import { getImporters, createQualityProfile } from '../../../api/quality-profiles';
import Modal from '../../../components/controls/Modal';
import Select from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';

interface Props {
  languages: Array<{ key: string; name: string }>;
  onClose: () => void;
  onCreate: Function;
  onRequestFail: (reasong: any) => void;
  organization: string | null;
}

interface State {
  importers: Array<{ key: string; languages: Array<string>; name: string }>;
  language?: string;
  loading: boolean;
  name: string;
  preloading: boolean;
}

export default class CreateProfileForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { importers: [], loading: false, name: '', preloading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchImporters();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchImporters() {
    getImporters().then(
      (importers: Array<{ key: string; languages: Array<string>; name: string }>) => {
        if (this.mounted) {
          this.setState({ importers, preloading: false });
        }
      }
    );
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleLanguageChange = (option: { value: string }) => {
    this.setState({ language: option.value });
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    this.setState({ loading: true });

    const data = new FormData(event.currentTarget);
    if (this.props.organization) {
      data.append('organization', this.props.organization);
    }

    createQualityProfile(data).then(
      (response: any) => this.props.onCreate(response.profile),
      (error: any) => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
        this.props.onRequestFail(error);
      }
    );
  };

  render() {
    const header = translate('quality_profiles.new_profile');

    const languages = sortBy(this.props.languages, 'name');
    const selectedLanguage = this.state.language || languages[0].key;
    const importers = this.state.importers.filter(importer =>
      importer.languages.includes(selectedLanguage)
    );

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="create-profile-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          {this.state.preloading ? (
            <div className="modal-body">
              <i className="spinner" />
            </div>
          ) : (
            <div className="modal-body">
              <div className="modal-field">
                <label htmlFor="create-profile-name">
                  {translate('name')}
                  <em className="mandatory">*</em>
                </label>
                <input
                  autoFocus={true}
                  id="create-profile-name"
                  maxLength={100}
                  name="name"
                  onChange={this.handleNameChange}
                  required={true}
                  size={50}
                  type="text"
                  value={this.state.name}
                />
              </div>
              <div className="modal-field spacer-bottom">
                <label htmlFor="create-profile-language">
                  {translate('language')}
                  <em className="mandatory">*</em>
                </label>
                <Select
                  clearable={false}
                  name="language"
                  onChange={this.handleLanguageChange}
                  options={languages.map(language => ({
                    label: language.name,
                    value: language.key
                  }))}
                  value={selectedLanguage}
                />
              </div>
              {importers.map(importer => (
                <div
                  className="modal-field spacer-bottom js-importer"
                  data-key={importer.key}
                  key={importer.key}>
                  <label htmlFor={'create-profile-form-backup-' + importer.key}>
                    {importer.name}
                  </label>
                  <input
                    id={'create-profile-form-backup-' + importer.key}
                    name={'backup_' + importer.key}
                    type="file"
                  />
                  <p className="note">
                    {translate('quality_profiles.optional_configuration_file')}
                  </p>
                </div>
              ))}
              {/* drop me when we stop supporting ie11 */}
              <input type="hidden" name="hello-ie11" value="" />
            </div>
          )}

          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            {!this.state.preloading && (
              <button disabled={this.state.loading} id="create-profile-submit">
                {translate('create')}
              </button>
            )}
            <a href="#" id="create-profile-cancel" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </div>
        </form>
      </Modal>
    );
  }
}

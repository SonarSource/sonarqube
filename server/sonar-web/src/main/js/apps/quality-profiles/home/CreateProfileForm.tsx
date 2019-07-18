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
import { sortBy } from 'lodash';
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import Select from 'sonar-ui-common/components/controls/Select';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  changeProfileParent,
  createQualityProfile,
  getImporters
} from '../../../api/quality-profiles';
import { Profile } from '../types';

interface Props {
  languages: Array<{ key: string; name: string }>;
  onClose: () => void;
  onCreate: Function;
  organization: string | null;
  profiles: Profile[];
}

interface State {
  importers: Array<{ key: string; languages: Array<string>; name: string }>;
  language?: string;
  loading: boolean;
  name: string;
  parent?: string;
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
      importers => {
        if (this.mounted) {
          this.setState({ importers, preloading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ preloading: false });
        }
      }
    );
  }

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleLanguageChange = (option: { value: string }) => {
    this.setState({ language: option.value });
  };

  handleParentChange = (option: { value: string } | null) => {
    this.setState({ parent: option ? option.value : undefined });
  };

  handleFormSubmit = async (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    this.setState({ loading: true });

    const data = new FormData(event.currentTarget);
    if (this.props.organization) {
      data.append('organization', this.props.organization);
    }

    try {
      const { profile } = await createQualityProfile(data);
      if (this.state.parent) {
        await changeProfileParent(profile.key, this.state.parent);
      }
      this.props.onCreate(profile);
    } finally {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    }
  };

  render() {
    const header = translate('quality_profiles.new_profile');
    const languages = sortBy(this.props.languages, 'name');
    let profiles: Array<{ label: string; value: string }> = [];

    const selectedLanguage = this.state.language || languages[0].key;
    const importers = this.state.importers.filter(importer =>
      importer.languages.includes(selectedLanguage)
    );

    if (selectedLanguage) {
      const languageProfiles = this.props.profiles.filter(p => p.language === selectedLanguage);
      profiles = [
        { label: translate('none'), value: '' },
        ...sortBy(languageProfiles, 'name').map(profile => ({
          label: profile.isBuiltIn
            ? `${profile.name} (${translate('quality_profiles.built_in')})`
            : profile.name,
          value: profile.key
        }))
      ];
    }

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose} size="small">
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
              <div className="modal-field">
                <label htmlFor="create-profile-language">
                  {translate('language')}
                  <em className="mandatory">*</em>
                </label>
                <Select
                  clearable={false}
                  id="create-profile-language"
                  name="language"
                  onChange={this.handleLanguageChange}
                  options={languages.map(language => ({
                    label: language.name,
                    value: language.key
                  }))}
                  value={selectedLanguage}
                />
              </div>
              {selectedLanguage && profiles.length && (
                <div className="modal-field">
                  <label htmlFor="create-profile-parent">
                    {translate('quality_profiles.parent')}
                  </label>
                  <Select
                    clearable={true}
                    id="create-profile-parent"
                    name="parentKey"
                    onChange={this.handleParentChange}
                    options={profiles}
                    value={this.state.parent || ''}
                  />
                </div>
              )}
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
              <input name="hello-ie11" type="hidden" value="" />
            </div>
          )}

          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            {!this.state.preloading && (
              <SubmitButton disabled={this.state.loading} id="create-profile-submit">
                {translate('create')}
              </SubmitButton>
            )}
            <ResetButtonLink id="create-profile-cancel" onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}

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
import { sortBy } from 'lodash';
import * as React from 'react';
import {
  changeProfileParent,
  copyProfile,
  createQualityProfile,
  getImporters,
} from '../../../api/quality-profiles';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Modal from '../../../components/controls/Modal';
import RadioCard from '../../../components/controls/RadioCard';
import Select from '../../../components/controls/Select';
import ValidationInput from '../../../components/controls/ValidationInput';
import { Location } from '../../../components/hoc/withRouter';
import CopyQualityProfileIcon from '../../../components/icons/CopyQualityProfileIcon';
import ExtendQualityProfileIcon from '../../../components/icons/ExtendQualityProfileIcon';
import NewQualityProfileIcon from '../../../components/icons/NewQualityProfileIcon';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { parseAsOptionalString } from '../../../helpers/query';
import { Profile, ProfileActionModals } from '../types';

interface Props {
  languages: Array<{ key: string; name: string }>;
  location: Location;
  onClose: () => void;
  onCreate: Function;
  profiles: Profile[];
}

interface State {
  importers: Array<{ key: string; languages: Array<string>; name: string }>;
  action?: ProfileActionModals.Copy | ProfileActionModals.Extend;
  language?: string;
  loading: boolean;
  name: string;
  profile?: string;
  preloading: boolean;
  isValidName?: boolean;
  isValidProflie?: boolean;
  isValidLanguage?: boolean;
}

export default class CreateProfileForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    importers: [],
    loading: false,
    name: '',
    preloading: true,
    action: ProfileActionModals.Extend,
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchImporters();
    const languageQueryFilter = parseAsOptionalString(this.props.location.query.language);
    if (languageQueryFilter !== undefined) {
      this.setState({ language: languageQueryFilter, isValidLanguage: true });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchImporters() {
    getImporters().then(
      (importers) => {
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

  handleSelectExtend = () => {
    this.setState({ action: ProfileActionModals.Extend });
  };

  handleSelectCopy = () => {
    this.setState({ action: ProfileActionModals.Copy });
  };

  handleSelectBlank = () => {
    this.setState({ action: undefined });
  };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({
      name: event.currentTarget.value,
      isValidName: event.currentTarget.value.length > 0,
    });
  };

  handleLanguageChange = (option: { value: string }) => {
    this.setState({ language: option.value, isValidLanguage: true });
  };

  handleQualityProfileChange = (option: { value: string } | null) => {
    this.setState({ profile: option ? option.value : undefined, isValidProflie: option !== null });
  };

  handleFormSubmit = async (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    this.setState({ loading: true });
    const { action, language, name, profile: parent } = this.state;

    try {
      if (action === ProfileActionModals.Copy && parent && name) {
        const profile = await copyProfile(parent, name);
        this.props.onCreate(profile);
      } else if (action === ProfileActionModals.Extend) {
        const { profile } = await createQualityProfile({ language, name });

        const parentProfile = this.props.profiles.find((p) => p.key === parent);
        if (parentProfile) {
          await changeProfileParent(profile, parentProfile);
        }

        this.props.onCreate(profile);
      } else {
        const data = new FormData(event.currentTarget);
        const { profile } = await createQualityProfile(data);
        this.props.onCreate(profile);
      }
    } finally {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    }
  };

  canSubmit() {
    const { action, isValidName, isValidProflie, isValidLanguage } = this.state;

    return (
      (action === undefined && isValidName && isValidLanguage) ||
      (action !== undefined && isValidLanguage && isValidName && isValidProflie)
    );
  }

  render() {
    const header = translate('quality_profiles.new_profile');
    const { action, isValidName, isValidProflie, isValidLanguage } = this.state;
    const languageQueryFilter = parseAsOptionalString(this.props.location.query.language);
    const languages = sortBy(this.props.languages, 'name');

    const selectedLanguage = this.state.language || languageQueryFilter;
    const importers = selectedLanguage
      ? this.state.importers.filter((importer) => importer.languages.includes(selectedLanguage))
      : [];

    const languageProfiles = this.props.profiles.filter((p) => p.language === selectedLanguage);
    const profiles = sortBy(languageProfiles, 'name').map((profile) => ({
      label: profile.isBuiltIn
        ? `${profile.name} (${translate('quality_profiles.built_in')})`
        : profile.name,
      value: profile.key,
    }));

    const languagesOptions = languages.map((l) => ({
      label: l.name,
      value: l.key,
    }));

    const canSubmit = this.canSubmit();

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose} size="medium">
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
              <fieldset className="modal-field big-spacer-bottom">
                <label className="spacer-top">
                  {translate('quality_profiles.chose_creation_type')}
                </label>
                <div className="display-flex-row spacer-top">
                  <RadioCard
                    noRadio={true}
                    selected={action === ProfileActionModals.Extend}
                    onClick={this.handleSelectExtend}
                    title={<ExtendQualityProfileIcon size={64} />}
                  >
                    <h3 className="spacer-bottom h4">
                      {translate('quality_profiles.creation_from_extend')}
                    </h3>
                    <p className="spacer-bottom">
                      {translate('quality_profiles.creation_from_extend_description_1')}
                    </p>
                    <p>{translate('quality_profiles.creation_from_extend_description_2')}</p>
                  </RadioCard>
                  <RadioCard
                    noRadio={true}
                    selected={action === ProfileActionModals.Copy}
                    onClick={this.handleSelectCopy}
                    title={<CopyQualityProfileIcon size={64} />}
                  >
                    <h3 className="spacer-bottom h4">
                      {translate('quality_profiles.creation_from_copy')}
                    </h3>
                    <p className="spacer-bottom">
                      {translate('quality_profiles.creation_from_copy_description_1')}
                    </p>
                    <p>{translate('quality_profiles.creation_from_copy_description_2')}</p>
                  </RadioCard>
                  <RadioCard
                    noRadio={true}
                    onClick={this.handleSelectBlank}
                    selected={action === undefined}
                    title={<NewQualityProfileIcon size={64} />}
                  >
                    <h3 className="spacer-bottom h4">
                      {translate('quality_profiles.creation_from_blank')}
                    </h3>
                    <p>{translate('quality_profiles.creation_from_blank_description')}</p>
                  </RadioCard>
                </div>
              </fieldset>

              <MandatoryFieldsExplanation className="modal-field" />

              <ValidationInput
                className="form-field"
                labelHtmlFor="create-profile-language-input"
                label={translate('language')}
                required={true}
                isInvalid={isValidLanguage !== undefined && !isValidLanguage}
                isValid={!!isValidLanguage}
              >
                <Select
                  autoFocus={true}
                  inputId="create-profile-language-input"
                  name="language"
                  isClearable={false}
                  onChange={this.handleLanguageChange}
                  options={languagesOptions}
                  isSearchable={true}
                  value={languagesOptions.filter((o) => o.value === selectedLanguage)}
                />
              </ValidationInput>
              {action !== undefined && (
                <ValidationInput
                  className="form-field"
                  labelHtmlFor="create-profile-parent-input"
                  label={translate(
                    action === ProfileActionModals.Copy
                      ? 'quality_profiles.creation.choose_copy_quality_profile'
                      : 'quality_profiles.creation.choose_parent_quality_profile'
                  )}
                  required={true}
                  isInvalid={isValidProflie !== undefined && !isValidProflie}
                  isValid={!!isValidProflie}
                >
                  <Select
                    autoFocus={true}
                    inputId="create-profile-parent-input"
                    name="parentKey"
                    isClearable={false}
                    onChange={this.handleQualityProfileChange}
                    options={profiles}
                    isSearchable={true}
                    value={profiles.filter((o) => o.value === this.state.profile)}
                  />
                </ValidationInput>
              )}
              <ValidationInput
                className="form-field"
                labelHtmlFor="create-profile-name"
                label={translate('name')}
                error={translate('quality_profiles.name_invalid')}
                required={true}
                isInvalid={isValidName !== undefined && !isValidName}
                isValid={!!isValidName}
              >
                <input
                  autoFocus={true}
                  id="create-profile-name"
                  maxLength={100}
                  name="name"
                  onChange={this.handleNameChange}
                  size={50}
                  type="text"
                  value={this.state.name}
                />
              </ValidationInput>

              {action === undefined &&
                importers.map((importer) => (
                  <div
                    className="modal-field spacer-bottom js-importer"
                    data-key={importer.key}
                    key={importer.key}
                  >
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
            </div>
          )}

          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            {!this.state.preloading && (
              <SubmitButton disabled={this.state.loading || !canSubmit} id="create-profile-submit">
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

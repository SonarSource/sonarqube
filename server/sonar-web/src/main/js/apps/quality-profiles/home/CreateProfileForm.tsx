/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { FlagMessage } from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import {
  changeProfileParent,
  copyProfile,
  createQualityProfile,
  getImporters,
} from '../../../api/quality-profiles';
import Modal from '../../../components/controls/Modal';
import RadioCard from '../../../components/controls/RadioCard';
import Select from '../../../components/controls/Select';
import ValidationInput from '../../../components/controls/ValidationInput';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import { Location } from '../../../components/hoc/withRouter';
import CopyQualityProfileIcon from '../../../components/icons/CopyQualityProfileIcon';
import ExtendQualityProfileIcon from '../../../components/icons/ExtendQualityProfileIcon';
import NewQualityProfileIcon from '../../../components/icons/NewQualityProfileIcon';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import Spinner from '../../../components/ui/Spinner';
import { translate } from '../../../helpers/l10n';
import { parseAsOptionalString } from '../../../helpers/query';
import { useProfileInheritanceQuery } from '../../../queries/quality-profiles';
import { Profile, ProfileActionModals } from '../types';

interface Props {
  languages: Array<{ key: string; name: string }>;
  location: Location;
  onClose: () => void;
  onCreate: Function;
  profiles: Profile[];
}

export default function CreateProfileForm(props: Readonly<Props>) {
  const { languages, profiles, onCreate } = props;
  const [importers, setImporters] = React.useState<
    Array<{ key: string; languages: string[]; name: string }>
  >([]);
  const [action, setAction] = React.useState<
    ProfileActionModals.Copy | ProfileActionModals.Extend | undefined
  >();
  const [submitting, setSubmitting] = React.useState(false);
  const [name, setName] = React.useState('');
  const [loading, setLoading] = React.useState(true);
  const [language, setLanguage] = React.useState<string>();
  const [isValidLanguage, setIsValidLanguage] = React.useState<boolean>();
  const [isValidName, setIsValidName] = React.useState<boolean>();
  const [isValidProfile, setIsValidProfile] = React.useState<boolean>();
  const [profile, setProfile] = React.useState<Profile>();

  const fetchImporters = React.useCallback(async () => {
    setLoading(true);
    try {
      const importers = await getImporters();
      setImporters(importers);
    } finally {
      setLoading(false);
    }
  }, [setImporters, setLoading]);

  function handleSelectExtend() {
    setAction(ProfileActionModals.Extend);
  }

  const handleSelectCopy = React.useCallback(() => {
    setAction(ProfileActionModals.Copy);
  }, [setAction]);

  const handleSelectBlank = React.useCallback(() => {
    setAction(undefined);
  }, [setAction]);

  const handleNameChange = React.useCallback(
    (event: React.SyntheticEvent<HTMLInputElement>) => {
      setName(event.currentTarget.value);
      setIsValidName(event.currentTarget.value.length > 0);
    },
    [setName, setIsValidName],
  );

  const handleLanguageChange = React.useCallback(
    (option: { value: string }) => {
      setLanguage(option.value);
      setIsValidLanguage(true);
      setProfile(undefined);
      setIsValidProfile(false);
    },
    [setLanguage, setIsValidLanguage],
  );

  const handleQualityProfileChange = React.useCallback(
    (option: { value: Profile } | null) => {
      setProfile(option?.value);
      setIsValidProfile(option !== null);
    },
    [setProfile, setIsValidProfile],
  );

  const handleFormSubmit = React.useCallback(
    async (event: React.SyntheticEvent<HTMLFormElement>) => {
      event.preventDefault();

      setSubmitting(true);
      const profileKey = profile?.key;
      try {
        if (action === ProfileActionModals.Copy && profileKey && name) {
          const profile = await copyProfile(profileKey, name);
          onCreate(profile);
        } else if (action === ProfileActionModals.Extend) {
          const { profile } = await createQualityProfile({ language, name });

          const parentProfile = profiles.find((p) => p.key === profileKey);
          if (parentProfile) {
            await changeProfileParent(profile, parentProfile);
          }

          onCreate(profile);
        } else {
          const data = new FormData(event.currentTarget);
          const { profile } = await createQualityProfile(data);
          onCreate(profile);
        }
      } finally {
        setSubmitting(false);
      }
    },
    [setSubmitting, onCreate, profiles, action, language, name, profile],
  );

  React.useEffect(() => {
    fetchImporters();
    const languageQueryFilter = parseAsOptionalString(props.location.query.language);
    if (languageQueryFilter !== undefined) {
      setLanguage(languageQueryFilter);
      setIsValidLanguage(true);
    }
  }, [fetchImporters, props.location.query.language]);

  const { data: { ancestors } = {}, isLoading } = useProfileInheritanceQuery(
    action === undefined || language === undefined || profile === undefined
      ? undefined
      : {
          language,
          name: profile.name,
        },
  );

  const extendsBuiltIn = ancestors?.some((p) => p.isBuiltIn);
  const showBuiltInWarning =
    action === undefined ||
    (action === ProfileActionModals.Copy && !extendsBuiltIn && profile !== undefined) ||
    (action === ProfileActionModals.Extend &&
      !extendsBuiltIn &&
      profile !== undefined &&
      !profile.isBuiltIn);
  const canSubmit =
    (action === undefined && isValidName && isValidLanguage) ||
    (action !== undefined && isValidLanguage && isValidName && isValidProfile);
  const header = translate('quality_profiles.new_profile');

  const languageQueryFilter = parseAsOptionalString(props.location.query.language);
  const selectedLanguage = language ?? languageQueryFilter;
  const filteredImporters = selectedLanguage
    ? importers.filter((importer) => importer.languages.includes(selectedLanguage))
    : [];

  const profilesForSelectedLanguage = profiles.filter((p) => p.language === selectedLanguage);
  const profileOptions = sortBy(profilesForSelectedLanguage, 'name').map((profile) => ({
    label: profile.isBuiltIn
      ? `${profile.name} (${translate('quality_profiles.built_in')})`
      : profile.name,
    value: profile,
  }));

  const languagesOptions = sortBy(languages, 'name').map((l) => ({
    label: l.name,
    value: l.key,
  }));

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose} size="medium">
      <form id="create-profile-form" onSubmit={handleFormSubmit}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        {loading ? (
          <div className="modal-body">
            <Spinner />
          </div>
        ) : (
          <div className="modal-body modal-container">
            <fieldset className="modal-field big-spacer-bottom">
              <label className="spacer-top">
                {translate('quality_profiles.chose_creation_type')}
              </label>
              <div className="display-flex-row spacer-top">
                <RadioCard
                  noRadio
                  selected={action === ProfileActionModals.Extend}
                  onClick={handleSelectExtend}
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
                  noRadio
                  selected={action === ProfileActionModals.Copy}
                  onClick={handleSelectCopy}
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
                  noRadio
                  onClick={handleSelectBlank}
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

            {!isLoading && showBuiltInWarning && (
              <FlagMessage variant="info" className="sw-mb-4">
                <div className="sw-flex sw-flex-col">
                  {translate('quality_profiles.no_built_in_updates_warning.new_profile')}
                  <span className="sw-mt-1">
                    {translate('quality_profiles.no_built_in_updates_warning.new_profile.2')}
                  </span>
                </div>
              </FlagMessage>
            )}

            <MandatoryFieldsExplanation className="modal-field" />

            <ValidationInput
              className="form-field"
              labelHtmlFor="create-profile-language-input"
              label={translate('language')}
              required
              isInvalid={isValidLanguage !== undefined && !isValidLanguage}
              isValid={!!isValidLanguage}
            >
              <Select
                autoFocus
                inputId="create-profile-language-input"
                name="language"
                isClearable={false}
                onChange={handleLanguageChange}
                options={languagesOptions}
                isSearchable
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
                    : 'quality_profiles.creation.choose_parent_quality_profile',
                )}
                required
                isInvalid={isValidProfile !== undefined && !isValidProfile}
                isValid={!!isValidProfile}
              >
                <Select
                  autoFocus
                  inputId="create-profile-parent-input"
                  name="parentKey"
                  isClearable={false}
                  onChange={handleQualityProfileChange}
                  options={profileOptions}
                  isSearchable
                  value={profileOptions.filter((o) => o.value === profile)}
                />
              </ValidationInput>
            )}
            <ValidationInput
              className="form-field"
              labelHtmlFor="create-profile-name"
              label={translate('name')}
              error={translate('quality_profiles.name_invalid')}
              required
              isInvalid={isValidName !== undefined && !isValidName}
              isValid={!!isValidName}
            >
              <input
                autoFocus
                id="create-profile-name"
                maxLength={100}
                name="name"
                onChange={handleNameChange}
                size={50}
                type="text"
                value={name}
              />
            </ValidationInput>

            {action === undefined &&
              filteredImporters.map((importer) => (
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
          {(submitting || isLoading) && <i className="spinner spacer-right" />}
          {!loading && (
            <SubmitButton disabled={submitting || !canSubmit} id="create-profile-submit">
              {translate('create')}
            </SubmitButton>
          )}
          <ResetButtonLink id="create-profile-cancel" onClick={props.onClose}>
            {translate('cancel')}
          </ResetButtonLink>
        </div>
      </form>
    </Modal>
  );
}

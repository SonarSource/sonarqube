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
import {
  ButtonPrimary,
  FileInput,
  FlagMessage,
  FormField,
  InputField,
  LabelValueSelectOption,
  LightLabel,
  Modal,
  Note,
  PopupZLevel,
  SearchSelectDropdown,
  SelectionCard,
  Spinner,
} from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { useRef } from 'react';
import { useIntl } from 'react-intl';
import { SingleValue } from 'react-select';
import {
  changeProfileParent,
  copyProfile,
  createQualityProfile,
  getImporters,
} from '../../../api/quality-profiles';
import { Location } from '../../../components/hoc/withRouter';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
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

  const intl = useIntl();

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

  const backupForm = useRef<HTMLFormElement>(null);

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
    (option: SingleValue<LabelValueSelectOption<string>>) => {
      setLanguage(option?.value);
      setIsValidLanguage(true);
      setProfile(undefined);
      setIsValidProfile(false);
    },
    [setLanguage, setIsValidLanguage],
  );

  const handleQualityProfileChange = React.useCallback(
    (option: SingleValue<LabelValueSelectOption<Profile>>) => {
      setProfile(option?.value);
      setIsValidProfile(Boolean(option?.value));
    },
    [setProfile, setIsValidProfile],
  );

  const handleFormSubmit = React.useCallback(async () => {
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
        const formData = new FormData(backupForm?.current ? backupForm.current : undefined);
        formData.set('language', language ?? '');
        formData.set('name', name);
        const { profile } = await createQualityProfile(formData);
        onCreate(profile);
      }
    } finally {
      setSubmitting(false);
    }
  }, [setSubmitting, onCreate, profiles, action, language, name, profile]);

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
  const header = intl.formatMessage({ id: 'quality_profiles.new_profile' });

  const languageQueryFilter = parseAsOptionalString(props.location.query.language);
  const selectedLanguage = language ?? languageQueryFilter;
  const filteredImporters = selectedLanguage
    ? importers.filter((importer) => importer.languages.includes(selectedLanguage))
    : [];

  const profilesForSelectedLanguage = profiles.filter((p) => p.language === selectedLanguage);
  const profileOptions = sortBy(profilesForSelectedLanguage, 'name').map((profile) => ({
    label: profile.isBuiltIn
      ? `${profile.name} (${intl.formatMessage({ id: 'quality_profiles.built_in' })})`
      : profile.name,
    value: profile,
  }));

  const languagesOptions = sortBy(languages, 'name').map((l) => ({
    label: l.name,
    value: l.key,
  }));

  function handleSearch<T>(
    options: { label: string; value: T }[],
    query: string,
    cb: (options: LabelValueSelectOption<T>[]) => void,
  ) {
    cb(options.filter((option) => option.label.toLowerCase().includes(query.toLowerCase())));
  }

  return (
    <Modal
      headerTitle={header}
      onClose={props.onClose}
      primaryButton={
        !loading && (
          <ButtonPrimary
            onClick={handleFormSubmit}
            disabled={submitting || !canSubmit}
            type="submit"
          >
            {intl.formatMessage({ id: 'create' })}
          </ButtonPrimary>
        )
      }
      secondaryButtonLabel={intl.formatMessage({ id: 'cancel' })}
      body={
        <>
          <LightLabel>
            {intl.formatMessage({ id: 'quality_profiles.chose_creation_type' })}
          </LightLabel>
          <div className="sw-mt-4 sw-flex sw-flex-col sw-gap-2">
            <SelectionCard
              selected={action === ProfileActionModals.Extend}
              onClick={handleSelectExtend}
              title={intl.formatMessage({ id: 'quality_profiles.creation_from_extend' })}
            >
              <p className="spacer-bottom">
                {intl.formatMessage({ id: 'quality_profiles.creation_from_extend_description_1' })}
              </p>
              <p>
                {intl.formatMessage({ id: 'quality_profiles.creation_from_extend_description_2' })}
              </p>
            </SelectionCard>
            <SelectionCard
              selected={action === ProfileActionModals.Copy}
              onClick={handleSelectCopy}
              title={intl.formatMessage({ id: 'quality_profiles.creation_from_copy' })}
            >
              <p className="spacer-bottom">
                {intl.formatMessage({ id: 'quality_profiles.creation_from_copy_description_1' })}
              </p>
              <p>
                {intl.formatMessage({ id: 'quality_profiles.creation_from_copy_description_2' })}
              </p>
            </SelectionCard>
            <SelectionCard
              selected={action === undefined}
              onClick={handleSelectBlank}
              title={intl.formatMessage({ id: 'quality_profiles.creation_from_blank' })}
            >
              {intl.formatMessage({ id: 'quality_profiles.creation_from_blank_description' })}
            </SelectionCard>
          </div>
          {!isLoading && showBuiltInWarning && (
            <FlagMessage variant="info" className="sw-block sw-my-4">
              <div className="sw-flex sw-flex-col">
                {intl.formatMessage({
                  id: 'quality_profiles.no_built_in_updates_warning.new_profile',
                })}
                <span className="sw-mt-1">
                  {intl.formatMessage({
                    id: 'quality_profiles.no_built_in_updates_warning.new_profile.2',
                  })}
                </span>
              </div>
            </FlagMessage>
          )}
          <div className="sw-my-4">
            <MandatoryFieldsExplanation />
          </div>
          <FormField label={intl.formatMessage({ id: 'language' })} required>
            <SearchSelectDropdown
              controlAriaLabel={intl.formatMessage({ id: 'language' })}
              autoFocus
              inputId="create-profile-language-input"
              name="language"
              onChange={handleLanguageChange}
              defaultOptions={languagesOptions}
              loadOptions={(inputValue, cb) => handleSearch(languagesOptions, inputValue, cb)}
              value={languagesOptions.find((o) => o.value === selectedLanguage)}
              zLevel={PopupZLevel.Global}
            />
          </FormField>
          {action !== undefined && (
            <FormField label={intl.formatMessage({ id: 'quality_profiles.parent' })} required>
              <SearchSelectDropdown
                controlAriaLabel={intl.formatMessage({
                  id:
                    action === ProfileActionModals.Copy
                      ? 'quality_profiles.creation.choose_copy_quality_profile'
                      : 'quality_profiles.creation.choose_parent_quality_profile',
                })}
                autoFocus
                inputId="create-profile-parent-input"
                name="parentKey"
                onChange={handleQualityProfileChange}
                defaultOptions={profileOptions}
                loadOptions={(inputValue, cb) => handleSearch(profileOptions, inputValue, cb)}
                isSearchable
                value={profileOptions.find((o) => o.value === profile)}
              />
            </FormField>
          )}
          <FormField
            htmlFor="create-profile-name"
            label={intl.formatMessage({ id: 'name' })}
            required
          >
            <InputField
              autoFocus
              id="create-profile-name"
              maxLength={50}
              name="name"
              onChange={handleNameChange}
              required
              size="full"
              type="text"
              value={name}
            />
          </FormField>

          {action === undefined && (
            <form ref={backupForm}>
              {filteredImporters.map((importer) => (
                <FormField
                  key={importer.key}
                  htmlFor={'create-profile-form-backup-' + importer.key}
                  label={importer.name}
                >
                  <FileInput
                    id={`create-profile-form-backup-${importer.key}`}
                    name={`backup_${importer.key}`}
                    chooseLabel={intl.formatMessage({ id: 'choose_file' })}
                    clearLabel={intl.formatMessage({ id: 'clear_file' })}
                    noFileLabel={intl.formatMessage({ id: 'no_file_selected' })}
                  />
                  <Note>
                    {intl.formatMessage({ id: 'quality_profiles.optional_configuration_file' })}
                  </Note>
                </FormField>
              ))}{' '}
            </form>
          )}

          <Spinner loading={submitting || isLoading} />
        </>
      }
    />
  );
}

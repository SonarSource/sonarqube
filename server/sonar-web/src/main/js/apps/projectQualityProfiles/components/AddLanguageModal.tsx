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
import { ButtonPrimary, FormField, InputSelect, Modal } from 'design-system';
import { difference } from 'lodash';
import * as React from 'react';
import { Profile } from '../../../api/quality-profiles';
import withLanguagesContext from '../../../app/components/languages/withLanguagesContext';
import { LabelValueSelectOption } from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';
import { Languages } from '../../../types/languages';
import { Dict } from '../../../types/types';
import LanguageProfileSelectOption, { ProfileOption } from './LanguageProfileSelectOption';

export interface AddLanguageModalProps {
  languages: Languages;
  onClose: () => void;
  onSubmit: (key: string) => Promise<void>;
  profilesByLanguage: Dict<Profile[]>;
  unavailableLanguages: string[];
}

export function AddLanguageModal(props: AddLanguageModalProps) {
  const { languages, profilesByLanguage, unavailableLanguages } = props;

  const [{ language, key }, setSelected] = React.useState<{ language?: string; key?: string }>({
    language: undefined,
    key: undefined,
  });

  const header = translate('project_quality_profile.add_language_modal.title');

  const languageOptions: LabelValueSelectOption[] = difference(
    Object.keys(profilesByLanguage),
    unavailableLanguages,
  ).map((l) => ({ value: l, label: languages[l].name }));

  const profileOptions: ProfileOption[] =
    language !== undefined
      ? profilesByLanguage[language].map((p) => ({
          value: p.key,
          label: p.name,
          language,
          isDisabled: p.activeRuleCount === 0,
        }))
      : [];

  const onFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (language && key) {
      props.onSubmit(key);
    }
  };

  const renderForm = (
    <form id="add-language-quality-profile" onSubmit={onFormSubmit}>
      <div>
        <FormField
          className="sw-mb-4"
          label={translate('project_quality_profile.add_language_modal.choose_language')}
          htmlFor="language"
        >
          <InputSelect
            size="full"
            id="language"
            aria-label={translate('project_quality_profile.add_language_modal.choose_language')}
            onChange={({ value }: LabelValueSelectOption) => {
              setSelected({ language: value, key: undefined });
            }}
            options={languageOptions}
          />
        </FormField>

        <FormField
          className="sw-mb-4"
          label={translate('project_quality_profile.add_language_modal.choose_profile')}
          htmlFor="profiles"
        >
          <InputSelect
            size="full"
            isDisabled={!language}
            id="profiles"
            aria-label={translate('project_quality_profile.add_language_modal.choose_profile')}
            onChange={({ value }: ProfileOption) => setSelected({ language, key: value })}
            options={profileOptions}
            components={{
              Option: LanguageProfileSelectOption,
            }}
            value={profileOptions.find((o) => o.value === key) ?? null}
          />
        </FormField>
      </div>
    </form>
  );

  return (
    <Modal
      onClose={props.onClose}
      headerTitle={header}
      isOverflowVisible
      body={renderForm}
      primaryButton={
        <ButtonPrimary
          disabled={!language || !key}
          form="add-language-quality-profile"
          type="submit"
        >
          {translate('save')}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}

export default withLanguagesContext(AddLanguageModal);

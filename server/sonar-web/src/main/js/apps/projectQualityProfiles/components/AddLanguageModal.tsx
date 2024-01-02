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
import { difference } from 'lodash';
import * as React from 'react';
import { Profile } from '../../../api/quality-profiles';
import withLanguagesContext from '../../../app/components/languages/withLanguagesContext';
import { ButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Select, { BasicSelectOption } from '../../../components/controls/Select';
import SimpleModal from '../../../components/controls/SimpleModal';
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

  const languageOptions: BasicSelectOption[] = difference(
    Object.keys(profilesByLanguage),
    unavailableLanguages
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

  return (
    <SimpleModal
      header={header}
      onClose={props.onClose}
      onSubmit={() => {
        if (language && key) {
          props.onSubmit(key);
        }
      }}
    >
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <form onSubmit={onFormSubmit}>
            <div className="modal-body">
              <div className="big-spacer-bottom">
                <div className="little-spacer-bottom">
                  <label className="text-bold" htmlFor="language">
                    {translate('project_quality_profile.add_language_modal.choose_language')}
                  </label>
                </div>
                <Select
                  className="abs-width-300"
                  isDisabled={submitting}
                  id="language"
                  onChange={({ value }: BasicSelectOption) => {
                    setSelected({ language: value, key: undefined });
                  }}
                  options={languageOptions}
                />
              </div>

              <div className="big-spacer-bottom">
                <div className="little-spacer-bottom">
                  <label className="text-bold" htmlFor="profiles">
                    {translate('project_quality_profile.add_language_modal.choose_profile')}
                  </label>
                </div>
                <Select
                  className="abs-width-300"
                  isDisabled={submitting || !language}
                  id="profiles"
                  onChange={({ value }: ProfileOption) => setSelected({ language, key: value })}
                  options={profileOptions}
                  components={{
                    Option: LanguageProfileSelectOption,
                  }}
                  value={profileOptions.find((o) => o.value === key) ?? null}
                />
              </div>
            </div>

            <div className="modal-foot">
              {submitting && <i className="spinner spacer-right" />}
              <SubmitButton disabled={submitting || !language || !key}>
                {translate('save')}
              </SubmitButton>
              <ButtonLink disabled={submitting} onClick={onCloseClick}>
                {translate('cancel')}
              </ButtonLink>
            </div>
          </form>
        </>
      )}
    </SimpleModal>
  );
}

export default withLanguagesContext(AddLanguageModal);

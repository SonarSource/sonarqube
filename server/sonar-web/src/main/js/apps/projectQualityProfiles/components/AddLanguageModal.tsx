/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { connect } from 'react-redux';
import { ButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Select from 'sonar-ui-common/components/controls/Select';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Profile } from '../../../api/quality-profiles';
import { Store } from '../../../store/rootReducer';

export interface AddLanguageModalProps {
  languages: T.Languages;
  onClose: () => void;
  onSubmit: (key: string) => Promise<void>;
  profilesByLanguage: T.Dict<Profile[]>;
  unavailableLanguages: string[];
}

export function AddLanguageModal(props: AddLanguageModalProps) {
  const { languages, profilesByLanguage, unavailableLanguages } = props;

  const [{ language, key }, setSelected] = React.useState<{ language?: string; key?: string }>({
    language: undefined,
    key: undefined
  });

  const header = translate('project_quality_profile.add_language_modal.title');

  const languageOptions = difference(
    Object.keys(profilesByLanguage),
    unavailableLanguages
  ).map(l => ({ value: l, label: languages[l].name }));

  const profileOptions =
    language !== undefined
      ? profilesByLanguage[language].map(p => ({ value: p.key, label: p.name }))
      : [];

  return (
    <SimpleModal
      header={header}
      onClose={props.onClose}
      onSubmit={() => {
        if (language && key) {
          props.onSubmit(key);
        }
      }}>
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
                  clearable={false}
                  disabled={submitting}
                  id="language"
                  onChange={({ value }: { value: string }) => setSelected({ language: value })}
                  options={languageOptions}
                  value={language}
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
                  clearable={false}
                  disabled={submitting || !language}
                  id="profiles"
                  onChange={({ value }: { value: string }) => setSelected({ language, key: value })}
                  options={profileOptions}
                  value={key}
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

function mapStateToProps({ languages }: Store) {
  return { languages };
}

export default connect(mapStateToProps)(AddLanguageModal);

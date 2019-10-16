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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ALM_KEYS } from '../../utils';

export interface AlmPRDecorationFormModalProps {
  alm: string;
  canSubmit: () => boolean;
  formData: T.GithubDefinition;
  onCancel: () => void;
  onSubmit: () => void;
  onFieldChange: (id: string, value: string) => void;
  originalKey: string;
}

function renderField(params: {
  autoFocus?: boolean;
  formData: T.GithubDefinition;
  help: boolean;
  id: string;
  isTextArea: boolean;
  maxLength: number;
  onFieldChange: (id: string, value: string) => void;
  propKey: keyof T.GithubDefinition;
}) {
  const { autoFocus, formData, help, id, isTextArea, maxLength, onFieldChange, propKey } = params;
  return (
    <div className="modal-field">
      <label htmlFor={id}>
        {translate('settings.pr_decoration.form', id)}
        <em className="mandatory spacer-right">*</em>
        {help && <HelpTooltip overlay={translate('settings.pr_decoration.form', id, 'help')} />}
      </label>
      {isTextArea ? (
        <textarea
          className="settings-large-input"
          id="privateKey"
          maxLength={maxLength}
          onChange={e => onFieldChange(propKey, e.currentTarget.value)}
          required={true}
          rows={5}
          value={formData[propKey]}
        />
      ) : (
        <input
          autoFocus={autoFocus}
          className="input-super-large"
          id={id}
          maxLength={maxLength}
          name={id}
          onChange={e => onFieldChange(propKey, e.currentTarget.value)}
          size={50}
          type="text"
          value={formData[propKey]}
        />
      )}
    </div>
  );
}

export default function AlmPRDecorationFormModalRenderer(props: AlmPRDecorationFormModalProps) {
  const { alm, formData, onFieldChange, originalKey } = props;
  const header = translate('settings.pr_decoration.form.header', originalKey ? 'edit' : 'create');

  return (
    <SimpleModal header={header} onClose={props.onCancel} onSubmit={props.onSubmit} size="medium">
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <form className="views-form" onSubmit={onFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <div className="modal-body modal-container">
            {renderField({
              autoFocus: true,
              id: 'name',
              formData,
              propKey: 'key',
              maxLength: 40,
              onFieldChange,
              help: true,
              isTextArea: false
            })}
            {renderField({
              id: `url.${alm}`,
              formData,
              propKey: 'url',
              maxLength: 2000,
              onFieldChange,
              help: false,
              isTextArea: false
            })}
            {alm === ALM_KEYS.GITHUB &&
              renderField({
                id: 'app_id',
                formData,
                propKey: 'appId',
                maxLength: 80,
                onFieldChange,
                help: false,
                isTextArea: false
              })}
            {renderField({
              id: 'private_key',
              formData,
              propKey: 'privateKey',
              maxLength: 2000,
              onFieldChange,
              help: false,
              isTextArea: true
            })}
          </div>

          <div className="modal-foot">
            <DeferredSpinner className="spacer-right" loading={submitting} />
            <SubmitButton disabled={submitting || !props.canSubmit()}>
              {translate('settings.pr_decoration.form.save')}
            </SubmitButton>
            <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
          </div>
        </form>
      )}
    </SimpleModal>
  );
}

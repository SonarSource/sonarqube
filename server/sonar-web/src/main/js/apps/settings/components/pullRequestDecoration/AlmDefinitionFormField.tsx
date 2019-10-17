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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface AlmDefinitionFormFieldProps<B extends T.AlmSettingsBinding> {
  autoFocus?: boolean;
  formData: B;
  help: boolean;
  id: string;
  isTextArea: boolean;
  maxLength: number;
  onFieldChange: (id: keyof B, value: string) => void;
  propKey: keyof B;
}

export function AlmDefinitionFormField<B extends T.AlmSettingsBinding>(
  props: AlmDefinitionFormFieldProps<B>
) {
  const { autoFocus, formData, help, id, isTextArea, maxLength, onFieldChange, propKey } = props;

  return (
    <div className="modal-field">
      <label className="display-flex-center" htmlFor={id}>
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
          value={String(formData[propKey])}
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
          value={String(formData[propKey])}
        />
      )}
    </div>
  );
}

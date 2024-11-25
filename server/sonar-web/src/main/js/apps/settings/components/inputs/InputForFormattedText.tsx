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

import * as React from 'react';
import FormattingTipsWithLink from '../../../../components/common/FormattingTipsWithLink';
import { Button } from '../../../../components/controls/buttons';
import EditIcon from '../../../../components/icons/EditIcon';
import { translate } from '../../../../helpers/l10n';
import { SafeHTMLInjection, SanitizeLevel } from '../../../../helpers/sanitize';
import { DefaultSpecializedInputProps } from '../../utils';

export default function InputForFormattedText(props: DefaultSpecializedInputProps) {
  const { isEditing, setting, name, value } = props;
  const { values, hasValue } = setting;
  const editMode = !hasValue || isEditing;
  // 0th value of the values array is markdown and 1st is the formatted text
  const formattedValue = values ? values[1] : undefined;

  function handleInputChange(event: React.ChangeEvent<HTMLTextAreaElement>) {
    props.onChange(event.target.value);
  }

  return (
    <div>
      {editMode ? (
        <div className="display-flex-row">
          <textarea
            className="settings-large-input text-top spacer-right"
            name={name}
            onChange={handleInputChange}
            rows={5}
            value={value || ''}
          />
          <FormattingTipsWithLink className="abs-width-100" />
        </div>
      ) : (
        <>
          <SafeHTMLInjection
            htmlAsString={formattedValue ?? ''}
            sanitizeLevel={SanitizeLevel.USER_INPUT}
          >
            <div className="markdown-preview markdown" />
          </SafeHTMLInjection>

          <Button className="spacer-top" onClick={props.onEditing}>
            <EditIcon className="spacer-right" />
            {translate('edit')}
          </Button>
        </>
      )}
    </div>
  );
}

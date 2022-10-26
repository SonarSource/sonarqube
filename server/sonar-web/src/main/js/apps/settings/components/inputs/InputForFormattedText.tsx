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
import * as React from 'react';
import FormattingTipsWithLink from '../../../../components/common/FormattingTipsWithLink';
import { Button } from '../../../../components/controls/buttons';
import EditIcon from '../../../../components/icons/EditIcon';
import { translate } from '../../../../helpers/l10n';
import { sanitizeString } from '../../../../helpers/sanitize';
import { DefaultSpecializedInputProps } from '../../utils';

interface State {
  editMessage: boolean;
}

export default class InputForFormattedText extends React.PureComponent<
  DefaultSpecializedInputProps,
  State
> {
  constructor(props: DefaultSpecializedInputProps) {
    super(props);
    this.state = {
      editMessage: !this.props.setting.hasValue
    };
  }

  componentDidUpdate(prevProps: DefaultSpecializedInputProps) {
    /*
     * Reset `editMessage` if:
     *  - the value is reset (valueChanged -> !valueChanged)
     *     or
     *  - the value changes from outside the input (i.e. store update/reset/cancel)
     */
    if (
      (prevProps.hasValueChanged || this.props.setting.value !== prevProps.setting.value) &&
      !this.props.hasValueChanged
    ) {
      this.setState({ editMessage: !this.props.setting.hasValue });
    }
  }

  handleInputChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.props.onChange(event.target.value);
  };

  toggleEditMessage = () => {
    const { editMessage } = this.state;
    this.setState({ editMessage: !editMessage });
  };

  render() {
    const { editMessage } = this.state;
    const { values } = this.props.setting;
    // 0th value of the values array is markdown and 1st is the formatted text
    const formattedValue = values ? values[1] : undefined;

    return (
      <div>
        {editMessage ? (
          <div className="display-flex-row">
            <textarea
              className="settings-large-input text-top spacer-right"
              name={this.props.name}
              onChange={this.handleInputChange}
              rows={5}
              value={this.props.value || ''}
            />
            <FormattingTipsWithLink className="abs-width-100" />
          </div>
        ) : (
          <>
            <div
              className="markdown-preview markdown"
              // eslint-disable-next-line react/no-danger
              dangerouslySetInnerHTML={{ __html: sanitizeString(formattedValue ?? '') }}
            />
            <Button className="spacer-top" onClick={this.toggleEditMessage}>
              <EditIcon className="spacer-right" />
              {translate('edit')}
            </Button>
          </>
        )}
      </div>
    );
  }
}

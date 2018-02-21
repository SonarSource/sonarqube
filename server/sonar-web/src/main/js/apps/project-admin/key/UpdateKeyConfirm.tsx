/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import ConfirmButton, { ChildrenProps } from '../../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  children: (props: ChildrenProps) => React.ReactNode;
  component: { key: string; name: string };
  newKey: string | undefined;
  onConfirm: (oldKey: string, newKey: string) => Promise<void>;
}

export default class UpdateKeyConfirm extends React.PureComponent<Props> {
  handleConfirm = () => {
    return this.props.newKey
      ? this.props.onConfirm(this.props.component.key, this.props.newKey)
      : Promise.reject(undefined);
  };

  render() {
    const { children, component, newKey } = this.props;

    return (
      <ConfirmButton
        confirmButtonText={translate('update_verb')}
        modalBody={
          <>
            {translateWithParameters('update_key.are_you_sure_to_change_key', component.name)}
            <div className="spacer-top">
              {translate('update_key.old_key')}
              {': '}
              <strong>{component.key}</strong>
            </div>
            <div className="spacer-top">
              {translate('update_key.new_key')}
              {': '}
              <strong>{newKey}</strong>
            </div>
          </>
        }
        modalHeader={translate('update_key.page')}
        onConfirm={this.handleConfirm}>
        {children}
      </ConfirmButton>
    );
  }
}

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
import { omit } from 'lodash';
import * as React from 'react';
import { components, ControlProps, OptionProps, SingleValueProps } from 'react-select';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Modal from '../../../components/controls/Modal';
import { SearchSelect } from '../../../components/controls/Select';
import GroupIcon from '../../../components/icons/GroupIcon';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { Group, isUser } from '../../../types/quality-gates';
import { UserBase } from '../../../types/users';
import { OptionWithValue } from './QualityGatePermissionsAddModal';

export interface QualityGatePermissionsAddModalRendererProps {
  onClose: () => void;
  handleSearch: (q: string, resolve: (options: OptionWithValue[]) => void) => void;
  onSelection: (selection: OptionWithValue) => void;
  selection?: UserBase | Group;
  onSubmit: (event: React.SyntheticEvent<HTMLFormElement>) => void;
  submitting: boolean;
}

export default function QualityGatePermissionsAddModalRenderer(
  props: QualityGatePermissionsAddModalRendererProps
) {
  const { selection, submitting } = props;

  const header = translate('quality_gates.permissions.grant');

  const noResultsText = translate('no_results');

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <header className="modal-head">
        <h2>{header}</h2>
      </header>
      <form onSubmit={props.onSubmit}>
        <div className="modal-body">
          <div className="modal-field">
            <label>{translate('quality_gates.permissions.search')}</label>
            <SearchSelect
              autoFocus={true}
              isClearable={false}
              placeholder=""
              defaultOptions={true}
              noOptionsMessage={() => noResultsText}
              onChange={props.onSelection}
              loadOptions={props.handleSearch}
              getOptionValue={(opt) => (isUser(opt) ? opt.login : opt.name)}
              large={true}
              components={{
                Option: optionRenderer,
                SingleValue: singleValueRenderer,
                Control: controlRenderer,
              }}
            />
          </div>
        </div>
        <footer className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          <SubmitButton disabled={!selection || submitting}>{translate('add_verb')}</SubmitButton>
          <ResetButtonLink onClick={props.onClose}>{translate('cancel')}</ResetButtonLink>
        </footer>
      </form>
    </Modal>
  );
}

export function customOptions(option: OptionWithValue) {
  return (
    <span className="display-flex-center" data-testid="qg-add-permission-option">
      {isUser(option) ? (
        <Avatar hash={option.avatar} name={option.name} size={16} />
      ) : (
        <GroupIcon size={16} />
      )}
      <strong className="spacer-left">{option.name}</strong>
      {isUser(option) && <span className="note little-spacer-left">{option.login}</span>}
    </span>
  );
}

function optionRenderer(props: OptionProps<OptionWithValue, false>) {
  return <components.Option {...props}>{customOptions(props.data)}</components.Option>;
}

function singleValueRenderer(props: SingleValueProps<OptionWithValue>) {
  return <components.SingleValue {...props}>{customOptions(props.data)}</components.SingleValue>;
}

function controlRenderer(props: ControlProps<OptionWithValue, false>) {
  return (
    <components.Control {...omit(props, ['children'])} className="abs-height-100">
      {props.children}
    </components.Control>
  );
}

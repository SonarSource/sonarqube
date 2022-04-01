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
import { identity, omit } from 'lodash';
import * as React from 'react';
import { components, ControlProps, OptionProps, SingleValueProps } from 'react-select';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Modal from '../../../components/controls/Modal';
import Select from '../../../components/controls/Select';
import GroupIcon from '../../../components/icons/GroupIcon';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { Group, isUser } from '../../../types/quality-gates';
import { UserBase } from '../../../types/users';

export interface QualityGatePermissionsAddModalRendererProps {
  onClose: () => void;
  onInputChange: (query: string) => void;
  onSubmit: (event: React.SyntheticEvent<HTMLFormElement>) => void;
  onSelection: (selection: Option) => void;
  submitting: boolean;
  loading: boolean;
  query: string;
  searchResults: Array<UserBase | Group>;
  selection?: UserBase | Group;
}

export type Option = (UserBase | Group) & { value: string };

export default function QualityGatePermissionsAddModalRenderer(
  props: QualityGatePermissionsAddModalRendererProps
) {
  const { loading, searchResults, selection, submitting } = props;

  const header = translate('quality_gates.permissions.grant');

  const noResultsText = translate('no_results');

  const options = searchResults.map(r => ({ ...r, value: getValue(r) }));

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <header className="modal-head">
        <h2>{header}</h2>
      </header>
      <form onSubmit={props.onSubmit}>
        <div className="modal-body">
          <div className="modal-field">
            <label>{translate('quality_gates.permissions.search')}</label>
            <Select
              className="Select-big"
              autoFocus={true}
              isClearable={false}
              isSearchable={true}
              placeholder=""
              isLoading={loading}
              filterOptions={identity}
              noOptionsMessage={() => noResultsText}
              onChange={props.onSelection}
              onInputChange={props.onInputChange}
              components={{
                Option: optionRenderer,
                SingleValue: singleValueRenderer,
                Control: controlRenderer
              }}
              options={options}
              value={options.find(o => o.value === (selection && getValue(selection)))}
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

function getValue(option: UserBase | Group) {
  return isUser(option) ? option.login : option.name;
}

export function customOptions(option: Option) {
  return (
    <>
      {isUser(option) ? (
        <Avatar hash={option.avatar} name={option.name} size={16} />
      ) : (
        <GroupIcon size={16} />
      )}
      <strong className="spacer-left">{option.name}</strong>
      {isUser(option) && <span className="note little-spacer-left">{option.login}</span>}
    </>
  );
}

function optionRenderer(props: OptionProps<Option, false>) {
  return (
    <components.Option {...props} className="Select-option">
      {customOptions(props.data)}
    </components.Option>
  );
}

function singleValueRenderer(props: SingleValueProps<Option>) {
  return (
    <components.SingleValue {...props} className="Select-value-label">
      {customOptions(props.data)}
    </components.SingleValue>
  );
}

function controlRenderer(props: ControlProps<Option, false>) {
  return (
    <components.Control {...omit(props, ['children'])} className="abs-height-100 Select-control">
      {props.children}
    </components.Control>
  );
}

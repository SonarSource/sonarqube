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
import {
  ButtonPrimary,
  FormField,
  GenericAvatar,
  LabelValueSelectOption,
  Modal,
  Note,
  SearchSelectDropdown,
  UserGroupIcon,
} from 'design-system';
import * as React from 'react';
import { GroupBase, OptionProps, Options, SingleValue, components } from 'react-select';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { Group as UserGroup, isUser } from '../../../types/quality-gates';
import { UserBase } from '../../../types/users';

export interface QualityGatePermissionsAddModalRendererProps {
  handleSearch: (
    q: string,
    resolve: (options: Options<LabelValueSelectOption<UserBase | UserGroup>>) => void,
  ) => void;
  onClose: () => void;
  onSelection: (selection: SingleValue<LabelValueSelectOption<UserBase | UserGroup>>) => void;
  onSubmit: (event: React.SyntheticEvent<HTMLFormElement>) => void;
  selection?: UserBase | UserGroup;
  submitting: boolean;
}

const FORM_ID = 'quality-gate-permissions-add-modal';
const USER_SELECT_INPUT_ID = 'quality-gate-permissions-add-modal-select-input';

export default function QualityGatePermissionsAddModalRenderer(
  props: Readonly<QualityGatePermissionsAddModalRendererProps>,
) {
  const { selection, submitting } = props;

  const renderedSelection = React.useMemo(() => {
    return <OptionRenderer option={selection} small />;
  }, [selection]);

  return (
    <Modal
      onClose={props.onClose}
      headerTitle={translate('quality_gates.permissions.grant')}
      body={
        <form onSubmit={props.onSubmit} id={FORM_ID}>
          <FormField
            label={translate('quality_gates.permissions.search')}
            htmlFor={USER_SELECT_INPUT_ID}
          >
            <SearchSelectDropdown
              className="sw-mb-2"
              controlAriaLabel={translate('quality_gates.permissions.search')}
              inputId={USER_SELECT_INPUT_ID}
              autoFocus
              defaultOptions
              noOptionsMessage={() => translate('no_results')}
              onChange={props.onSelection}
              loadOptions={props.handleSearch}
              getOptionValue={({ value }: LabelValueSelectOption<UserBase | UserGroup>) =>
                isUser(value) ? value.login : value.name
              }
              controlLabel={renderedSelection}
              components={{
                Option,
              }}
            />
          </FormField>
        </form>
      }
      primaryButton={
        <ButtonPrimary disabled={!selection || submitting} type="submit" form={FORM_ID}>
          {translate('add_verb')}
        </ButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}

function OptionRenderer({
  option,
  small = false,
}: Readonly<{
  option?: UserBase | UserGroup;
  small?: boolean;
}>) {
  if (!option) {
    return null;
  }
  return (
    <>
      {isUser(option) ? (
        <>
          <Avatar
            className={small ? 'sw-my-1' : ''}
            hash={option.avatar}
            name={option.name}
            size={small ? 'xs' : 'sm'}
          />
          <span className="sw-ml-2">
            <strong className="sw-body-sm-highlight sw-mr-1">{option.name}</strong>
            <Note>{option.login}</Note>
          </span>
        </>
      ) : (
        <>
          <GenericAvatar
            className={small ? 'sw-my-1' : ''}
            Icon={UserGroupIcon}
            name={option.name}
            size={small ? 'xs' : 'sm'}
          />
          <strong className="sw-body-sm-highlight sw-ml-2">{option.name}</strong>
        </>
      )}
    </>
  );
}

function Option<
  Option extends LabelValueSelectOption<UserBase | UserGroup>,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(props: OptionProps<Option, IsMulti, Group>) {
  const {
    data: { value },
  } = props;

  return (
    <components.Option {...props}>
      <div className="sw-flex sw-items-center">
        <OptionRenderer option={value} />
      </div>
    </components.Option>
  );
}

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

import { sortBy } from 'lodash';
import * as React from 'react';
import {
  ButtonPrimary,
  FlagMessage,
  FormField,
  InputSelect,
  LabelValueSelectOption,
  Modal,
} from '~design-system';
import { changeProfileParent } from '../../../api/quality-profiles';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { Profile } from '../types';

interface Props {
  onChange: () => void;
  onClose: () => void;
  profile: Profile;
  profiles: Profile[];
}

interface State {
  loading: boolean;
  selected: { value: string } | null;
}

export default class ChangeParentForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: false,
    selected: null,
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSelectChange = (option: { value: string }) => {
    this.setState({ selected: option });
  };

  handleFormSubmit = () => {
    const parent = this.props.profiles.find((p) => p.key === this.state.selected?.value);

    this.setState({ loading: true });
    changeProfileParent(this.props.profile, parent)
      .then(this.props.onChange)
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  render() {
    const { profiles, profile } = this.props;
    const { loading, selected } = this.state;

    const options = [
      { label: translate('none'), value: '' },
      ...sortBy(profiles, 'name').map((profile) => ({
        label: profile.isBuiltIn
          ? `${profile.name} (${translate('quality_profiles.built_in')})`
          : profile.name,
        value: profile.key,
      })),
    ].filter((o) => o.value !== profile.key);

    const submitDisabled = loading || selected == null || selected.value === profile.parentKey;

    const selectedValue = selected
      ? options.find((o) => o.value === selected?.value)
      : options.find((o) => o.value === profile.parentKey);

    return (
      <Modal
        headerTitle={translate('quality_profiles.change_parent')}
        onClose={this.props.onClose}
        loading={loading}
        isOverflowVisible
        body={
          <>
            {profile.parentKey !== undefined && (
              <FlagMessage variant="info" className="sw-mb-8">
                {translate('quality_profiles.change_parent_warning')}
              </FlagMessage>
            )}

            <MandatoryFieldsExplanation />

            <FormField
              className="sw-mt-2"
              htmlFor="quality-profile-new-parent"
              label={translate('quality_profiles.parent')}
              required
            >
              <InputSelect
                id="quality-profile-new-parent"
                name="parent"
                onChange={(data: LabelValueSelectOption<string>) => this.handleSelectChange(data)}
                options={options}
                required
                size="full"
                value={selectedValue}
              />
            </FormField>
          </>
        }
        primaryButton={
          <ButtonPrimary onClick={this.handleFormSubmit} disabled={submitDisabled}>
            {translate('change_verb')}
          </ButtonPrimary>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}

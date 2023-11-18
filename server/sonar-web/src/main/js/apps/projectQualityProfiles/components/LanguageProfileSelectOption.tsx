/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Link } from 'design-system';
import * as React from 'react';
import { components, OptionProps } from 'react-select';
import DisableableSelectOption from '../../../components/common/DisableableSelectOption';
import { LabelValueSelectOption } from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../helpers/urls';

export interface ProfileOption extends LabelValueSelectOption {
  language: string;
  isDisabled: boolean;
}

export type LanguageProfileSelectOptionProps = OptionProps<ProfileOption, false>;

export default function LanguageProfileSelectOption(props: LanguageProfileSelectOptionProps) {
  const option = props.data;

  const SelectOptionDisableTooltipOverlay = React.useCallback(
    () => (
      <>
        <p>
          {translate(
            'project_quality_profile.add_language_modal.profile_unavailable_no_active_rules',
          )}
        </p>
        {option.label && option.language && (
          <Link to={getQualityProfileUrl(option.label, option.language)}>
            {translate('project_quality_profile.add_language_modal.go_to_profile')}
          </Link>
        )}
      </>
    ),
    [option.label, option.language],
  );

  return (
    <components.Option {...props}>
      <div>
        <DisableableSelectOption
          option={option}
          disabledReason={translate('project_quality_profile.add_language_modal.no_active_rules')}
          disableTooltipOverlay={SelectOptionDisableTooltipOverlay}
        />
      </div>
    </components.Option>
  );
}

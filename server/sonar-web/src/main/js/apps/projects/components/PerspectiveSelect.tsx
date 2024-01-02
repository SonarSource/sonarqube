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
import { InputSelect, LabelValueSelectOption, StyledPageTitle } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { VIEWS } from '../utils';

interface Props {
  onChange: (x: { view: string }) => void;
  view: string;
}

export interface PerspectiveOption {
  value: string;
  label: string;
}

export default class PerspectiveSelect extends React.PureComponent<Props> {
  handleChange = (option: PerspectiveOption) => {
    this.props.onChange({ view: option.value });
  };

  render() {
    const { view } = this.props;
    const options: PerspectiveOption[] = [
      ...VIEWS.map((opt) => ({
        value: opt.value,
        label: translate('projects.view', opt.label),
      })),
    ];
    return (
      <div className="sw-flex sw-items-center">
        <StyledPageTitle
          id="aria-projects-perspective"
          as="label"
          className="sw-body-sm-highlight sw-mr-2"
        >
          {translate('projects.perspective')}
        </StyledPageTitle>
        <InputSelect
          aria-labelledby="aria-projects-perspective"
          className="sw-mr-4 sw-body-sm"
          onChange={(data: LabelValueSelectOption<string>) => this.handleChange(data)}
          options={options}
          placeholder={translate('project_activity.filter_events')}
          size="small"
          value={options.find((option) => option.value === view)}
        />
      </div>
    );
  }
}

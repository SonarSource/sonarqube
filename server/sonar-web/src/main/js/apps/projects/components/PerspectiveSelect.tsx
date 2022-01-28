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
import { omit } from 'lodash';
import * as React from 'react';
import { components, OptionProps } from 'react-select';
import Select from '../../../components/controls/Select';
import BubblesIcon from '../../../components/icons/BubblesIcon';
import ListIcon from '../../../components/icons/ListIcon';
import { translate } from '../../../helpers/l10n';
import { VIEWS, VISUALIZATIONS } from '../utils';

interface Props {
  className?: string;
  onChange: (x: { view: string; visualization?: string }) => void;
  view: string;
  visualization?: string;
}

export interface PerspectiveOption {
  type: string;
  value: string;
  label: string;
}

export default class PerspectiveSelect extends React.PureComponent<Props> {
  handleChange = (option: PerspectiveOption) => {
    if (option && option.type === 'view') {
      this.props.onChange({ view: option.value });
    } else if (option && option.type === 'visualization') {
      this.props.onChange({ view: 'visualizations', visualization: option.value });
    }
  };

  perspectiveOptionRender = (props: OptionProps<PerspectiveOption, false>) => {
    const { data, className } = props;
    return (
      <components.Option
        {...omit(props, ['children', 'className'])}
        className={`it__projects-perspective-option-${data.value} ${className}`}>
        {data.type === 'view' && <ListIcon className="little-spacer-right" />}
        {data.type === 'visualization' && <BubblesIcon className="little-spacer-right" />}
        {props.children}
      </components.Option>
    );
  };

  render() {
    const { view, visualization } = this.props;
    const perspective = view === 'visualizations' ? visualization : view;
    const options: PerspectiveOption[] = [
      ...VIEWS.map(opt => ({
        type: 'view',
        value: opt.value,
        label: translate('projects.view', opt.label)
      })),
      ...VISUALIZATIONS.map(opt => ({
        type: 'visualization',
        value: opt,
        label: translate('projects.visualization', opt)
      }))
    ];
    return (
      <div className={this.props.className}>
        <label id="aria-projects-perspective">{translate('projects.perspective')}:</label>
        <Select
          aria-labelledby="aria-projects-perspective"
          className="little-spacer-left input-medium it__projects-perspective-select"
          isClearable={false}
          onChange={this.handleChange}
          components={{
            Option: this.perspectiveOptionRender
          }}
          options={options}
          isSearchable={false}
          value={options.find(option => option.value === perspective)}
        />
      </div>
    );
  }
}

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
import { WrappedComponentProps, injectIntl } from 'react-intl';
import { DatePicker, FacetBox } from '~design-system';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Query } from '../query';

interface Props {
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: keyof Query) => void;
  open: boolean;
  value?: Date;
}

class AvailableSinceFacet extends React.PureComponent<Props & WrappedComponentProps> {
  property: keyof Query = 'availableSince';

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ availableSince: undefined });
  };

  handlePeriodChange = (date: Date | undefined) => {
    this.props.onChange({ availableSince: date });
  };

  render() {
    const { open, value } = this.props;
    const headerId = `facet_${this.property}`;
    const count = value ? 1 : undefined;
    return (
      <FacetBox
        className="it__search-navigator-facet-box"
        clearIconLabel={translate('clear')}
        data-property={this.property}
        id={headerId}
        name={translate('coding_rules.facet.available_since')}
        onClear={this.handleClear}
        onClick={this.handleHeaderClick}
        open={open}
        count={count}
        countLabel={count ? translateWithParameters('x_selected', count) : undefined}
      >
        {open && (
          <DatePicker
            name="available-since"
            clearButtonLabel={translate('clear')}
            onChange={this.handlePeriodChange}
            placeholder={translate('date')}
            value={value}
            showClearButton
            alignRight
            size="auto"
          />
        )}
      </FacetBox>
    );
  }
}

export default injectIntl(AvailableSinceFacet);

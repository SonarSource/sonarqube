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
import { Button, ButtonLink } from '../../components/controls/buttons';
import HelpTooltip from '../../components/controls/HelpTooltip';
import OpenCloseIcon from '../../components/icons/OpenCloseIcon';
import DeferredSpinner from '../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../helpers/l10n';
import Tooltip from '../controls/Tooltip';

interface Props {
  children?: React.ReactNode;
  fetching?: boolean;
  helper?: string;
  disabled?: boolean;
  disabledHelper?: string;
  name: string;
  onClear?: () => void;
  onClick?: () => void;
  open: boolean;
  values?: string[];
}

export default class FacetHeader extends React.PureComponent<Props> {
  handleClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.nativeEvent.preventDefault();
    if (this.props.onClick) {
      this.props.onClick();
    }
  };

  renderHelper() {
    if (!this.props.helper) {
      return null;
    }
    return <HelpTooltip className="spacer-left" overlay={this.props.helper} />;
  }

  renderValueIndicator() {
    const { values } = this.props;
    if (!values || !values.length) {
      return null;
    }
    const value =
      values.length === 1 ? values[0] : translateWithParameters('x_selected', values.length);
    return (
      <span className="badge text-ellipsis" title={value}>
        {value}
      </span>
    );
  }

  render() {
    const { disabled, values, disabledHelper, name, open, children, fetching } = this.props;
    const showClearButton = values != null && values.length > 0 && this.props.onClear != null;
    const header = disabled ? (
      <Tooltip overlay={disabledHelper} accessible={false}>
        <ButtonLink
          className="disabled"
          aria-disabled={true}
          aria-label={`${name}, ${disabledHelper}`}
        >
          {name}
        </ButtonLink>
      </Tooltip>
    ) : (
      name
    );
    return (
      <div className="search-navigator-facet-header-wrapper display-flex-center">
        {this.props.onClick ? (
          <span className="search-navigator-facet-header display-flex-center">
            <button
              className="button-link"
              type="button"
              onClick={this.handleClick}
              aria-expanded={open}
              tabIndex={0}
            >
              <OpenCloseIcon className="little-spacer-right" open={open} />
              {header}
            </button>
            {this.renderHelper()}
          </span>
        ) : (
          <span className="search-navigator-facet-header display-flex-center">
            {header}
            {this.renderHelper()}
          </span>
        )}

        {children}

        <span className="search-navigator-facet-header-value spacer-left spacer-right ">
          {this.renderValueIndicator()}
        </span>

        {fetching && (
          <span className="little-spacer-right">
            <DeferredSpinner />
          </span>
        )}

        {showClearButton && (
          <Button
            className="search-navigator-facet-header-button button-small button-red"
            aria-label={translateWithParameters('clear_x_filter', name)}
            onClick={this.props.onClear}
          >
            {translate('clear')}
          </Button>
        )}
      </div>
    );
  }
}

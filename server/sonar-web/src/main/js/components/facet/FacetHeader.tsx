/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import OpenCloseIcon from 'sonar-ui-common/components/icons/OpenCloseIcon';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';

interface Props {
  children?: React.ReactNode;
  clearLabel?: string;
  fetching?: boolean;
  helper?: string;
  name: React.ReactNode;
  onClear?: () => void;
  onClick?: () => void;
  open: boolean;
  values?: string[];
}

export default class FacetHeader extends React.PureComponent<Props> {
  handleClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
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
    const showClearButton =
      this.props.values != null && this.props.values.length > 0 && this.props.onClear != null;

    return (
      <div className="search-navigator-facet-header-wrapper display-flex-center">
        {this.props.onClick ? (
          <span className="search-navigator-facet-header display-flex-center">
            <a href="#" onClick={this.handleClick}>
              <OpenCloseIcon className="little-spacer-right" open={this.props.open} />
              {this.props.name}
            </a>
            {this.renderHelper()}
          </span>
        ) : (
          <span className="search-navigator-facet-header display-flex-center">
            {this.props.name}
            {this.renderHelper()}
          </span>
        )}

        {this.props.children}

        <span className="search-navigator-facet-header-value spacer-left spacer-right ">
          {this.renderValueIndicator()}
        </span>

        {this.props.fetching && (
          <span className="little-spacer-right">
            <DeferredSpinner />
          </span>
        )}

        {showClearButton && (
          <Button
            className="search-navigator-facet-header-button button-small button-red"
            onClick={this.props.onClear}>
            {translate(this.props.clearLabel || 'clear')}
          </Button>
        )}
      </div>
    );
  }
}

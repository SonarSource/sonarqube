/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';

export const TooltipsMixin = {
  componentDidMount() {
    this.initTooltips();
  },

  componentWillUpdate() {
    this.hideTooltips();
  },

  componentDidUpdate() {
    this.initTooltips();
  },

  componentWillUnmount() {
    this.destroyTooltips();
  },

  initTooltips() {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this)).tooltip({
        container: 'body',
        placement: 'bottom',
        html: true
      });
    }
  },

  hideTooltips() {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this)).tooltip('hide');
    }
  },

  destroyTooltips() {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this)).tooltip('destroy');
    }
  }
};

export class TooltipsContainer extends React.PureComponent {
  componentDidMount() {
    this.initTooltips();
  }

  componentWillUpdate() {
    this.destroyTooltips();
  }

  componentDidUpdate() {
    this.initTooltips();
  }

  componentWillUnmount() {
    this.destroyTooltips();
  }

  initTooltips = () => {
    if ($.fn && $.fn.tooltip) {
      const options = Object.assign(
        { container: 'body', placement: 'bottom', html: true },
        this.props.options
      );
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this)).tooltip(options);
    }
  };

  hideTooltips = () => {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this)).tooltip('hide');
    }
  };

  destroyTooltips = () => {
    if ($.fn && $.fn.tooltip) {
      $('[data-toggle="tooltip"]', ReactDOM.findDOMNode(this)).tooltip('destroy');
    }
  };

  render() {
    return this.props.children;
  }
}

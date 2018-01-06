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
import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import { max } from 'd3-array';
import { scaleLinear } from 'd3-scale';
import { sortBy } from 'lodash';
import { TooltipsMixin } from './../mixins/tooltips-mixin';

export function Word(props) {
  let tooltipAttrs = {};
  if (props.tooltip) {
    tooltipAttrs = {
      'data-toggle': 'tooltip',
      title: props.tooltip
    };
  }
  return (
    <a {...tooltipAttrs} style={{ fontSize: props.size }} href={props.link}>
      {props.text}
    </a>
  );
}

Word.propTypes = {
  size: PropTypes.number.isRequired,
  text: PropTypes.string.isRequired,
  tooltip: PropTypes.string,
  link: PropTypes.string.isRequired
};

export const WordCloud = createReactClass({
  displayName: 'WordCloud',

  propTypes: {
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    sizeRange: PropTypes.arrayOf(PropTypes.number)
  },

  mixins: [TooltipsMixin],

  getDefaultProps() {
    return {
      sizeRange: [10, 24]
    };
  },

  render() {
    const len = this.props.items.length;
    const sortedItems = sortBy(this.props.items, (item, idx) => {
      const index = len - idx;
      return (index % 2) * (len - index) + index / 2;
    });

    const sizeScale = scaleLinear()
      .domain([0, max(this.props.items, d => d.size)])
      .range(this.props.sizeRange);
    const words = sortedItems.map((item, index) => (
      <Word
        key={index}
        text={item.text}
        size={sizeScale(item.size)}
        link={item.link}
        tooltip={item.tooltip}
      />
    ));
    return <div className="word-cloud">{words}</div>;
  }
});

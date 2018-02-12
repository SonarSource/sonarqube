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
import * as classNames from 'classnames';
import * as React from 'react';
import ComponentName from './ComponentName';
import ComponentMeasure from './ComponentMeasure';
import ComponentLink from './ComponentLink';
import ComponentPin from './ComponentPin';
import { Component as IComponent } from '../types';

const TOP_OFFSET = 200;
const BOTTOM_OFFSET = 10;

interface Props {
  branch?: string;
  canBrowse?: boolean;
  component: IComponent;
  previous?: IComponent;
  rootComponent: IComponent;
  selected?: boolean;
}

export default class Component extends React.PureComponent<Props> {
  node?: HTMLElement | null;

  componentDidMount() {
    this.handleUpdate();
  }

  componentDidUpdate() {
    this.handleUpdate();
  }

  handleUpdate() {
    const { selected } = this.props;

    // scroll viewport so the current selected component is visible
    if (selected) {
      setTimeout(() => {
        this.handleScroll();
      }, 0);
    }
  }

  handleScroll() {
    if (this.node) {
      const position = this.node.getBoundingClientRect();
      const { top, bottom } = position;
      if (bottom > window.innerHeight - BOTTOM_OFFSET) {
        window.scrollTo(0, bottom - window.innerHeight + window.pageYOffset + BOTTOM_OFFSET);
      } else if (top < TOP_OFFSET) {
        window.scrollTo(0, top + window.pageYOffset - TOP_OFFSET);
      }
    }
  }

  render() {
    const {
      branch,
      component,
      rootComponent,
      selected = false,
      previous,
      canBrowse = false
    } = this.props;
    const isPortfolio = ['VW', 'SVW'].includes(rootComponent.qualifier);
    const isApplication = rootComponent.qualifier === 'APP';

    let componentAction = null;

    if (!component.refKey || component.qualifier === 'SVW') {
      switch (component.qualifier) {
        case 'FIL':
        case 'UTS':
          componentAction = <ComponentPin branch={branch} component={component} />;
          break;
        default:
          componentAction = <ComponentLink branch={branch} component={component} />;
      }
    }

    const columns = isPortfolio
      ? [
          { metric: 'releasability_rating', type: 'RATING' },
          { metric: 'reliability_rating', type: 'RATING' },
          { metric: 'security_rating', type: 'RATING' },
          { metric: 'sqale_rating', type: 'RATING' },
          { metric: 'ncloc', type: 'SHORT_INT' }
        ]
      : ([
          isApplication && { metric: 'alert_status', type: 'LEVEL' },
          { metric: 'ncloc', type: 'SHORT_INT' },
          { metric: 'bugs', type: 'SHORT_INT' },
          { metric: 'vulnerabilities', type: 'SHORT_INT' },
          { metric: 'code_smells', type: 'SHORT_INT' },
          { metric: 'coverage', type: 'PERCENT' },
          { metric: 'duplicated_lines_density', type: 'PERCENT' }
        ].filter(Boolean) as Array<{ metric: string; type: string }>);

    return (
      <tr className={classNames({ selected })} ref={node => (this.node = node)}>
        <td className="thin nowrap">
          <span className="spacer-right">{componentAction}</span>
        </td>
        <td className="code-name-cell">
          <ComponentName
            branch={branch}
            component={component}
            rootComponent={rootComponent}
            previous={previous}
            canBrowse={canBrowse}
          />
        </td>

        {columns.map(column => (
          <td key={column.metric} className="thin nowrap text-right">
            <div className="code-components-cell">
              <ComponentMeasure
                component={component}
                metricKey={column.metric}
                metricType={column.type}
              />
            </div>
          </td>
        ))}
      </tr>
    );
  }
}

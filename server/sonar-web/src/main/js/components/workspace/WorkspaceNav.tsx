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
import { ComponentDescriptor, RuleDescriptor } from './context';
import WorkspaceNavComponent from './WorkspaceNavComponent';
import WorkspaceNavRule from './WorkspaceNavRule';

export interface Props {
  components: ComponentDescriptor[];
  rules: RuleDescriptor[];
  onComponentClose: (componentKey: string) => void;
  onComponentOpen: (componentKey: string) => void;
  onRuleClose: (ruleKey: string) => void;
  onRuleOpen: (ruleKey: string) => void;
  open: { component?: string; rule?: string };
}

export default function WorkspaceNav(props: Props) {
  // do not show a tab for the currently open component/rule
  const components = props.components.filter(x => x.key !== props.open.component);
  const rules = props.rules.filter(x => x.key !== props.open.rule);

  return (
    <nav className="workspace-nav">
      <ul className="workspace-nav-list">
        {components.map(component => (
          <WorkspaceNavComponent
            component={component}
            key={`component-${component.key}`}
            onClose={props.onComponentClose}
            onOpen={props.onComponentOpen}
          />
        ))}

        {rules.map(rule => (
          <WorkspaceNavRule
            key={`rule-${rule.key}`}
            onClose={props.onRuleClose}
            onOpen={props.onRuleOpen}
            rule={rule}
          />
        ))}
      </ul>
    </nav>
  );
}

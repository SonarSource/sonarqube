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
import classNames from 'classnames';
import { Breadcrumbs, ClipboardIconButton, HoverLink } from 'design-system';
import * as React from 'react';
import { getBreadcrumbs } from '../../../api/components';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branch-like';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { collapsePath, limitComponentName } from '../../../helpers/path';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier, isProject } from '../../../types/component';
import { ComponentMeasure, ComponentMeasureIntern } from '../../../types/types';

interface Props {
  backToFirst: boolean;
  branchLike?: BranchLike;
  className?: string;
  component: ComponentMeasure;
  handleSelect: (component: ComponentMeasureIntern) => void;
  rootComponent: ComponentMeasure;
}

interface State {
  breadcrumbs: ComponentMeasure[];
}

export default class MeasuresBreadcrumbs extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { breadcrumbs: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchBreadcrumbs();
    document.addEventListener('keydown', this.handleKeyDown);
  }

  componentDidUpdate(prevProps: Props) {
    if (
      this.props.component !== prevProps.component ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike)
    ) {
      this.fetchBreadcrumbs();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.removeEventListener('keydown', this.handleKeyDown);
  }

  handleKeyDown = (event: KeyboardEvent) => {
    if (event.key === KeyboardKeys.LeftArrow) {
      event.preventDefault();
      const { breadcrumbs } = this.state;
      if (breadcrumbs.length > 1) {
        const idx = this.props.backToFirst ? 0 : breadcrumbs.length - 2;
        this.props.handleSelect(breadcrumbs[idx]);
      }
    }
  };

  fetchBreadcrumbs = () => {
    const { branchLike, component, rootComponent } = this.props;
    const isRoot = component.key === rootComponent.key;
    if (isRoot) {
      if (this.mounted) {
        this.setState({ breadcrumbs: [component] });
      }
      return;
    }
    getBreadcrumbs({ component: component.key, ...getBranchLikeQuery(branchLike) }).then(
      (breadcrumbs) => {
        if (this.mounted) {
          this.setState({ breadcrumbs });
        }
      },
      () => {},
    );
  };

  render() {
    const { breadcrumbs } = this.state;
    const lastBreadcrumb = breadcrumbs[breadcrumbs.length - 1];

    if (breadcrumbs.length <= 0) {
      return null;
    }

    return (
      <Breadcrumbs
        ariaLabel={translate('breadcrumbs')}
        className={classNames(this.props.className)}
        maxWidth={500}
        actions={
          !isProject(lastBreadcrumb.qualifier) &&
          lastBreadcrumb.path && <ClipboardIconButton copyValue={lastBreadcrumb.path} />
        }
      >
        {breadcrumbs.map((component) => (
          <HoverLink
            key={component.key}
            to="#"
            onClick={(event: React.MouseEvent<HTMLAnchorElement>) => {
              event.preventDefault();
              event.currentTarget.blur();
              this.props.handleSelect(component);
            }}
          >
            {component.qualifier === ComponentQualifier.Directory
              ? collapsePath(component.name, 15)
              : limitComponentName(component.name)}
          </HoverLink>
        ))}
      </Breadcrumbs>
    );
  }
}

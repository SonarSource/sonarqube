/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { last } from 'lodash';
import * as React from 'react';
import { Link } from 'react-router';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { isMainBranch } from '../../../../helpers/branch-like';
import { getProjectUrl } from '../../../../helpers/urls';
import { BranchLike } from '../../../../types/branch-like';

interface Props {
  component: T.Component;
  currentBranchLike: BranchLike | undefined;
}

export function Breadcrumb(props: Props) {
  const {
    component: { breadcrumbs },
    currentBranchLike
  } = props;
  const lastBreadcrumbElement = last(breadcrumbs);
  const isNoMainBranch = currentBranchLike && !isMainBranch(currentBranchLike);

  return (
    <div className="big flex-shrink display-flex-center">
      {breadcrumbs.map((breadcrumbElement, i) => {
        const isFirst = i === 0;
        const isNotLast = i < breadcrumbs.length - 1;

        return (
          <span className="flex-shrink display-flex-center" key={breadcrumbElement.key}>
            {isFirst && lastBreadcrumbElement && (
              <QualifierIcon className="spacer-right" qualifier={lastBreadcrumbElement.qualifier} />
            )}
            {isNoMainBranch || isNotLast ? (
              <Link
                className="link-no-underline text-ellipsis"
                title={breadcrumbElement.name}
                to={getProjectUrl(breadcrumbElement.key)}>
                {breadcrumbElement.name}
              </Link>
            ) : (
              <span className="text-ellipsis" title={breadcrumbElement.name}>
                {breadcrumbElement.name}
              </span>
            )}
            {isNotLast && <span className="slash-separator" />}
          </span>
        );
      })}
    </div>
  );
}

export default React.memo(Breadcrumb);

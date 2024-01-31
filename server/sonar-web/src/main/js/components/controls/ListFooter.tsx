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
import styled from '@emotion/styled';
import classNames from 'classnames';
import { ButtonSecondary, Spinner, themeColor } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import { MetricType } from '../../types/metrics';
import LegacySpinner from '../ui/Spinner';
import { Button } from './buttons';

export interface ListFooterProps {
  loadMoreAriaLabel?: string;
  count: number;
  className?: string;
  loading?: boolean;
  loadMore?: () => void;
  needReload?: boolean;
  pageSize?: number;
  reload?: () => void;
  ready?: boolean;
  total?: number;
  useMIUIButtons?: boolean;
}

export default function ListFooter(props: ListFooterProps) {
  const {
    loadMoreAriaLabel,
    className,
    count,
    loadMore,
    loading = false,
    needReload,
    total,
    pageSize,
    ready = true,
    useMIUIButtons = false,
  } = props;

  const rootNode = React.useRef<HTMLDivElement>(null);

  const onLoadMore = React.useCallback(() => {
    if (loadMore) {
      loadMore();
    }

    if (rootNode.current) {
      rootNode.current.focus();
    }
  }, [loadMore, rootNode]);

  let hasMore = false;
  if (total !== undefined) {
    hasMore = total > count;
  } else if (pageSize !== undefined) {
    hasMore = count % pageSize === 0;
  }

  let button;
  if (needReload && props.reload) {
    button = React.createElement(
      useMIUIButtons ? ButtonSecondary : Button,
      {
        'data-test': 'reload',
        className: classNames('sw-ml-2', { 'sw-body-sm': useMIUIButtons }),
        disabled: loading,
        onClick: props.reload,
      } as Button['props'],
      translate('reload'),
    );
  } else if (hasMore && props.loadMore) {
    button = React.createElement(
      useMIUIButtons ? ButtonSecondary : Button,
      {
        'aria-label': loadMoreAriaLabel,
        'data-test': 'show-more',
        className: classNames('sw-ml-2', { 'sw-body-sm': useMIUIButtons }),
        disabled: loading,
        onClick: onLoadMore,
      } as Button['props'],
      translate('show_more'),
    );
  }

  return (
    <StyledDiv
      tabIndex={-1}
      ref={rootNode}
      className={classNames(
        'list-footer', // .list-footer is only used by Selenium tests; we should find a way to remove it.
        'sw-body-sm sw-flex sw-items-center sw-justify-center',
        { 'sw-opacity-50 sw-duration-500 sw-ease-in-out': !ready },
        className,
      )}
    >
      <span aria-live="polite" aria-busy={loading}>
        {total !== undefined
          ? translateWithParameters(
              'x_of_y_shown',
              formatMeasure(count, MetricType.Integer),
              formatMeasure(total, MetricType.Integer),
            )
          : translateWithParameters('x_show', formatMeasure(count, MetricType.Integer))}
      </span>
      {button}
      {/* eslint-disable local-rules/no-conditional-rendering-of-deferredspinner */}
      {useMIUIButtons ? (
        <Spinner loading={loading} className="sw-ml-2" />
      ) : (
        <LegacySpinner loading={loading} className="sw-ml-2" />
      )}
    </StyledDiv>
  );
}

const StyledDiv = styled.div`
  color: ${themeColor('pageContentLight')};

  margin-top: 1rem /* 16px */;
`;

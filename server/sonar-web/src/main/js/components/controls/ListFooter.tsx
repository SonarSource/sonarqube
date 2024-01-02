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
import * as React from 'react';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import DeferredSpinner from '../ui/DeferredSpinner';
import { Button } from './buttons';

export interface ListFooterProps {
  accessibleLoadMoreLabel?: string;
  count: number;
  className?: string;
  loading?: boolean;
  loadMore?: () => void;
  needReload?: boolean;
  pageSize?: number;
  reload?: () => void;
  ready?: boolean;
  total?: number;
}

export default function ListFooter(props: ListFooterProps) {
  const {
    accessibleLoadMoreLabel,
    className,
    count,
    loadMore,
    loading = false,
    needReload,
    total,
    pageSize,
    ready = true,
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
    button = (
      <Button className="spacer-left" data-test="reload" disabled={loading} onClick={props.reload}>
        {translate('reload')}
      </Button>
    );
  } else if (hasMore && props.loadMore) {
    button = (
      <Button
        aria-label={accessibleLoadMoreLabel}
        className="spacer-left"
        disabled={loading}
        data-test="show-more"
        onClick={onLoadMore}
      >
        {translate('show_more')}
      </Button>
    );
  }

  return (
    <div
      tabIndex={-1}
      ref={rootNode}
      className={classNames(
        'list-footer spacer-top note text-center',
        { 'new-loading': !ready },
        className
      )}
    >
      <span aria-live="polite" aria-busy={loading}>
        {total !== undefined
          ? translateWithParameters(
              'x_of_y_shown',
              formatMeasure(count, 'INT', null),
              formatMeasure(total, 'INT', null)
            )
          : translateWithParameters('x_show', formatMeasure(count, 'INT', null))}
      </span>
      {button}
      {<DeferredSpinner loading={loading} className="text-bottom spacer-left position-absolute" />}
    </div>
  );
}

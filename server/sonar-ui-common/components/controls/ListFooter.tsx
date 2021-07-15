/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import DeferredSpinner from '../ui/DeferredSpinner';
import { Button } from './buttons';

export interface ListFooterProps {
  count: number;
  className?: string;
  loading?: boolean;
  loadMore?: () => void;
  needReload?: boolean;
  reload?: () => void;
  ready?: boolean;
  total?: number;
}

export default function ListFooter(props: ListFooterProps) {
  const { className, count, loading, needReload, total, ready = true } = props;
  const hasMore = total && total > count;

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
        className="spacer-left"
        disabled={loading}
        data-test="show-more"
        onClick={props.loadMore}>
        {translate('show_more')}
      </Button>
    );
  }

  return (
    <footer
      className={classNames('spacer-top note text-center', { 'new-loading': !ready }, className)}>
      {translateWithParameters(
        'x_of_y_shown',
        formatMeasure(count, 'INT', null),
        formatMeasure(total, 'INT', null)
      )}
      {button}
      {loading && <DeferredSpinner className="text-bottom spacer-left position-absolute" />}
    </footer>
  );
}

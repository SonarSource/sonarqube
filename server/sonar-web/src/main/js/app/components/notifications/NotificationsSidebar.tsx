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
import * as classNames from 'classnames';
import * as differenceInSeconds from 'date-fns/difference_in_seconds';
import * as React from 'react';
import { ClearButton } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { PrismicFeatureNews } from '../../../api/news';
import DateFormatter from '../../../components/intl/DateFormatter';

export interface Props {
  fetchMoreFeatureNews: () => void;
  loading: boolean;
  loadingMore: boolean;
  news: PrismicFeatureNews[];
  onClose: () => void;
  notificationsLastReadDate?: Date;
  paging?: T.Paging;
}

export default function NotificationsSidebar(props: Props) {
  const { loading, loadingMore, news, notificationsLastReadDate, paging } = props;
  const header = translate('embed_docs.whats_new');
  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <div className="notifications-sidebar">
        <div className="notifications-sidebar-top">
          <h3>{header}</h3>
          <ClearButton
            className="button-tiny"
            iconProps={{ size: 12, thin: true }}
            onClick={props.onClose}
          />
        </div>
        <div className="notifications-sidebar-content">
          {loading ? (
            <div className="text-center">
              <DeferredSpinner className="big-spacer-top" timeout={200} />
            </div>
          ) : (
            news.map((slice, index) => (
              <Notification
                key={slice.publicationDate}
                notification={slice}
                unread={isUnread(index, slice.publicationDate, notificationsLastReadDate)}
              />
            ))
          )}
        </div>
        {!loading && paging && paging.total > news.length && (
          <div className="notifications-sidebar-footer">
            <div className="spacer-top note text-center">
              <a className="spacer-left" href="#" onClick={props.fetchMoreFeatureNews}>
                {translate('show_more')}
              </a>
              {loadingMore && (
                <DeferredSpinner className="text-bottom spacer-left position-absolute" />
              )}
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}

export function isUnread(index: number, notificationDate: string, lastReadDate?: Date) {
  return !lastReadDate ? index < 1 : differenceInSeconds(notificationDate, lastReadDate) > 0;
}

interface NotificationProps {
  notification: PrismicFeatureNews;
  unread: boolean;
}

export function Notification({ notification, unread }: NotificationProps) {
  return (
    <div className={classNames('notifications-sidebar-slice', { unread })}>
      <h4>
        <DateFormatter date={notification.publicationDate} long={false} />
      </h4>
      {notification.features.map((feature, index) => (
        <Feature feature={feature} key={index} />
      ))}
    </div>
  );
}

interface FeatureProps {
  feature: PrismicFeatureNews['features'][0];
}

export function Feature({ feature }: FeatureProps) {
  return (
    <div className="feature">
      <ul className="categories spacer-bottom">
        {feature.categories.map(category => (
          <li key={category.name} style={{ backgroundColor: category.color }}>
            {category.name}
          </li>
        ))}
      </ul>
      <span>{feature.description}</span>
      {feature.readMore && (
        <a
          className="learn-more"
          href={feature.readMore}
          rel="noopener noreferrer nofollow"
          target="_blank">
          {translate('learn_more')}
        </a>
      )}
    </div>
  );
}

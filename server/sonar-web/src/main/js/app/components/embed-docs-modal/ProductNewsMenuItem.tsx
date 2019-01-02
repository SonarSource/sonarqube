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
import { connect } from 'react-redux';
import { fetchPrismicRefs, fetchPrismicNews, PrismicNews } from '../../../api/news';
import { getGlobalSettingValue, Store } from '../../../store/rootReducer';
import DateFormatter from '../../../components/intl/DateFormatter';
import ChevronRightIcon from '../../../components/icons-components/ChevronRightcon';
import PlaceholderBar from '../../../components/ui/PlaceholderBar';
import { translate } from '../../../helpers/l10n';

interface OwnProps {
  tag?: string;
}

interface StateProps {
  accessToken?: string;
}

type Props = OwnProps & StateProps;

interface State {
  loading: boolean;
  news?: PrismicNews;
}

export class ProductNewsMenuItem extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchProductNews();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchProductNews = () => {
    const { accessToken, tag } = this.props;
    if (accessToken) {
      this.setState({ loading: true });
      fetchPrismicRefs()
        .then(({ ref }) => fetchPrismicNews({ accessToken, ref, tag }))
        .then(
          news => {
            if (this.mounted) {
              this.setState({ news: news[0], loading: false });
            }
          },
          () => {
            if (this.mounted) {
              this.setState({ loading: false });
            }
          }
        );
    }
  };

  renderPlaceholder() {
    return (
      <a className="rich-item new-loading">
        <div className="flex-1">
          <div className="display-inline-flex-center">
            <h4>{translate('embed_docs.latest_blog')}</h4>
            <span className="note spacer-left">
              <PlaceholderBar color="#aaa" width={60} />
            </span>
          </div>
          <p className="little-spacer-bottom">
            <PlaceholderBar color="#aaa" width={84} /> <PlaceholderBar color="#aaa" width={48} />{' '}
            <PlaceholderBar color="#aaa" width={24} /> <PlaceholderBar color="#aaa" width={72} />{' '}
            <PlaceholderBar color="#aaa" width={24} /> <PlaceholderBar color="#aaa" width={48} />
          </p>
        </div>
        <ChevronRightIcon className="flex-0" />
      </a>
    );
  }

  render() {
    const link = 'https://blog.sonarsource.com/';
    const { loading, news } = this.state;

    if (loading) {
      return this.renderPlaceholder();
    }

    if (!news) {
      return null;
    }

    return (
      <a className="rich-item" href={link + news.uid} rel="noopener noreferrer" target="_blank">
        <div className="flex-1">
          <div className="display-inline-flex-center">
            <h4>{translate('embed_docs.latest_blog')}</h4>
            <DateFormatter date={news.last_publication_date}>
              {formattedDate => <span className="note spacer-left">{formattedDate}</span>}
            </DateFormatter>
          </div>
          <p className="little-spacer-bottom">{news.data.title}</p>
        </div>
        <ChevronRightIcon className="flex-0" />
      </a>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => {
  const accessToken = getGlobalSettingValue(state, 'sonar.prismic.accessToken');
  return {
    accessToken: accessToken && accessToken.value
  };
};

export default connect(mapStateToProps)(ProductNewsMenuItem);

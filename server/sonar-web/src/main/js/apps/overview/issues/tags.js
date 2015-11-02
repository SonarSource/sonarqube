import React from 'react';
import { DomainHeader } from '../domain/header';
import { WordCloud } from '../../../components/charts/word-cloud';
import { componentIssuesUrl } from '../../../helpers/Url';
import { formatMeasure } from '../../../helpers/measures';


export default class extends React.Component {
  renderWordCloud () {
    let tags = this.props.tags.map(tag => {
      let link = componentIssuesUrl(this.props.component.key, { resolved: 'false', tags: tag.val });
      let tooltip = `Issues: ${formatMeasure(tag.count, 'SHORT_INT')}`;
      return { text: tag.val, size: tag.count, link, tooltip };
    });
    return <WordCloud items={tags}/>;
  }

  render () {
    return <div className="overview-domain-section">
      <DomainHeader title="Issues By Tag"/>
      <div>
        {this.renderWordCloud()}
      </div>
    </div>;
  }
}

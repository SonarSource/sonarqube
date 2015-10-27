import React from 'react';
import Cards from './cards';
import DetailsLink from './details-link';


function checkMeasureForDomain (domain, measures) {
  if (domain === 'coverage' && measures.coverage == null) {
    return false;
  }
  if (domain === 'duplications' && measures.duplications == null) {
    return false;
  }
  return true;
}


export default React.createClass({
  render() {
    let domains = ['issues', 'coverage', 'duplications', 'size'].map(domain => {
      if (!checkMeasureForDomain(domain, this.props.measures)) {
        return null;
      }
      let active = domain === this.props.section;
      return <DetailsLink key={domain} linkTo={domain} onRoute={this.props.onRoute} active={active}/>;
    });

    return (
        <div className="overview-more">
          <h2 className="overview-title">More Details</h2>
          <Cards>{domains}</Cards>
        </div>
    );
  }
});

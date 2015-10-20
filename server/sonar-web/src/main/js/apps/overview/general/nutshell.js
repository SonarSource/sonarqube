import React from 'react';
import Cards from './cards';
import NutshellIssues from './nutshell-issues';
import NutshellCoverage from './nutshell-coverage';
import NutshellSize from './nutshell-size';
import NutshellDups from './nutshell-dups';

export default React.createClass({
  render() {
    let props = {
      measures: this.props.measures,
      component: this.props.component,
      section: this.props.section,
      onRoute: this.props.onRoute
    };
    return (
        <div className="overview-nutshell">
          <h2 className="overview-title">{window.t('overview.project_in_a_nutshell')}</h2>
          <Cards>
            <NutshellIssues {...props}/>
            <NutshellCoverage {...props}/>
            <NutshellDups {...props}/>
            <NutshellSize {...props}/>
          </Cards>
        </div>
    );
  }
});

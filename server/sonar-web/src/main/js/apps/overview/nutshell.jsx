import React from 'react';
import Cards from './cards';
import NutshellIssues from './nutshell-issues';
import NutshellCoverage from './nutshell-coverage';
import NutshellSize from './nutshell-size';
import NutshellDups from './nutshell-dups';

export default React.createClass({
  render() {
    const props = { measures: this.props.measures, component: this.props.component };
    return (
        <div className="overview-nutshell">
          <div className="overview-title">{window.t('overview.project_in_a_nutshell')}</div>
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

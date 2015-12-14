import React from 'react';

import Truncated from './Truncated';
import QualifierIcon from '../../../components/shared/qualifier-icon';


// TODO collapse dirs

const Component = ({ component, onBrowse }) => {
  const handleClick = (e) => {
    e.preventDefault();
    onBrowse(component);
  };

  return (
      <Truncated title={component.name}>
        <QualifierIcon qualifier={component.qualifier}/>
        {' '}
        {onBrowse ? (
            <a
                onClick={handleClick}
                href="#">
              {component.name}
            </a>
        ) : (
            <span>
              {component.name}
            </span>
        )}
      </Truncated>
  );
};


export default Component;

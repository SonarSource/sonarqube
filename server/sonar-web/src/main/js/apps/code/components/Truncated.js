import React from 'react';


const Truncated = ({ children, title }) => (
    <span
        className="code-truncated"
        title={title}>
      {children}
    </span>
);


export default Truncated;

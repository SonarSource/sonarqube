import React from 'react';

import RadioToggle from '../../components/shared/radio-toggle';


const rootQualifiersToOptions = (qualifiers) => {
  return qualifiers.map(q => {
    return {
      value: q,
      label: window.t('qualifiers', q)
    };
  });
};


export const QualifierFilter = ({ rootQualifiers, filter, onFilter }) => {
  const options = [{ value: '__ALL__', label: 'All' }, ...rootQualifiersToOptions(rootQualifiers)];

  return (
      <div className="display-inline-block text-top nowrap big-spacer-right">
        <RadioToggle value={filter}
                     options={options}
                     name="qualifier"
                     onCheck={onFilter}/>
      </div>
  );
};

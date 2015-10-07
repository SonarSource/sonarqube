import $ from 'jquery';
import {setLogLevel} from '../../api/system';

const LOG_LEVELS = ['INFO', 'DEBUG', 'TRACE'];

let cell = document.querySelector('#sonarqube-logs-level td:last-child');
if (cell) {
  let currentValue = cell.textContent.trim();

  while (cell.hasChildNodes()) {
    cell.removeChild(cell.lastChild);
  }

  let select = document.createElement('select');
  cell.appendChild(select);

  LOG_LEVELS.forEach(logLevel => {
    let option = document.createElement('option');
    option.value = logLevel;
    option.text = logLevel;
    option.selected = logLevel === currentValue;
    select.appendChild(option);
  });

  let format = (state) => {
    let className = state.id !== 'INFO' ? 'text-danger' : '';
    return `<span class="${className}">${state.id}</span>`;
  };

  $(select)
      .select2({
        width: '120px',
        minimumResultsForSearch: 999,
        formatResult: format,
        formatSelection: format
      })
      .on('change', () => {
        let newValue = select.value;
        setLogLevel(newValue);
      });
}

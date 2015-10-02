import $ from 'jquery';
import {setLogLevel} from '../../api/system';

const LOG_LEVELS = ['INFO', 'DEBUG', 'TRACE'];

window.sonarqube.appStarted.then(() => {
  let cell = $('#sonarqube-logs-level').find('td:last-child');
  if (cell.length) {
    let currentValue = cell.text().trim();
    cell.empty();

    let select = $('<select>');
    cell.append(select);

    LOG_LEVELS.forEach(logLevel => {
      let option = $('<option>');
      option.prop('value', logLevel);
      option.text(logLevel);
      option.prop('selected', logLevel === currentValue);
      select.append(option);
    });

    let format = (state) => {
      let className = state.id !== 'INFO' ? 'text-danger' : '';
      return `<span class="${className}">${state.id}</span>`;
    };

    let warning = $('<div>')
        .addClass('spacer-top text-danger')
        .text(window.t('system.log_level.warning'));

    function placeWarning () {
      if (select.val() === 'INFO') {
        warning.detach();
      } else {
        warning.insertAfter(select);
      }
    }

    placeWarning();

    $(select)
        .select2({
          width: '120px',
          minimumResultsForSearch: 999,
          formatResult: format,
          formatSelection: format
        })
        .on('change', () => {
          let newValue = select.val();
          setLogLevel(newValue);
          placeWarning();
        });
  }
});



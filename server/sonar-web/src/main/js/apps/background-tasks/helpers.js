const ONE_SECOND = 1000;
const ONE_MINUTE = 60 * ONE_SECOND;

function format(int, suffix) {
  return `${int}${suffix}`;
}

export function formatDuration(value) {
  if (!value) {
    return '';
  }
  if (value >= ONE_MINUTE) {
    let minutes = Math.round(value / ONE_MINUTE);
    return format(minutes, 'min');
  } else if (value >= ONE_SECOND) {
    let seconds = Math.round(value / ONE_SECOND);
    return format(seconds, 's');
  } else {
    return format(value, 'ms');
  }
}

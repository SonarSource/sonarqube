import _ from 'underscore';


export function collapsePath (path, limit = 30) {
  if (typeof path !== 'string') {
    return '';
  }

  var tokens = path.split('/');

  if (tokens.length <= 2) {
    return path;
  }

  var head = _.first(tokens),
      tail = _.last(tokens),
      middle = _.initial(_.rest(tokens)),
      cut = false;

  while (middle.join().length > limit && middle.length > 0) {
    middle.shift();
    cut = true;
  }

  var body = [].concat(head, cut ? ['...'] : [], middle, tail);
  return body.join('/');
}

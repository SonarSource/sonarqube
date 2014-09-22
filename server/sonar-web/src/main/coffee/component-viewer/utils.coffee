define ->

  splitLongName: (longName) ->
    lastSeparator = longName.lastIndexOf '/'
    if lastSeparator == -1
      lastSeparator = longName.lastIndexOf '.'
    dir: longName.substr 0, lastSeparator
    name: longName.substr lastSeparator + 1


  sortSeverities: (severities) ->
    order = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
    _.sortBy severities, (s) -> order.indexOf s.key


  mixOf: (base, mixins...) ->
    class Mixed extends base
    for mixin in mixins by -1 # earlier mixins override later ones
      for name, method of mixin::
        Mixed::[name] = method
    Mixed
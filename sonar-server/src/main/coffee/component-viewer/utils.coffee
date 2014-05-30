define ->

  splitLongName: (longName) ->
    lastSeparator = longName.lastIndexOf '/'
    dir: longName.substr 0, lastSeparator
    name: longName.substr lastSeparator + 1
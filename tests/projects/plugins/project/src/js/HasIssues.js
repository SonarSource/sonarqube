// FIXME
alert("should not be used");

function strict() {
  'use strict';
}

function pow(a, b) {
  if(b == 0) {
    return 0;
  }
  var x = a;
  for (var i = 1; i<b; i++) {
    //Dead store because the last return statement should return x instead of returning a
    x = x * a;
  }
  return a;
}


# fortune.py -- chooses a random fortune, as the fortune(8) program in
#               the BSD-games package does
#
# Copyright (c) 2010, Andrew M. Kuchling
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

import struct, random, string

# C long variables are different sizes on 32-bit and 64-bit machines,
# so we have to measure how big they are on the machine where this is running.
LONG_SIZE = struct.calcsize('L')
is_64_bit = (LONG_SIZE == 8)

def get(filename):
    "Select a random quotation, using a pregenerated .dat file"

    # First, we open the .dat file, and read the header information.
    # The C structure containing this info looks like:
    ## typedef struct {                         /* information table */
    ## #define  VERSION         1
    ##  unsigned long   str_version;            /* version number */
    ##  unsigned long   str_numstr;             /* # of strings in the file */
    ##  unsigned long   str_longlen;            /* length of longest string */
    ##  unsigned long   str_shortlen;           /* length of shortest string */
    ## #define  STR_RANDOM      0x1             /* randomized pointers */
    ## #define  STR_ORDERED     0x2             /* ordered pointers */
    ## #define  STR_ROTATED     0x4             /* rot-13'd text */
    ##  unsigned long   str_flags;              /* bit field for flags */
    ##  unsigned char   stuff[4];               /* long aligned space */
    ## #define  str_delim       stuff[0]        /* delimiting character */
    ## } STRFILE;

    datfile = open(filename+'.dat', 'r')
    data = datfile.read(5 * LONG_SIZE)
    if is_64_bit:
        v1, v2, n1, n2, l1, l2, s1, s2, f1, f2 = struct.unpack('!10L', data)
        version  = v1 + (v2 << 32)
        numstr   = n1 + (n2 << 32)
        longlen  = l1 + (l2 << 32)
        shortlen = s1 + (s2 << 32)
        flags    = f1 + (f2 << 32)
    else:
        version, numstr, longlen, shortlen, flags = struct.unpack('5l', data)

    delimiter = datfile.read(1)
    datfile.read(3)                     # Throw away padding bytes
    if is_64_bit: datfile.read(4)       # 64-bit machines align to 8 bytes

    # Pick a random number
    r = random.randint(0, numstr)
    datfile.seek(LONG_SIZE * r, 1)      # Seek to the chosen pointer
    data = datfile.read(LONG_SIZE * 2)

    if is_64_bit:
        s1, s2, e1, e2 = struct.unpack('!4L', data)
        start, end = s1 + (s2 << 32), e1 + (e2 << 32)
    else:
        start, end = struct.unpack('!ll', data)
    datfile.close()

    file = open(filename, 'r')
    file.seek(start)
    quotation = file.read(end-start)
    L=string.split(quotation, '\n')
    while string.strip(L[-1]) == delimiter or string.strip(L[-1]) == "":
        L=L[:-1]
    return string.join(L, '\n')

if __name__ == '__main__':
    import sys
    if len(sys.argv) == 1:
        print 'Usage: fortune.py <filename>'
        sys.exit()
    print get(sys.argv[1])

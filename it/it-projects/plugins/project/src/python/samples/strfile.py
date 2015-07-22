
# strfile.py -- write an index file for a fortune file, as the strfile(8)
#               program in the BSD-games package does
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

import struct, string, sys

if len(sys.argv)==1:
    print "Usage: strfile.py <filename>"
    sys.exit()

# C long variables are different sizes on 32-bit and 64-bit machines,
# so we have to measure how big they are on the machine where this is running.
LONG_SIZE = struct.calcsize('L')
is_64_bit = (LONG_SIZE == 8)

delimiter = '%'                         # The standard delimiter

filename = sys.argv[1]
input = open(filename, 'r')
output = open(filename + '.dat', 'w')
output.seek(LONG_SIZE * 6)              # Skip over the header for now

# Output a 32- or 64-bit integer

def write_long(x):
    if is_64_bit:
        output.write( struct.pack("!LL", x & 0xffffFFFFL, x >> 32) )
    else:
        output.write( struct.pack("!L", x) )

write_long(0)                           # Write the first pointer

# We need to track various statistics: the longest and shortest
# quotations, and their number

shortest = sys.maxint ; longest = 0
numstr = 0
quotation = ""

while (1):
    L=input.readline()                  # Get a line
    if L=="": break                     # Check for end-of-file
    if string.strip(L) != delimiter:
        # We haven't come to the end yet, so we just add the line to
        # the quotation we're building and continue
        quotation = quotation + L ; continue

    # If there's a leading % in the file, the first quotation will be
    # empty; we'll just ignore it
    if quotation == "": continue

    # Update the shortest and longest variables
    shortest = min(shortest, len(quotation) )
    longest = max(longest, len(quotation) )

    # Output the current file pointer
    write_long( input.tell() )
    numstr = numstr + 1
    quotation = ""                      # Reset the quotation to null

# To simplify the programming, we'll assume there's a trailing % line
# in the file, with no quotation following.
assert quotation == ""

input.close()

# We're done, so rewind to the beginning of the file and write the header
output.seek(0)
write_long( 1 )                         # Version
write_long(numstr)                      # Number of strings
write_long(longest)                     # Longest string length
write_long(shortest)                    # Shortest string length
write_long(0)                           # Flags; we'll set them to zero
output.write(delimiter + '\0'*(LONG_SIZE-1))
output.close()

print '''"%s.dat" created
There were %i strings
Longest string: %i bytes
Shortest string: %i bytes''' % (filename, numstr, longest, shortest)

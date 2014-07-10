#
# gradient_color.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

require File.dirname(__FILE__) + "/color_base"

class Treemap::GradientColor < Treemap::ColorBase
    attr_accessor(:min, :max, :min_color, :mean_color, :max_color, :increment)

    def initialize
        @min = -100
        @max = 100
        @min_color = "FF0000"   # red
        @mean_color = "000000"  # black
        @max_color = "00FF00"   # green
        @increment = 1

        yield self if block_given?

        # XXX add in error checking. if min >= max, if colors aren't hex, etc.
        @min = @min.to_f
        @max = @max.to_f
        @mean = (@min + @max) / 2.to_f
        @slices = (1.to_f / @increment.to_f) * (@max - @mean).to_f
    end

    def get_hex_color(value)
        value = @max if(value > @max)
        vaue = @min if(value < @min)


        r1, g1, b1 = to_rgb(@mean_color)
        r2, g2, b2 = to_rgb(@min_color)
        if(value >= @mean) 
            r2, g2, b2 = to_rgb(@max_color)
        end

        rfactor = ((r1 -r2).abs.to_f / @slices) * value.abs 
        gfactor = ((g1 -g2).abs.to_f / @slices) * value.abs
        bfactor = ((b1 -b2).abs.to_f / @slices) * value.abs

        newr = r1 + rfactor
        if(r1 > r2)
            newr = r1 - rfactor
        end

        newg = g1 + gfactor
        if(g1 > g2)
            newg = g1 - gfactor
        end

        newb = b1 + bfactor
        if(b1 > b2)
            newb = b1 - bfactor
        end

        sprintf("%02X", newr.round) + sprintf("%02X", newg.round) + sprintf("%02X", newb.round)
    end

    def get_rgb_color(value)
        to_rgb(get_hex_color(value))
    end
end

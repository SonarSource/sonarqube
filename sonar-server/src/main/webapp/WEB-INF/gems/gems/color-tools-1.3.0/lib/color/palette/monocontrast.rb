#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: monocontrast.rb,v 1.3 2005/08/08 02:44:17 austin Exp $
#++

require 'color/palette'

  # Generates a monochromatic constrasting colour palette for background and
  # foreground. What does this mean?
  #
  # Monochromatic: A single colour is used to generate the base palette, and
  # this colour is lightened five times and darkened five times to provide
  # eleven distinct colours.
  #
  # Contrasting: The foreground is also generated as a monochromatic colour
  # palettte; however, all generated colours are tested to see that they are
  # appropriately contrasting to ensure maximum readability of the
  # foreground against the background.
class Color::Palette::MonoContrast
    # Hash of CSS background colour values.
    # 
    # This is always 11 values:
    #
    # 0::       The starting colour.
    # +1..+5::  Lighter colours.
    # -1..-5::  Darker colours.
  attr_reader :background
    # Hash of CSS foreground colour values.
    # 
    # This is always 11 values:
    #
    # 0::       The starting colour.
    # +1..+5::  Lighter colours.
    # -1..-5::  Darker colours.
  attr_reader :foreground

  DEFAULT_MINIMUM_BRIGHTNESS_DIFF = (125.0 / 255.0)

    # The minimum brightness difference between the background and the
    # foreground, and must be between 0..1. Setting this value will
    # regenerate the palette based on the base colours. The default value
    # for this is 125 / 255.0. If this value is set to +nil+, it will be
    # restored to the default.
  attr_accessor :minimum_brightness_diff
  remove_method :minimum_brightness_diff= ;
  def minimum_brightness_diff=(bd) #:nodoc:
    if bd.nil?
      @minimum_brightness_diff = DEFAULT_MINIMUM_BRIGHTNESS_DIFF
    elsif bd > 1.0
      @minimum_brightness_diff = 1.0
    elsif bd < 0.0
      @minimum_brightness_diff = 0.0
    else
      @minimum_brightness_diff = bd
    end
      
    regenerate(@background[0], @foreground[0])
  end

  DEFAULT_MINIMUM_COLOR_DIFF = (500.0 / 255.0)

    # The minimum colour difference between the background and the
    # foreground, and must be between 0..3. Setting this value will
    # regenerate the palette based on the base colours. The default value
    # for this is 500 / 255.0.
  attr_accessor :minimum_color_diff
  remove_method :minimum_color_diff= ;
  def minimum_color_diff=(cd) #:noco:
    if cd.nil?
      @minimum_color_diff = DEFAULT_MINIMUM_COLOR_DIFF
    elsif cd > 3.0
      @minimum_color_diff = 3.0
    elsif cd < 0.0
      @minimum_color_diff = 0.0
    else
      @minimum_color_diff = cd
    end
    regenerate(@background[0], @foreground[0])
  end

    # Generate the initial palette.
  def initialize(background, foreground = nil)
    @minimum_brightness_diff = DEFAULT_MINIMUM_BRIGHTNESS_DIFF
    @minimum_color_diff = DEFAULT_MINIMUM_COLOR_DIFF

    regenerate(background, foreground)
  end

    # Generate the colour palettes.
  def regenerate(background, foreground = nil)
    foreground ||= background
    background = background.to_rgb
    foreground = foreground.to_rgb

    @background = {}
    @foreground = {}

    @background[-5] = background.darken_by(10)
    @background[-4] = background.darken_by(25)
    @background[-3] = background.darken_by(50)
    @background[-2] = background.darken_by(75)
    @background[-1] = background.darken_by(85)
    @background[ 0] = background
    @background[+1] = background.lighten_by(85)
    @background[+2] = background.lighten_by(75)
    @background[+3] = background.lighten_by(50)
    @background[+4] = background.lighten_by(25)
    @background[+5] = background.lighten_by(10)

    @foreground[-5] = calculate_foreground(@background[-5], foreground)
    @foreground[-4] = calculate_foreground(@background[-4], foreground)
    @foreground[-3] = calculate_foreground(@background[-3], foreground)
    @foreground[-2] = calculate_foreground(@background[-2], foreground)
    @foreground[-1] = calculate_foreground(@background[-1], foreground)
    @foreground[ 0] = calculate_foreground(@background[ 0], foreground)
    @foreground[+1] = calculate_foreground(@background[+1], foreground)
    @foreground[+2] = calculate_foreground(@background[+2], foreground)
    @foreground[+3] = calculate_foreground(@background[+3], foreground)
    @foreground[+4] = calculate_foreground(@background[+4], foreground)
    @foreground[+5] = calculate_foreground(@background[+5], foreground)
  end

    # Given a background colour and a foreground colour, modifies the
    # foreground colour so that it will have enough contrast to be seen
    # against the background colour.
    #
    # Uses #mininum_brightness_diff and #minimum_color_diff.
  def calculate_foreground(background, foreground)
    nfg = nil
      # Loop through brighter and darker versions of the foreground color.
      # The numbers here represent the amount of foreground color to mix
      # with black and white.
    [100, 75, 50, 25, 0].each do |percent|
      dfg = foreground.darken_by(percent)
      lfg = foreground.lighten_by(percent)

      dbd = brightness_diff(background, dfg)
      lbd = brightness_diff(background, lfg)

      if lbd > dbd
        nfg = lfg
        nbd = lbd
      else
        nfg = dfg
        nbd = dbd
      end

      ncd = color_diff(background, nfg)

      break if nbd >= @minimum_brightness_diff and ncd >= @minimum_color_diff
    end
    nfg
  end

    # Returns the absolute difference between the brightness levels of two
    # colours. This will be a decimal value between 0 and 1. W3C
    # accessibility guidelines for colour
    # contrast[http://www.w3.org/TR/AERT#color-contrast] suggest that this
    # value be at least approximately 0.49 (125 / 255.0) for proper contrast.
  def brightness_diff(c1, c2)
    (c1.brightness - c2.brightness).abs
  end

    # Returns the contrast between to colours, a decimal value between 0 and
    # 3. W3C accessibility guidelines for colour
    # contrast[http://www.w3.org/TR/AERT#color-contrast] suggest that this
    # value be at least approximately 1.96 (500 / 255.0) for proper contrast.
  def color_diff(c1, c2)
    r = (c1.r - c2.r).abs
    g = (c1.g - c2.g).abs
    b = (c1.b - c2.b).abs
    r + g + b
  end
end

#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: hsl.rb,v 1.2 2005/08/08 02:44:17 austin Exp $
#++

  # An HSL colour object. Internally, the hue (#h), saturation (#s), and
  # luminosity (#l) values are dealt with as fractional values in the range
  # 0..1.
class Color::HSL
  class << self
      # Creates an HSL colour object from fractional values 0..1.
    def from_fraction(h = 0.0, s = 0.0, l = 0.0)
      colour = Color::HSL.new
      colour.h = h
      colour.s = s
      colour.l = l
      colour
    end
  end

    # Compares the other colour to this one. The other colour will be
    # converted to HSL before comparison, so the comparison between a HSL
    # colour and a non-HSL colour will be approximate and based on the other
    # colour's #to_hsl conversion. If there is no #to_hsl conversion, this
    # will raise an exception. This will report that two HSL values are
    # equivalent if all component values are within 1e-4 (0.0001) of each
    # other.
  def ==(other)
    other = other.to_hsl
    other.kind_of?(Color::HSL) and
    ((@h - other.h).abs <= 1e-4) and
    ((@s - other.s).abs <= 1e-4) and
    ((@l - other.l).abs <= 1e-4)
  end

    # Creates an HSL colour object from the standard values of degrees and
    # percentages (e.g., 145º, 30%, 50%).
  def initialize(h = 0, s = 0, l = 0)
    @h = h / 360.0
    @s = s / 100.0
    @l = l / 100.0
  end

    # Present the colour as an HTML/CSS colour string.
  def html
    to_rgb.html
  end

    # Converting to HSL as adapted from Foley and Van-Dam from
    # http://www.bobpowell.net/RGBHSB.htm.
  def to_rgb(ignored = nil)
      # If luminosity is zero, the colour is always black.
    return Color::RGB.new if @l == 0
      # If luminosity is one, the colour is always white.
    return Color::RGB.new(0xff, 0xff, 0xff) if @l == 1
      # If saturation is zero, the colour is always a greyscale colour.
    return Color::RGB.new(@l, @l, @l) if @s <= 1e-5

    if (@l - 0.5) < 1e-5
      tmp2 = @l * (1.0 + @s.to_f)
    else
      tmp2 = @l + @s - (@l * @s.to_f)
    end
    tmp1 = 2.0 * @l - tmp2

    t3  = [ @h + 1.0 / 3.0, @h, @h - 1.0 / 3.0 ]
    t3 = t3.map { |tmp3|
      tmp3 += 1.0 if tmp3 < 1e-5
      tmp3 -= 1.0 if (tmp3 - 1.0) > 1e-5
      tmp3
    }

    rgb = t3.map do |tmp3|
      if ((6.0 * tmp3) - 1.0) < 1e-5
        tmp1 + ((tmp2 - tmp1) * tmp3 * 6.0)
      elsif ((2.0 * tmp3) - 1.0) < 1e-5
        tmp2
      elsif ((3.0 * tmp3) - 2.0) < 1e-5
        tmp1 + (tmp2 - tmp1) * ((2 / 3.0) - tmp3) * 6.0
      else
        tmp1
      end
    end

     Color::RGB.from_fraction(*rgb)
  end

    # Converts to RGB then YIQ.
  def to_yiq
    to_rgb.to_yiq
  end

    # Converts to RGB then CMYK.
  def to_cmyk
    to_rgb.to_cmyk
  end

    # Returns the luminosity (#l) of the colour.
  def brightness
    @l
  end
  def to_greyscale
    Color::GrayScale.from_fraction(@l)
  end
  alias to_grayscale to_greyscale

  attr_accessor :h, :s, :l
  remove_method :h=, :s=, :l= ;
  def h=(hh) #:nodoc:
    hh = 1.0 if hh > 1
    hh = 0.0 if hh < 0
    @h = hh
  end
  def s=(ss) #:nodoc:
    ss = 1.0 if ss > 1
    ss = 0.0 if ss < 0
    @s = ss
  end
  def l=(ll) #:nodoc:
    ll = 1.0 if ll > 1
    ll = 0.0 if ll < 0
    @l = ll
  end
end

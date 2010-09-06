#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: grayscale.rb,v 1.3 2005/08/08 02:44:17 austin Exp $
#++

  # A colour object representing shades of grey. Used primarily in PDF
  # document creation.
class Color::GrayScale
    # The format of a DeviceGrey colour for PDF. In color-tools 2.0 this
    # will be removed from this package and added back as a modification by
    # the PDF::Writer package.
  PDF_FORMAT_STR  = "%.3f %s"

    # Creates a greyscale colour object from fractional values 0..1.
    #
    #   Color::GreyScale.from_fraction(0.5)
  def self.from_fraction(g = 0)
    color = Color::GrayScale.new
    color.g = g
    color
  end

    # Creates a greyscale colour object from percentages 0..100.
    #
    #   Color::GrayScale.new(50)
  def initialize(g = 0)
    @g = g / 100.0
  end

    # Compares the other colour to this one. The other colour will be
    # converted to GreyScale before comparison, so the comparison between a
    # GreyScale colour and a non-GreyScale colour will be approximate and
    # based on the other colour's #to_greyscale conversion. If there is no
    # #to_greyscale conversion, this will raise an exception. This will
    # report that two GreyScale values are equivalent if they are within
    # 1e-4 (0.0001) of each other.
  def ==(other)
    other = other.to_grayscale
    other.kind_of?(Color::GrayScale) and
    ((@g - other.g).abs <= 1e-4)
  end

    # Present the colour as a DeviceGrey fill colour string for PDF. This
    # will be removed from the default package in color-tools 2.0.
  def pdf_fill
    PDF_FORMAT_STR % [ @g, "g" ]
  end

    # Present the colour as a DeviceGrey stroke colour string for PDF. This
    # will be removed from the default package in color-tools 2.0.
  def pdf_stroke
    PDF_FORMAT_STR % [ @g, "G" ]
  end

  def to_255
    [(@g * 255).round, 255].min
  end
  private :to_255

    # Present the colour as an HTML/CSS colour string.
  def html
    gs = "%02x" % to_255
    "##{gs * 3}"
  end

    # Convert the greyscale colour to CMYK.
  def to_cmyk
    k = 1.0 - @g.to_f
    Color::CMYK.from_fraction(0, 0, 0, k)
  end

    # Convert the greyscale colour to RGB.
  def to_rgb(ignored = true)
    g = to_255
    Color::RGB.new(g, g, g)
  end

  def to_grayscale
    self
  end
  alias to_greyscale to_grayscale

    # Lightens the greyscale colour by the stated percent.
  def lighten_by(percent)
    g = [@g + (@g * (percent / 100.0)), 1.0].min
    Color::GrayScale.from_fraction(g)
  end

    # Darken the RGB hue by the stated percent.
  def darken_by(percent)
    g = [@g - (@g * (percent / 100.0)), 0.0].max
    Color::GrayScale.from_fraction(g)
  end

    # Returns the YIQ (NTSC) colour encoding of the greyscale value. This
    # is an approximation, as the values for I and Q are calculated by
    # treating the greyscale value as an RGB value. The Y (intensity or
    # brightness) value is the same as the greyscale value.
  def to_yiq
    y = @g
    i = (@g * 0.596) + (@g * -0.275) + (@g * -0.321)
    q = (@g * 0.212) + (@g * -0.523) + (@g *  0.311)
    Color::YIQ.from_fraction(y, i, q)
  end

    # Returns the HSL colour encoding of the greyscale value.
  def to_hsl
    Color::HSL.from_fraction(0, 0, @g)
  end

    # Returns the brightness value for this greyscale value; this is the
    # greyscale value.
  def brightness
    @g
  end

  attr_accessor :g
  remove_method :g= ;
  def g=(gg) #:nodoc:
    gg = 1.0 if gg > 1
    gg = 0.0 if gg < 0
    @g = gg
  end
end

module Color
    # A synonym for Color::GrayScale.
  GreyScale = GrayScale
end

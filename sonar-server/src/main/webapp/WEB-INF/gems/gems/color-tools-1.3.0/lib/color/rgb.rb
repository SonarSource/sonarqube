#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: rgb.rb,v 1.6 2005/08/08 02:44:17 austin Exp $
#++

  # An RGB colour object.
class Color::RGB
    # The format of a DeviceRGB colour for PDF. In color-tools 2.0 this will
    # be removed from this package and added back as a modification by the
    # PDF::Writer package.
  PDF_FORMAT_STR  = "%.3f %.3f %.3f %s"

  class << self
      # Creates an RGB colour object from percentages 0..100.
      #
      #   Color::RGB.from_percentage(10, 20 30)
    def from_percentage(r = 0, g = 0, b = 0)
      from_fraction(r / 100.0, g / 100.0, b / 100.0)
    end

      # Creates an RGB colour object from fractional values 0..1.
      #
      #   Color::RGB.from_fraction(.3, .2, .1)
    def from_fraction(r = 0.0, g = 0.0, b = 0.0)
      colour = Color::RGB.new
      colour.r = r
      colour.g = g
      colour.b = b
      colour
    end

      # Creates an RGB colour object from an HTML colour descriptor (e.g.,
      # <tt>"fed"</tt> or <tt>"#cabbed;"</tt>.
      #
      #   Color::RGB.from_html("fed")
      #   Color::RGB.from_html("#fed")
      #   Color::RGB.from_html("#cabbed")
      #   Color::RGB.from_html("cabbed")
    def from_html(html_colour)
      html_colour = html_colour.gsub(%r{[#;]}, '')
      case html_colour.size 
      when 3
        colours = html_colour.scan(%r{[0-9A-Fa-f]}).map { |el| (el * 2).to_i(16) }
      when 6
        colours = html_colour.scan(%r<[0-9A-Fa-f]{2}>).map { |el| el.to_i(16) }
      else
        raise ArgumentError
      end

      Color::RGB.new(*colours)
    end
  end

    # Compares the other colour to this one. The other colour will be
    # converted to RGB before comparison, so the comparison between a RGB
    # colour and a non-RGB colour will be approximate and based on the other
    # colour's default #to_rgb conversion. If there is no #to_rgb
    # conversion, this will raise an exception. This will report that two
    # RGB colours are equivalent if all component values are within 1e-4
    # (0.0001) of each other.
  def ==(other)
    other = other.to_rgb
    other.kind_of?(Color::RGB) and
    ((@r - other.r).abs <= 1e-4) and
    ((@g - other.g).abs <= 1e-4) and
    ((@b - other.b).abs <= 1e-4)
  end

    # Creates an RGB colour object from the standard range 0..255.
    #
    #   Color::RGB.new(32, 64, 128)
    #   Color::RGB.new(0x20, 0x40, 0x80)
  def initialize(r = 0, g = 0, b = 0)
    @r = r / 255.0
    @g = g / 255.0
    @b = b / 255.0
  end

    # Present the colour as a DeviceRGB fill colour string for PDF. This
    # will be removed from the default package in color-tools 2.0.
  def pdf_fill
    PDF_FORMAT_STR % [ @r, @g, @b, "rg" ]
  end

    # Present the colour as a DeviceRGB stroke colour string for PDF. This
    # will be removed from the default package in color-tools 2.0.
  def pdf_stroke
    PDF_FORMAT_STR % [ @r, @g, @b, "RG" ]
  end

    # Present the colour as an HTML/CSS colour string.
  def html
    r = (@r * 255).round
    r = 255 if r > 255

    g = (@g * 255).round
    g = 255 if g > 255

    b = (@b * 255).round
    b = 255 if b > 255

    "#%02x%02x%02x" % [ r, g, b ]
  end

    # Converts the RGB colour to CMYK. Most colour experts strongly suggest
    # that this is not a good idea (some even suggesting that it's a very
    # bad idea). CMYK represents additive percentages of inks on white
    # paper, whereas RGB represents mixed colour intensities on a black
    # screen.
    #
    # However, the colour conversion can be done. The basic method is
    # multi-step:
    #
    # 1. Convert the R, G, and B components to C, M, and Y components.
    #     c = 1.0 – r
    #     m = 1.0 – g
    #     y = 1.0 – b
    # 2. Compute the minimum amount of black (K) required to smooth the
    #    colour in inks.
    #     k = min(c, m, y)
    # 3. Perform undercolour removal on the C, M, and Y components of the
    #    colours because less of each colour is needed for each bit of
    #    black. Also, regenerate the black (K) based on the undercolour
    #    removal so that the colour is more accurately represented in ink.
    #     c = min(1.0, max(0.0, c – UCR(k)))
    #     m = min(1.0, max(0.0, m – UCR(k)))
    #     y = min(1.0, max(0.0, y – UCR(k)))
    #     k = min(1.0, max(0.0, BG(k)))
    #
    # The undercolour removal function and the black generation functions
    # return a value based on the brightness of the RGB colour.
  def to_cmyk
    c = 1.0 - @r.to_f
    m = 1.0 - @g.to_f
    y = 1.0 - @b.to_f

    k = [c, m, y].min
    k = k - (k * brightness)

    c = [1.0, [0.0, c - k].max].min
    m = [1.0, [0.0, m - k].max].min
    y = [1.0, [0.0, y - k].max].min
    k = [1.0, [0.0, k].max].min

    Color::CMYK.from_fraction(c, m, y, k)
  end

  def to_rgb(ignored = nil)
    self
  end

    # Returns the YIQ (NTSC) colour encoding of the RGB value.
  def to_yiq
    y = (@r * 0.299) + (@g *  0.587) + (@b *  0.114)
    i = (@r * 0.596) + (@g * -0.275) + (@b * -0.321)
    q = (@r * 0.212) + (@g * -0.523) + (@b *  0.311)
    Color::YIQ.from_fraction(y, i, q)
  end

    # Returns the HSL colour encoding of the RGB value.
  def to_hsl
    min   = [ @r, @g, @b ].min
    max   = [ @r, @g, @b ].max
    delta = (max - min).to_f

    lum   = (max + min) / 2.0

    if delta <= 1e-5  # close to 0.0, so it's a grey
      hue = 0
      sat = 0
    else
      if (lum - 0.5) <= 1e-5
        sat = delta / (max + min).to_f
      else
        sat = delta / (2 - max - min).to_f
      end

      if @r == max
        hue = (@g - @b) / delta.to_f
      elsif @g == max
        hue = (2.0 + @b - @r) / delta.to_f
      elsif (@b - max) <= 1e-5
        hue = (4.0 + @r - @g) / delta.to_f
      end
      hue /= 6.0

      hue += 1 if hue < 0
      hue -= 1 if hue > 1
    end
    Color::HSL.from_fraction(hue, sat, lum)
  end

    # Mix the RGB hue with White so that the RGB hue is the specified
    # percentage of the resulting colour. Strictly speaking, this isn't a
    # darken_by operation.
  def lighten_by(percent)
    mix_with(White, percent)
  end

    # Mix the RGB hue with Black so that the RGB hue is the specified
    # percentage of the resulting colour. Strictly speaking, this isn't a
    # darken_by operation.
  def darken_by(percent)
    mix_with(Black, percent)
  end

    # Mix the mask colour (which must be an RGB object) with the current
    # colour at the stated opacity percentage (0..100).
  def mix_with(mask, opacity)
    opacity /= 100.0
    rgb = self.dup
    
    rgb.r = (@r * opacity) + (mask.r * (1 - opacity))
    rgb.g = (@g * opacity) + (mask.g * (1 - opacity))
    rgb.b = (@b * opacity) + (mask.b * (1 - opacity))

    rgb
  end

    # Returns the brightness value for a colour, a number between 0..1.
    # Based on the Y value of YIQ encoding, representing luminosity, or
    # perceived brightness.
    #
    # This may be modified in a future version of color-tools to use the
    # luminosity value of HSL.
  def brightness
    to_yiq.y
  end
  def to_grayscale
    Color::GrayScale.from_fraction(to_hsl.l)
  end

  alias to_greyscale to_grayscale

    # Returns a new colour with the brightness adjusted by the specified
    # percentage. Negative percentages will darken the colour; positive
    # percentages will brighten the colour.
    #
    #   Color::RGB::DarkBlue.adjust_brightness(10)
    #   Color::RGB::DarkBlue.adjust_brightness(-10)
  def adjust_brightness(percent)
    percent /= 100.0
    percent += 1.0
    percent  = [ percent, 2.0 ].min
    percent  = [ 0.0, percent ].max

    hsl      = to_hsl
    hsl.l   *= percent
    hsl.to_rgb
  end

    # Returns a new colour with the saturation adjusted by the specified
    # percentage. Negative percentages will reduce the saturation; positive
    # percentages will increase the saturation.
    #
    #   Color::RGB::DarkBlue.adjust_saturation(10)
    #   Color::RGB::DarkBlue.adjust_saturation(-10)
  def adjust_saturation(percent)
    percent  /= 100.0
    percent  += 1.0
    percent  = [ percent, 2.0 ].min
    percent  = [ 0.0, percent ].max

    hsl      = to_hsl
    hsl.s   *= percent
    hsl.to_rgb
  end

    # Returns a new colour with the hue adjusted by the specified
    # percentage. Negative percentages will reduce the hue; positive
    # percentages will increase the hue.
    #
    #   Color::RGB::DarkBlue.adjust_hue(10)
    #   Color::RGB::DarkBlue.adjust_hue(-10)
  def adjust_hue(percent)
    percent  /= 100.0
    percent  += 1.0
    percent  = [ percent, 2.0 ].min
    percent  = [ 0.0, percent ].max

    hsl      = to_hsl
    hsl.h   *= percent
    hsl.to_rgb
  end

  attr_accessor :r, :g, :b
  remove_method :r=, :g=, :b= ;
  def r=(rr) #:nodoc:
    rr = 1.0 if rr > 1
    rr = 0.0 if rr < 0
    @r = rr
  end
  def g=(gg) #:nodoc:
    gg = 1.0 if gg > 1
    gg = 0.0 if gg < 0
    @g = gg
  end
  def b=(bb) #:nodoc:
    bb = 1.0 if bb > 1
    bb = 0.0 if bb < 0
    @b = bb
  end
end

require 'color/rgb-colors'

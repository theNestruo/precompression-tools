
# Precompression tools

Optimizes MSX CHRTBL/CLRTBL data to be more RLE-friendly.

Extends the original optimization algorithm added to [PCXTOOLS v4.0](https://thenestruo.github.io/#link-2025-12-09) to use the entire charset data (instead of particular characters) and prefer larger chunks of RLE-friendly in either CHRTBL or CLRTBL data.

## Explanation

<style>
	.data {
		font-family:monospace;
		text-align:center;
	}
	.pixelart {
		width:48rem;
		image-rendering:pixelated;
	}
	.color0 {
		background: repeating-linear-gradient(-45deg, #000, #000 8px, #444 8px, #444 16px);
		color:#fff;
	}
	.color1 {
		background-color:#000000;
		color:#fff;
	}
	.color3 {
		background-color:#5EDC78;
		color:#000;
	}
	.colorC {
		background-color:#21B03B;
		color:#000;
	}
	.colorF {
		background-color:#FFFFFF;
		color:#000;
	}
	.unused {
		background: repeating-linear-gradient(-45deg, #400, #400 4px, #c00 4px, #c00 8px);
		color:#fff;
	}
	.ignore {
		background: repeating-linear-gradient(-45deg, #020, #020 4px, #060 4px, #060 8px);
		color:#fff;
	}
</style>

Using one of the 16x16 tiles of the original freeware version of [Ninja Senki](http://ninjasenki.com/) (&copy; 2010 Jonathan Lavigne) as an example, the typical output of _PNG2MSX_ from [PCXTOOLS](https://github.com/theNestruo/pcxtools) (or any other similar conversion tool) would be as follows:

<table>
	<thead>
		<tr><th>Image</th><th>CHRTBL</th><th colspan="2">CLRTBL</th><th>CHRTBL</th><th colspan="2">CLRTBL</th></tr>
	</thead>
	<tbody class="data">
		<tr><td rowspan=16><img src="./images/example.png" class="pixelart"></td>
			<td>0x00</td><td class="color0">0</td><td class="color1">1</td><td>0x00</td><td class="color0">0</td><td class="color1">1</td></tr>
		<tr><td>0x00</td><td class="color0">0</td><td class="colorC">c</td><td>0x00</td><td class="color0">0</td><td class="colorC">c</td></tr>
		<tr><td>0x00</td><td class="color0">0</td><td class="colorF">f</td><td>0x00</td><td class="color0">0</td><td class="colorF">f</td></tr>
		<tr><td>0x00</td><td class="color0">0</td><td class="color3">3</td><td>0x00</td><td class="color0">0</td><td class="color3">3</td></tr>
		<tr><td>0x00</td><td class="color0">0</td><td class="color3">3</td><td>0x00</td><td class="color0">0</td><td class="color3">3</td></tr>
		<tr><td>0x00</td><td class="color0">0</td><td class="color3">3</td><td>0x00</td><td class="color0">0</td><td class="color3">3</td></tr>
		<tr><td>0x00</td><td class="color0">0</td><td class="color3">3</td><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x7e</td><td class="color3">3</td><td class="colorC">c</td><td>0x18</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td><td>0x3c</td><td class="colorC">c</td><td class="color3">3</td></tr>
		<tr><td>0x66</td><td class="colorC">c</td><td class="color3">3</td><td>0x18</td><td class="colorC">c</td><td class="color3">3</td></tr>
		<tr><td>0x3c</td><td class="colorC">c</td><td class="color3">3</td><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x66</td><td class="color3">3</td><td class="colorC">c</td><td>0x18</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td><td>0x00</td><td class="color0">0</td><td class="colorC">c</td></tr>
		<tr><td>0x18</td><td class="color3">3</td><td class="colorC">c</td><td>0x00</td><td class="color0">0</td><td class="colorC">c</td></tr>
		<tr><td>0x00</td><td class="color0">0</td><td class="colorC">c</td><td>0x00</td><td class="color0">0</td><td class="colorC">c</td></tr>
		<tr><td>0x00</td><td class="color0">0</td><td class="color1">1</td><td>0x00</td><td class="color0">0</td><td class="color1">1</td></tr>
	</tbody>
</table>

Actual output may vary depending on the particular conversion tool and/or the parameters used.

Variations are possible because each line can be, at least, inverted (i.e.: negating the pattern and swapping the foreground and background colors would render as the same image). But also because there may be unused values: lines with only one visible color can be either ignoring their foreground or background color, or their pattern (if both colors are set to the same value).

In the example output, the color 0 is not used to render the image:

<table>
	<thead>
		<tr><th>Image</th><th>CHRTBL</th><th colspan="2">CLRTBL</th><th>CHRTBL</th><th colspan="2">CLRTBL</th></tr>
	</thead>
	<tbody class="data">
		<tr><td rowspan=16><img src="./images/example.png" class="pixelart"></td>
			<td>0x00</td><td class="unused">0</td><td class="color1">1</td><td>0x00</td><td class="unused">0</td><td class="color1">1</td></tr>
		<tr><td>0x00</td><td class="unused">0</td><td class="colorC">c</td><td>0x00</td><td class="unused">0</td><td class="colorC">c</td></tr>
		<tr><td>0x00</td><td class="unused">0</td><td class="colorF">f</td><td>0x00</td><td class="unused">0</td><td class="colorF">f</td></tr>
		<tr><td>0x00</td><td class="unused">0</td><td class="color3">3</td><td>0x00</td><td class="unused">0</td><td class="color3">3</td></tr>
		<tr><td>0x00</td><td class="unused">0</td><td class="color3">3</td><td>0x00</td><td class="unused">0</td><td class="color3">3</td></tr>
		<tr><td>0x00</td><td class="unused">0</td><td class="color3">3</td><td>0x00</td><td class="unused">0</td><td class="color3">3</td></tr>
		<tr><td>0x00</td><td class="unused">0</td><td class="color3">3</td><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x7e</td><td class="color3">3</td><td class="colorC">c</td><td>0x18</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td><td>0x3c</td><td class="colorC">c</td><td class="color3">3</td></tr>
		<tr><td>0x66</td><td class="colorC">c</td><td class="color3">3</td><td>0x18</td><td class="colorC">c</td><td class="color3">3</td></tr>
		<tr><td>0x3c</td><td class="colorC">c</td><td class="color3">3</td><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x66</td><td class="color3">3</td><td class="colorC">c</td><td>0x18</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td><td>0x00</td><td class="unused">0</td><td class="colorC">c</td></tr>
		<tr><td>0x18</td><td class="color3">3</td><td class="colorC">c</td><td>0x00</td><td class="unused">0</td><td class="colorC">c</td></tr>
		<tr><td>0x00</td><td class="unused">0</td><td class="colorC">c</td><td>0x00</td><td class="unused">0</td><td class="colorC">c</td></tr>
		<tr><td>0x00</td><td class="unused">0</td><td class="color1">1</td><td>0x00</td><td class="unused">0</td><td class="color1">1</td></tr>
	</tbody>
</table>

Based on the existence of this variations, it is possible to invert some lines and change the value of the unused colors. By carefully chosing the variations, it is possible to reduce the count of different values in CLRTBL, and to create longer chunks of repeated values. Those properties are likely to improve the compression ration in RLE-based and dicrionary coders compression algorithms (such as [ZX0](https://github.com/einar-saukas/ZX0) by Einar Saukas).

In the example output, CLRTBL data can be reduced from 6 to 2 values (`0x1f` and `0x3c`) and create chunks of 12 repetitions of the same value without altering the rendered image:

<table>
	<thead>
		<tr><th>Image</th><th>CHRTBL</th><th colspan="2">CLRTBL</th><th>CHRTBL</th><th colspan="2">CLRTBL</th></tr>
	</thead>
	<tbody class="data">
		<tr><td rowspan=16><img src="./images/example.png" class="pixelart"></td>
			<td>0xff</td><td class="color1">1</td><td class="ignore">f</td><td>0xff</td><td class="color1">1</td><td class="ignore">f</td></tr>
		<tr><td>0x00</td><td class="ignore">3</td><td class="colorC">c</td><td>0x00</td><td class="ignore">3</td><td class="colorC">c</td></tr>
		<tr><td>0x00</td><td class="ignore">1</td><td class="colorF">f</td><td>0x00</td><td class="ignore">1</td><td class="colorF">f</td></tr>
		<tr><td>0xff</td><td class="color3">3</td><td class="ignore">c</td><td>0xff</td><td class="color3">3</td><td class="ignore">c</td></tr>
		<tr><td>0xff</td><td class="color3">3</td><td class="ignore">c</td><td>0xff</td><td class="color3">3</td><td class="ignore">c</td></tr>
		<tr><td>0xff</td><td class="color3">3</td><td class="ignore">c</td><td>0xff</td><td class="color3">3</td><td class="ignore">c</td></tr>
		<tr><td>0xff</td><td class="color3">3</td><td class="ignore">c</td><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x7e</td><td class="color3">3</td><td class="colorC">c</td><td>0x18</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td><td>0xc3</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x99</td><td class="color3">3</td><td class="colorC">c</td><td>0x7e</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0xc3</td><td class="color3">3</td><td class="colorC">c</td><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x66</td><td class="color3">3</td><td class="colorC">c</td><td>0x18</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td>0x3c</td><td class="color3">3</td><td class="colorC">c</td><td>0x00</td><td class="ignore">3</td><td class="colorC">c</td></tr>
		<tr><td>0x18</td><td class="color3">3</td><td class="colorC">c</td><td>0x00</td><td class="ignore">3</td><td class="colorC">c</td></tr>
		<tr><td>0x00</td><td class="ignore">3</td><td class="colorC">c</td><td>0x00</td><td class="ignore">3</td><td class="colorC">c</td></tr>
		<tr><td>0xff</td><td class="color1">1</td><td class="ignore">f</td><td>0xff</td><td class="color1">1</td><td class="ignore">f</td></tr>
	</tbody>
</table>

Alternatively, variations can be chosen to reduce the count of different values in CHRTBL (instead of CLRTBL).

In the example output, CHRTBL data can be reduced to 2 values (`0x7e`, `0x3c`, `0x66` and `0x18`) and create chunks of 8 and 9 repetitions of the same value without altering the rendered image:

<table>
	<thead>
		<tr><th>Image</th><th>CHRTBL</th><th colspan="2">CLRTBL</th><th>CHRTBL</th><th colspan="2">CLRTBL</th></tr>
	</thead>
	<tbody class="data">
		<tr><td rowspan=16><img src="./images/example.png" class="pixelart"></td>
			<td class="ignore">0x7e</td><td class="color1">1</td><td class="color1">1</td><td class="ignore">0x18</td><td class="color1">1</td><td class="color1">1</td></tr>
		<tr><td class="ignore">0x7e</td><td class="colorC">c</td><td class="colorC">c</td><td class="ignore">0x18</td><td class="colorC">c</td><td class="colorC">c</td></tr>
		<tr><td class="ignore">0x7e</td><td class="colorF">f</td><td class="colorF">f</td><td class="ignore">0x18</td><td class="colorF">f</td><td class="colorF">f</td></tr>
		<tr><td class="ignore">0x7e</td><td class="color3">3</td><td class="color3">3</td><td class="ignore">0x18</td><td class="color3">3</td><td class="color3">3</td></tr>
		<tr><td class="ignore">0x7e</td><td class="color3">3</td><td class="color3">3</td><td class="ignore">0x18</td><td class="color3">3</td><td class="color3">3</td></tr>
		<tr><td class="ignore">0x7e</td><td class="color3">3</td><td class="color3">3</td><td class="ignore">0x18</td><td class="color3">3</td><td class="color3">3</td></tr>
		<tr><td class="ignore">0x7e</td><td class="color3">3</td><td class="color3">3</td><td               >0x3c</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td               >0x7e</td><td class="color3">3</td><td class="colorC">c</td><td               >0x18</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td               >0x3c</td><td class="color3">3</td><td class="colorC">c</td><td               >0x3c</td><td class="colorC">c</td><td class="color3">3</td></tr>
		<tr><td               >0x66</td><td class="colorC">c</td><td class="color3">3</td><td               >0x18</td><td class="colorC">c</td><td class="color3">3</td></tr>
		<tr><td               >0x3c</td><td class="colorC">c</td><td class="color3">3</td><td               >0x3c</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td               >0x66</td><td class="color3">3</td><td class="colorC">c</td><td               >0x18</td><td class="color3">3</td><td class="colorC">c</td></tr>
		<tr><td               >0x3c</td><td class="color3">3</td><td class="colorC">c</td><td class="ignore">0x18</td><td class="colorC">c</td><td class="colorC">c</td></tr>
		<tr><td               >0x18</td><td class="color3">3</td><td class="colorC">c</td><td class="ignore">0x18</td><td class="colorC">c</td><td class="colorC">c</td></tr>
		<tr><td class="ignore">0x18</td><td class="colorC">c</td><td class="colorC">c</td><td class="ignore">0x18</td><td class="colorC">c</td><td class="colorC">c</td></tr>
		<tr><td class="ignore">0x18</td><td class="color1">1</td><td class="color1">1</td><td class="ignore">0x18</td><td class="color1">1</td><td class="color1">1</td></tr>
	<tbody>
</table>

## Measurements

### On some publicly available charsets

Measured on four charset: _ninjasenki.png_, _pyramidwarpex.png_, _stevedore.png_, and _youkaiyashiki.png_:

| **Size (in bytes)** | **&Delta;** | **Compression ratio** | **&Delta;** | **Algorithms** |
| ------------------: | ----------: | --------------------: | ----------: | --- |
|         10&nbsp;080 |             |               100,00% |             | Total raw size |
|      **5&nbsp;656** |             |            **56,11%** |             | **_png2msx_ unoptimized ZX0 size** |
|      **5&nbsp;403** |    **-253** |            **53,60%** |  **-2,51%** | **_png2msx -o_ optimized ZX0 size** |
|                     |             |                       |             | |
|          5&nbsp;234 |        -422 |                51,92% |      -4,19% | PatternAndColor + ColorAndPattern _(PrioritizeColor)_ |
|      **5&nbsp;235** |    **-421** |            **51,93%** |  **-4,18%**<sup>1</sup> | **&mdash; + ColorAndPattern**<sup>2</sup> |
|          5&nbsp;235 |        -421 |                51,93% |      -4,18% | PatternOnly + ColorAndPattern _(PrioritizeColor)_ |
|          5&nbsp;268 |        -388 |                52,26% |      -3,85% | PatternOnly + ColorAndPattern _(Default)_ |
|          5&nbsp;339 |        -317 |                52,97% |      -3,14% | PatternAndColor + ColorAndPattern _(Default)_ |
|          5&nbsp;396 |        -260 |                53,53% |      -2,58% | &mdash; + ColorOnly |
|          5&nbsp;396 |        -260 |                53,53% |      -2,58% | PatternOnly + ColorOnly _(PrioritizeColor)_ |
|          5&nbsp;401 |        -255 |                53,58% |      -2,53% | PatternAndColor + ColorOnly _(PrioritizeColor)_ |
|          5&nbsp;421 |        -235 |                53,78% |      -2,33% | PatternOnly + ColorOnly _(Default)_ |
|          5&nbsp;487 |        -169 |                54,43% |      -1,68% | PatternAndColor + ColorOnly _(Default)_ |
|          5&nbsp;511 |        -145 |                54,67% |      -1,44% | PatternAndColor + ColorAndPattern _(PrioritizePattern)_ |
|          5&nbsp;529 |        -127 |                54,85% |      -1,26% | PatternAndColor + &mdash; |
|          5&nbsp;529 |        -127 |                54,85% |      -1,26% | PatternAndColor + ColorOnly _(PrioritizePattern)_ |
|          5&nbsp;599 |         -57 |                55,55% |      -0,57% | PatternOnly + ColorAndPattern _(PrioritizePattern)_ |
|          5&nbsp;615 |         -41 |                55,70% |      -0,41% | PatternOnly + ColorOnly _(PrioritizePattern)_ |
|          5&nbsp;656 |           0 |                56,11% |       0,00% | &mdash; + &mdash; |
|          5&nbsp;667 |         +11 |                56,22% |      +0,11% | PatternOnly + &mdash; |

<sup>1</sup> The 4,19% delta is relative to the original compression ratio of 56,11%. The ratio of the compressed size reduction (412 bytes) relative to the original size (5&nbsp;656 bytes) is **-7,44%**

<sup>2</sup> Default algorithms

<br>


### Including non-public charsets

Adds seven dense charsets: _trucho-river.png_, _trucho-reservoir.png_, _trucho-pier.png_, _trucho-beach.png_, _roadster-forest.png_, _roadster-night.png_, and _roadster-desert.png_:

| **Size (in bytes)** | **&Delta;** | **Compression ratio** | **&Delta;** | **Algorithms** |
| ------------------: | ----------: | --------------------: | ----------: | --- |
|         31&nbsp;072 |             |               100,00% |             | Total raw size |
|     **15&nbsp;486** |             |            **49,84%** |             | **_png2msx_ unoptimized ZX0 size** |
|     **14&nbsp;545** |    **-941** |            **46,81%** |  **-3,03%** | **_png2msx -o_ optimized ZX0 size** |
|                     |             |                       |             | |
|     **13&nbsp;943** |   **-1543** |            **44,87%** |  **-4,97%**<sup>1</sup> | **&mdash; + ColorAndPattern**<sup>2</sup> |
|         13&nbsp;943 |       -1543 |                44,87% |      -4,97% | PatternOnly + ColorAndPattern _(PrioritizeColor)_ |
|         13&nbsp;948 |       -1538 |                44,89% |      -4,95% | PatternAndColor + ColorAndPattern _(PrioritizeColor)_ |
|         14&nbsp;073 |       -1413 |                45,29% |      -4,55% | PatternOnly + ColorAndPattern _(Default)_ |
|         14&nbsp;382 |       -1104 |                46,29% |      -3,55% | PatternAndColor + ColorAndPattern _(Default)_ |
|         14&nbsp;575 |        -911 |                46,91% |      -2,93% | &mdash; + ColorOnly |
|         14&nbsp;575 |        -911 |                46,91% |      -2,93% | PatternOnly + ColorOnly _(PrioritizeColor)_ |
|         14&nbsp;638 |        -848 |                47,11% |      -2,73% | PatternAndColor + ColorOnly _(PrioritizeColor)_ |
|         14&nbsp;658 |        -828 |                47,17% |      -2,66% | PatternOnly + ColorOnly _(Default)_ |
|         15&nbsp;027 |        -459 |                48,36% |      -1,48% | PatternAndColor + ColorOnly _(Default)_ |
|         15&nbsp;182 |        -304 |                48,86% |      -0,98% | PatternAndColor + ColorAndPattern _(PrioritizePattern)_ |
|         15&nbsp;206 |        -280 |                48,94% |      -0,90% | PatternAndColor + &mdash;  |
|         15&nbsp;206 |        -280 |                48,94% |      -0,90% | PatternAndColor + ColorOnly _(PrioritizePattern)_ |
|         15&nbsp;331 |        -155 |                49,34% |      -0,50% | PatternOnly + ColorOnly _(PrioritizePattern)_ |
|         15&nbsp;382 |        -104 |                49,50% |      -0,33% | PatternOnly + ColorAndPattern _(PrioritizePattern)_ |
|         15&nbsp;486 |          +0 |                49,84% |      +0,00% | &mdash; + &mdash; |
|         15&nbsp;497 |         +11 |                49,87% |      +0,04% | PatternOnly + &mdash;  |

<sup>1</sup> The 4,97% delta is relative to the original compression ratio of 49,84%. The ratio of the compressed size reduction (1543 bytes) relative to the original size (15&nbsp;486 bytes) is **-9,96%**

<sup>2</sup> Default algorithms

<br>

As a reference, the values for the seven dense non-public charsets only (default algorithms only):

| **Size (in bytes)** | **&Delta;** | **Compression ratio** | **&Delta;** | **Algorithms** |
| ------------------: | ----------: | --------------------: | ----------: | --- |
|         20&nbsp;992 |             |               100,00% |             | Total raw size |
|      **9&nbsp;830** |             |            **46,83%** |             | **_png2msx_ unoptimized ZX0 size** |
|      **9&nbsp;142** |    **-688** |            **43,55%** |  **-3,28%** | **_png2msx -o_ optimized ZX0 size** |
|                     |             |                       |             | |
|      **8&nbsp;708** |   **-1122** |            **41,48%** |  **-5,34%**<sup>1</sup> | **&mdash; + ColorAndPattern** |

<sup>1</sup> The 5,34% delta is relative to the original compression ratio of 46,83%. The ratio of the compressed size reduction (1122 bytes) relative to the original size (9&nbsp;830 bytes) is **-11,41%**

## Author and last words

Coded by [**theNestruo**](https://github.com/theNestruo) (Néstor Sancho).

* Test graphics extracted from the original freeware version of [Ninja Senki](http://ninjasenki.com/) &copy; 2010 Jonathan Lavigne.

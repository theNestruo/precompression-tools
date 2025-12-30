
# Precompression tools

Optimizes MSX CHRTBL/CLRTBL data to be more RLE-friendly.

Extends the original optimization algorithm added to [PCXTOOLS v4.0](https://thenestruo.github.io/#link-2025-12-09) to use the entire charset data (instead of particular characters) and prefer larger chunks of RLE-friendly in either CHRTBL or CLRTBL data.

## Measurements

### On publicly available charsets

Measured on four charset: _ninjasenki.png_, _pyramidwarpex.png_, _stevedore.png_, and _youkaiyashiki.png_:

|  **Optimized ZX0 size** | **&Delta;** | **Compression ratio** | **&Delta;** | **Algorithms** |
| ----------------------: | ----------: | --------------------: | ----------: | --- |
|             10&nbsp;080 |             |               100,00% |             | Total raw size |
|          **5&nbsp;656** |             |            **56,11%** |             | **Unoptimized ZX0 size** |
|              5&nbsp;234 |        -422 |                51,92% |      -4,19% | PatternAndColor + ColorAndPattern (PrioritizeColor) |
|          **5&nbsp;235** |    **-421** |            **51,93%** |  **-4,18%** | **Null + ColorAndPattern (Default)**<sup>1</sup> |
|              5&nbsp;235 |        -421 |                51,93% |      -4,18% | Null + ColorAndPattern (PrioritizePattern) |
|              5&nbsp;235 |        -421 |                51,93% |      -4,18% | Null + ColorAndPattern (PrioritizeColor) |
|              5&nbsp;235 |        -421 |                51,93% |      -4,18% | PatternOnly + ColorAndPattern (PrioritizeColor) |
|              5&nbsp;268 |        -388 |                52,26% |      -3,85% | PatternOnly + ColorAndPattern (Default) |
|              5&nbsp;339 |        -317 |                52,97% |      -3,14% | PatternAndColor + ColorAndPattern (Default) |
|              5&nbsp;396 |        -260 |                53,53% |      -2,58% | Null + ColorOnly (Default) |
|              5&nbsp;396 |        -260 |                53,53% |      -2,58% | Null + ColorOnly (PrioritizePattern) |
|              5&nbsp;396 |        -260 |                53,53% |      -2,58% | Null + ColorOnly (PrioritizeColor) |
|              5&nbsp;396 |        -260 |                53,53% |      -2,58% | PatternOnly + ColorOnly (PrioritizeColor) |
|              5&nbsp;401 |        -255 |                53,58% |      -2,53% | PatternAndColor + ColorOnly (PrioritizeColor) |
|              5&nbsp;421 |        -235 |                53,78% |      -2,33% | PatternOnly + ColorOnly (Default) |
|              5&nbsp;487 |        -169 |                54,43% |      -1,68% | PatternAndColor + ColorOnly (Default) |
|              5&nbsp;511 |        -145 |                54,67% |      -1,44% | PatternAndColor + ColorAndPattern (PrioritizePattern) |
|              5&nbsp;529 |        -127 |                54,85% |      -1,26% | PatternAndColor + Null (Default) |
|              5&nbsp;529 |        -127 |                54,85% |      -1,26% | PatternAndColor + Null (PrioritizePattern) |
|              5&nbsp;529 |        -127 |                54,85% |      -1,26% | PatternAndColor + Null (PrioritizeColor) |
|              5&nbsp;529 |        -127 |                54,85% |      -1,26% | PatternAndColor + ColorOnly (PrioritizePattern) |
|              5&nbsp;599 |         -57 |                55,55% |      -0,57% | PatternOnly + ColorAndPattern (PrioritizePattern) |
|              5&nbsp;615 |         -41 |                55,70% |      -0,41% | PatternOnly + ColorOnly (PrioritizePattern) |
|              5&nbsp;656 |           0 |                56,11% |       0,00% | Null + Null (Default) |
|              5&nbsp;656 |           0 |                56,11% |       0,00% | Null + Null (PrioritizePattern) |
|              5&nbsp;656 |           0 |                56,11% |       0,00% | Null + Null (PrioritizeColor) |
|              5&nbsp;667 |         +11 |                56,22% |      +0,11% | PatternOnly + Null (Default) |
|              5&nbsp;667 |         +11 |                56,22% |      +0,11% | PatternOnly + Null (PrioritizePattern) |
|              5&nbsp;667 |         +11 |                56,22% |      +0,11% | PatternOnly + Null (PrioritizeColor) |

<sup>1</sup> Default algorithms

### Including non-public charsets

Adds seven dense charsets: _trucho-river.png_, _trucho-reservoir.png_, _trucho-pier.png_, _trucho-beach.png_, _roadster-forest.png_, _roadster-night.png_, and _roadster-desert.png_:

|  **Optimized ZX0 size** | **&Delta;** | **Compression ratio** | **&Delta;** | **Algorithms** |
| ----------------------: | ----------: | --------------------: | ----------: | --- |
|             31&nbsp;072 |             |               100,00% |             | Total raw size |
|         **15&nbsp;486** |             |            **49,84%** |             | **Unoptimized ZX0 size** |
|         **13&nbsp;943** |   **-1543** |            **44,87%** |  **-4,97%** | **Null + ColorAndPattern (Default)**<sup>1</sup> |
|             13&nbsp;943 |       -1543 |                44,87% |      -4,97% | Null + ColorAndPattern (PrioritizePattern) |
|             13&nbsp;943 |       -1543 |                44,87% |      -4,97% | Null + ColorAndPattern (PrioritizeColor) |
|             13&nbsp;943 |       -1543 |                44,87% |      -4,97% | PatternOnly + ColorAndPattern (PrioritizeColor) |
|             13&nbsp;948 |       -1538 |                44,89% |      -4,95% | PatternAndColor + ColorAndPattern (PrioritizeColor) |
|             14&nbsp;073 |       -1413 |                45,29% |      -4,55% | PatternOnly + ColorAndPattern (Default) |
|             14&nbsp;382 |       -1104 |                46,29% |      -3,55% | PatternAndColor + ColorAndPattern (Default) |
|             14&nbsp;575 |        -911 |                46,91% |      -2,93% | Null + ColorOnly (Default) |
|             14&nbsp;575 |        -911 |                46,91% |      -2,93% | Null + ColorOnly (PrioritizePattern) |
|             14&nbsp;575 |        -911 |                46,91% |      -2,93% | Null + ColorOnly (PrioritizeColor) |
|             14&nbsp;575 |        -911 |                46,91% |      -2,93% | PatternOnly + ColorOnly (PrioritizeColor) |
|             14&nbsp;638 |        -848 |                47,11% |      -2,73% | PatternAndColor + ColorOnly (PrioritizeColor) |
|             14&nbsp;658 |        -828 |                47,17% |      -2,66% | PatternOnly + ColorOnly (Default) |
|             15&nbsp;027 |        -459 |                48,36% |      -1,48% | PatternAndColor + ColorOnly (Default) |
|             15&nbsp;182 |        -304 |                48,86% |      -0,98% | PatternAndColor + ColorAndPattern (PrioritizePattern) |
|             15&nbsp;206 |        -280 |                48,94% |      -0,90% | PatternAndColor + Null (Default) |
|             15&nbsp;206 |        -280 |                48,94% |      -0,90% | PatternAndColor + Null (PrioritizePattern) |
|             15&nbsp;206 |        -280 |                48,94% |      -0,90% | PatternAndColor + Null (PrioritizeColor) |
|             15&nbsp;206 |        -280 |                48,94% |      -0,90% | PatternAndColor + ColorOnly (PrioritizePattern) |
|             15&nbsp;331 |        -155 |                49,34% |      -0,50% | PatternOnly + ColorOnly (PrioritizePattern) |
|             15&nbsp;382 |        -104 |                49,50% |      -0,33% | PatternOnly + ColorAndPattern (PrioritizePattern) |
|             15&nbsp;486 |          +0 |                49,84% |      +0,00% | Null + Null (Default) |
|             15&nbsp;486 |          +0 |                49,84% |      +0,00% | Null + Null (PrioritizePattern) |
|             15&nbsp;486 |          +0 |                49,84% |      +0,00% | Null + Null (PrioritizeColor) |
|             15&nbsp;497 |         +11 |                49,87% |      +0,04% | PatternOnly + Null (Default) |
|             15&nbsp;497 |         +11 |                49,87% |      +0,04% | PatternOnly + Null (PrioritizePattern) |
|             15&nbsp;497 |         +11 |                49,87% |      +0,04% | PatternOnly + Null (PrioritizeColor) |

<sup>1</sup> Default algorithms

## Author and last words

Coded by [**theNestruo**](https://github.com/theNestruo) (Néstor Sancho).

* Test graphics extracted from the original freeware version of [Ninja Senki](http://ninjasenki.com/) &copy; 2010 Jonathan Lavigne.


# Precompression tools

Optimizes MSX CHRTBL/CLRTBL data to be more compressor friendly (both for RLE compressors and literal repetitions compressors).

~~Extends the original optimization algorithm added to [PCXTOOLS v4.0](https://thenestruo.github.io/#link-2025-12-09) to use the entire charset data (instead of particular characters) and prefer larger chunks of RLE-friendly in either CHRTBL or CLRTBL data.~~

Compression ratio improvement will vary depending on the particular charset and the actual compression tool, but expect a size reduction ranging between ~7% and ~11%.

## Description and examples

See [Precompression tools](https://thenestruo.github.io/#link-2026-01-04) post.

## Usage

```
usage:  java -jar precompression.jar <input1> [<input2>]
	[-help] [-verbose]
	[-exclude <arg>]

with:
	<input1>  Binary input file (CHRTBL)
	<input2>  Binary input file (CLRTBL)

		Options						Description
-help                   Shows usage
-verbose                Verbose execution

-exclude <arg>          Excluded range of addresses: <from>..<to>
```

## Author and last words

Coded by [**theNestruo**](https://github.com/theNestruo) (Néstor Sancho).

* Test graphics extracted from:
	- Original freeware version of [Ninja Senki](http://ninjasenki.com/) &copy; 2010 Jonathan Lavigne
	- [Pyramid Warp (1983) - Enhanced plus](https://github.com/theNestruo/msx-pyramidwarpex)
	- [Stevedore (MSX, 2020)](https://github.com/theNestruo/msx-stevedore)
	- [Youkai Yashiki (MSX, Casio, 1986) - Graphics patch](https://github.com/theNestruo/msx-youkaiyashiki-patch)


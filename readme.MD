
# Precompression tools

Optimizes MSX CHRTBL/CLRTBL data to be more compressor friendly (both for RLE compressors and literal repetitions compressors).

~~Extends the original optimization algorithm added to [PCXTOOLS v4.0](https://thenestruo.github.io/#link-2025-12-09) to use the entire charset data (instead of particular characters) and prefer larger chunks of RLE-friendly in either CHRTBL or CLRTBL data.~~

Compression ratio improvement will vary depending on the particular charset, the conversion (image to CHRTBL/CLRTBL data), and the actual compression tool, but expect a size reduction of up to ~14%.

> Note: Some tools, such as [aOrante](https://github.com/mvac7)'s [MSXTiles-devtool](https://github.com/mvac7/MSXTILESdevtool]), do a really good job and generate optimal, or almost optimal, CHRTBL/CLRTBL data. In those cases, this tool cannot further reduce the compressed size.

## Description and examples

See [Precompression tools](https://thenestruo.github.io/#link-2026-01-04) post.

## Usage

```
Usage: java -jar precompression.jar [-hinv] [-e=<exclusionRange>] chrtbl [clrtbl]
      chrtbl      binary input file(s): CHRTBL
      [clrtbl]    binary input file(s): CLRTBL
  -h, --help      shows usage
  -v, --verbose   verbose execution
  -e, --exclude=<exclusionRange>
                  Excluded range of addresses: <from>..<to>
  -i, --invert    invert the CHRTBL/CLRTBL bytes of the entire charset
```

## Author and last words

Coded by [**theNestruo**](https://github.com/theNestruo) (Néstor Sancho).

* Test graphics extracted from:
	- Original freeware version of [Ninja Senki](http://ninjasenki.com/) &copy; 2010 Jonathan Lavigne
	- [Pyramid Warp (1983) - Enhanced plus](https://github.com/theNestruo/msx-pyramidwarpex)
	- [Stevedore (MSX, 2020)](https://github.com/theNestruo/msx-stevedore)
	- [Youkai Yashiki (MSX, Casio, 1986) - Graphics patch](https://github.com/theNestruo/msx-youkaiyashiki-patch)


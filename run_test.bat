@echo off

@REM Original CHRTBL/CLRTBL
if not exist .\test\test.png.chr (
	png2msx.exe .\test\test.png
)

@REM Reference values
if not exist .\test\test.png.chr.zx0 (
	zx0.exe .\test\test.png.chr
)
if not exist .\test\test.png.clr.zx0 (
	zx0.exe .\test\test.png.clr
)

@REM Actual tool
if not exist .\target\precompression.jar (
	mvn --batch-mode clean package
)

@REM Precompressed CHRTBL/CLRTBL
if not exist .\test\test.png.chr.opt (
	java -jar .\target\precompression.jar .\test\test.png.chr .\test\test.png.clr
)

@REM Reference values
if not exist .\test\test.png.chr.opt.zx0 (
	zx0.exe .\test\test.png.chr.opt
)
if not exist .\test\test.png.clr.opt.zx0 (
	zx0.exe .\test\test.png.clr.opt
)

@REM Display sizes
dir .\test\test.png.c?r.zx0 /one
dir .\test\test.png.c?r.opt.zx0 /one

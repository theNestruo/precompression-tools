package com.github.thenestruo.msx.precompression.support;

public class MsxCharset {

	private final byte[] chrtbl;
	private final byte[] clrtbl;

	public MsxCharset(final byte[] chrtbl, final byte[] clrtbl) {
		this.chrtbl = chrtbl;
		this.clrtbl = clrtbl;
	}

	public int size() {
		return this.chrtbl.length;
	}

	public MsxLine get(final int address) {
		return new MsxLine(this.chrtbl[address], this.clrtbl[address]);
	}

	public void set(final int address, final MsxLine line) {
		this.chrtbl[address] = line.getPattern();
		this.clrtbl[address] = line.getColor();
	}

	public byte[] getChrtbl() {
		return this.chrtbl;
	}

	public byte[] getClrtbl() {
		return this.clrtbl;
	}
}

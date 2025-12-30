package com.github.thenestruo.msx.precompression.model;

import org.apache.commons.lang3.ArrayUtils;

public class MsxCharset {

	private final byte[] chrtbl;
	private final byte[] clrtbl;

	public MsxCharset(final MsxCharset that) {
		this(that.chrtbl, that.clrtbl);
	}

	public MsxCharset(final byte[] chrtbl, final byte[] clrtbl) {
		this.chrtbl = ArrayUtils.clone(chrtbl);
		this.clrtbl = ArrayUtils.clone(clrtbl);
	}

	public byte[] chrtbl() {
		return this.chrtbl;
	}

	public byte[] clrtbl() {
		return this.clrtbl;
	}

	public int size() {
		return this.chrtbl.length;
	}

	public MsxLine get(final int address) {
		return new MsxLine(this.chrtbl[address], this.clrtbl[address]);
	}

	public void set(final int address, final MsxLine line) {
		this.chrtbl[address] = line.pattern();
		this.clrtbl[address] = line.color();
	}
}

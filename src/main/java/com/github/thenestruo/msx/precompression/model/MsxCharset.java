package com.github.thenestruo.msx.precompression.model;

import java.util.Objects;

public class MsxCharset {

	private final byte[] chrtbl;
	private final byte[] clrtbl;

	public MsxCharset(final MsxCharset that) {
		this(that.chrtbl, that.clrtbl);
	}

	public MsxCharset(final byte[] chrtbl, final byte[] clrtbl) {
		this.chrtbl = Objects.requireNonNull(chrtbl).clone();
		this.clrtbl = Objects.requireNonNull(clrtbl).clone();
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

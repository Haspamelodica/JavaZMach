package net.haspamelodica.javaz.core.text;

public class UnicodeZSCIIConverterStream implements UnicodeCharStream
{
	private final ZSCIICharStreamReceiver	zsciiTarget;

	private ZSCIICharStream				source;
	private UnicodeCharStreamReceiver	target;

	public UnicodeZSCIIConverterStream(UnicodeZSCIIConverter unicodeConv)
	{
		this.zsciiTarget = z -> unicodeConv.zsciiToUnicode(z, target);
	}

	public void reset(ZSCIICharStream source)
	{
		this.source = source;
	}

	@Override
	public void decode(UnicodeCharStreamReceiver target)
	{
		this.target = target;
		source.decode(zsciiTarget);
	}
}
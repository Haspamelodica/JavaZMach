package net.haspamelodica.javazmach.assembler.core;

import java.math.BigInteger;

public interface LocationAndLabelResolver extends LocationResolver, LabelResolver
{
	public default BigInteger resolveAbsoluteOrNull(String label)
	{
		return resolveAbsoluteOrNull(resolveToLocation(label));
	}

	public static LocationAndLabelResolver of(LocationResolver locationResolver, LabelResolver labelResolver)
	{
		return new LocationAndLabelResolver()
		{
			@Override
			public Location resolveToLocation(String label)
			{
				return labelResolver.resolveToLocation(label);
			}

			@Override
			public BigInteger resolveAbsoluteOrNull(Location location)
			{
				return locationResolver.resolveAbsoluteOrNull(location);
			}
		};
	}
}

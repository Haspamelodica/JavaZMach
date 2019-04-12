package net.haspamelodica.javaz;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class GlobalConfig
{
	private static final Set<String>	boolTrue	= Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"yes", "y", "true", "t", "1")));
	private static final Set<String>	boolFalse	= Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"no", "n", "false", "f", "0")));

	private static final String		stringDefault	= "";
	private static final boolean	boolDefault		= true;
	private static final int		intDefault		= 0;

	private final Map<String, String> values;

	public GlobalConfig()
	{
		this(null);
	}
	public GlobalConfig(Map<String, String> values)
	{
		this.values = new HashMap<>();
		if(values != null)
			this.values.putAll(values);
	}

	public String getString(String name)
	{
		return getGeneric(name, "string", Function.identity(), stringDefault, Function.identity());
	}
	public boolean getBool(String name)
	{
		return getGeneric(name, "bool", this::toConfVal, boolDefault, val ->
		{
			if(boolTrue.contains(val))
				return true;
			else if(boolFalse.contains(val))
				return false;
			else
				return null;
		});
	}
	public int getInt(String name)
	{
		return getGeneric(name, "int", this::toConfVal, intDefault, val ->
		{
			try
			{
				return Integer.parseInt(val);
			} catch(NumberFormatException x)
			{
				return null;
			}
		});
	}
	public void setString(String name, String val)
	{
		values.put(name, val);
	}
	public void setBool(String name, boolean val)
	{
		values.put(name, toConfVal(val));
	}
	public void setInt(String name, int val)
	{
		values.put(name, toConfVal(val));
	}

	private <T> T getGeneric(String name, String convValTypeName, Function<T, String> toConfVal, T defaultVal, Function<String, T> parseConvVal)
	{
		if(values.containsKey(name))
		{
			String val = values.get(name);
			T t = parseConvVal.apply(val);
			if(t == null)
			{
				System.err.println("<Config> Illegal " + convValTypeName + " config value: \"" + val + "\" given for \"" + name + "\"; defaulting to \"" + toConfVal.apply(defaultVal) + '"');
				return defaultVal;
			} else
				return t;
		} else
		{
			System.err.println("<Config> No value given for \"" + name + "\"; defaulting to \"" + toConfVal.apply(defaultVal) + '"');
			return defaultVal;
		}
	}

	private String toConfVal(boolean val)
	{
		return val ? "yes" : "no";
	}
	private String toConfVal(int val)
	{
		return String.valueOf(val);
	}
}
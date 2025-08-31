package com.crabsfordummies;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CrabsForDummiesTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CrabsForDummiesPlugin.class);
		RuneLite.main(args);
	}
}
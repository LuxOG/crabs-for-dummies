package com.crabsfordummies;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;


@Slf4j
@PluginDescriptor(
	name = "CrabsForDummies"
)
public class CrabsForDummiesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private CrabsForDummiesConfig config;

	// Track when we're about to equip Elder Maul
	private int justClickedWield;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Crab stun plugin started");
		justClickedWield = 0;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// Clear the flag after processing
		if (justClickedWield > 0) {
			justClickedWield--;
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		// Check if player clicked to equip Elder Maul or Dragon Warhammer
		if ("Wield".equals(event.getMenuOption()) &&
			(event.getMenuEntry().getTarget().contains("Elder maul") || event.getMenuEntry().getTarget().contains("Dragon warhammer"))) {
			log.info("Player clicked to equip Elder Maul/Dragon Warhammer");
			justClickedWield=2;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if (client.getGameState() != GameState.LOGGED_IN || !isInChambers() || !hasMaulEquippedOrAboutTo()) {
			return;
		}

		// Check if this is a crab and we're adding an Attack option (your working fix!)
		if ("Attack".equals(event.getOption()) && isCrab(event.getTarget())) {
			log.info("Swapping Smash to top for crab");
			log.info(String.valueOf(justClickedWield));

			// Apply the original swapping logic immediately
			MenuEntry[] menuEntries = client.getMenu().getMenuEntries();
			int smashIdx = -1;
			int topIdx = menuEntries.length - 1;

			// Find the Smash option
			for (int i = 0; i < topIdx; i++) {
				if (Text.removeTags(menuEntries[i].getOption()).equals("Smash") &&
					isCrab(menuEntries[i].getTarget())) {
					smashIdx = i;
					break;
				}
			}

			if (smashIdx == -1) {
				return;
			}

			// Swap the Smash option to the top position (same logic as original)
			MenuEntry smashEntry = menuEntries[smashIdx];
			MenuEntry topEntry = menuEntries[topIdx];

			menuEntries[smashIdx] = topEntry;
			menuEntries[topIdx] = smashEntry;

			client.setMenuEntries(menuEntries);
		}
	}

	private boolean hasMaulEquippedOrAboutTo() {
		// Check if we just clicked to wield the maul
		if (justClickedWield > 0) {
			return true;
		}

		// Check if we already have it equipped
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		return equipment != null && (equipment.contains(21003) || equipment.contains(13576));
	}

	private boolean isInChambers() {
		// Chamber of Xeric region IDs
		int[] chamberRegions = {12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145, 13393, 13394, 13395, 13396, 13397, 13398, 13399};
		int[] mapRegions = client.getMapRegions();

		if (mapRegions == null) {
			return false;
		}

		for (int region : mapRegions) {
			for (int chamberRegion : chamberRegions) {
				if (region == chamberRegion) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isCrab(String target) {
		// Remove color tags and check if it's a crab
		String cleanTarget = Text.removeTags(target).toLowerCase();
		return cleanTarget.contains("crab");
	}

	@Provides
	CrabsForDummiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CrabsForDummiesConfig.class);
	}
}
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

	// Track when we're about to equip Elder Maul
	private int justClickedWield;
	// Track when we're about to equip something else (to revert to Attack)
	private int justClickedWieldOther;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Crab stun plugin started");
		justClickedWield = 0;
		justClickedWieldOther = 0;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// Clear the flags after processing
		if (justClickedWield > 0) {
			justClickedWield--;
		}
		if (justClickedWieldOther > 0) {
			justClickedWieldOther--;
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		// Check if player clicked to equip something
		if ("Wield".equals(event.getMenuOption())) {
			String target = event.getMenuEntry().getTarget();

			// Check if it's Elder Maul or Dragon Warhammer
			if (target.contains("Elder maul") || target.contains("Dragon warhammer")) {
				log.info("Player clicked to equip Elder Maul/Dragon Warhammer");
				justClickedWield = 2;
				justClickedWieldOther = 0; // Clear the other flag
			} else {
				// Player is wielding something else - should revert to Attack priority
				log.info("Player clicked to equip something else: " + target);
				justClickedWieldOther = 2;
				justClickedWield = 0; // Clear the maul flag
			}
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if (client.getGameState() != GameState.LOGGED_IN || !isInChambers()) {
			return;
		}

		// Check if this is a crab
		if (!isCrab(event.getTarget())) {
			return;
		}

		// Handle prioritizing Smash when we have Elder Maul
		if ("Attack".equals(event.getOption()) && hasMaulEquippedOrAboutTo() && !shouldRevertToAttack()) {
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

			// Swap the Smash option to the top position
			MenuEntry smashEntry = menuEntries[smashIdx];
			MenuEntry topEntry = menuEntries[topIdx];

			menuEntries[smashIdx] = topEntry;
			menuEntries[topIdx] = smashEntry;

			client.setMenuEntries(menuEntries);
		}
	}

	private boolean shouldRevertToAttack() {
		// We should revert to Attack if:
		// 1. We just clicked to wield something else (non-maul weapon), OR
		// 2. We don't have a maul equipped and didn't just click to wield one
		return justClickedWieldOther > 0 || (!hasMaulEquipped() && justClickedWield == 0);
	}

	private boolean hasMaulEquipped() {
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		return equipment != null && (equipment.contains(21003) || equipment.contains(13576));
	}

	private boolean hasMaulEquippedOrAboutTo() {
		// Check if we just clicked to wield the maul
		if (justClickedWield > 0) {
			return true;
		}

		// Check if we already have it equipped
		return hasMaulEquipped();
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

}
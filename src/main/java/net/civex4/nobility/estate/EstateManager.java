package net.civex4.nobility.estate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.civex4.nobility.Nobility;
import net.civex4.nobility.development.Development;
import net.civex4.nobility.development.DevelopmentType;
import net.civex4.nobility.group.Group;
import vg.civcraft.mc.civmodcore.api.ItemAPI;
import vg.civcraft.mc.civmodcore.api.ItemNames;
import vg.civcraft.mc.civmodcore.chatDialog.Dialog;
import vg.civcraft.mc.civmodcore.inventorygui.Clickable;
import vg.civcraft.mc.civmodcore.inventorygui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;

public class EstateManager {
	
	public ArrayList<Estate> estates = new ArrayList<Estate>();
	
	//HashMap player-estate
	private HashMap<Player, Estate> estateOfPlayer = new HashMap<Player, Estate>();
	
	/*public EstateManager() {
		ArrayList<Estate> estates = new ArrayList<Estate>();
	}*/
	
	public boolean isVulnerable(Estate e) {
		int h = e.getVulnerabilityHour(); //should be between 0 and 23;
		Calendar rightNow = Calendar.getInstance();
		int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
		return currentHour >= h && currentHour < ((h+2) % 24);
	}
	
	public Estate createEstate(Block block, Player player) {
		Group group = Nobility.getGroupManager().getGroup(player);	
		block.setType(Material.ENDER_CHEST);		
		Estate estate = new Estate(block, group);		
		estates.add(estate);
		
		player.sendMessage("You have created an estate for " + group.getName());
		setEstateOfPlayer(player, estate);
		
		return estate;
		
	}
	
	/* Need to create a menu with the options players can take with a development
	 * The options are upgrade, enable, disable, see other developments in region,
	 * and destroy, plus any additional features. Storehouse needs a feature, "open
	 * storehouse inventory" 
	 */
	
	public void openEstateGUI(Player player) {
		Estate estate = getEstateOfPlayer(player);
		final int rowLength = 9;
		ClickableInventory estateGUI = new ClickableInventory(rowLength * 3, estate.getGroup().getName());

		// BUTTONS:
		// DEVELOPMENT GUI
		ItemStack developmentGUIIcon = createIcon(Material.CRAFTING_TABLE, "Build a Development");
		Clickable developmentButton = new Clickable(developmentGUIIcon) {
			@Override
			public void clicked(Player p) {
				openBuildGUI(p);
			}			
		};
		estateGUI.addSlot(developmentButton);
		
		// RENAME ESTATE
		ItemStack renameIcon = createIcon(Material.FEATHER, "Rename this Estate");
		Clickable estateNameButton = new Clickable(renameIcon) {

			@Override
			public void clicked(Player p) {
				p.closeInventory();
				new Dialog(player, Nobility.getNobility(), "Enter in a new name:") {
					
					@Override
					public List<String> onTabComplete(String wordCompleted, String[] fullMessage) {
						return null;
					}
					
					@Override
					public void onReply(String[] message) {
						// Set messages to one word
						String newName = "";
						for (String str : message) {newName = newName + str + " ";}
						
						estate.getGroup().setName(newName);
						
						player.sendMessage("This Estate is now called " + newName);
						this.end();
					}
				};
				
			}
			
		};
		estateGUI.addSlot(estateNameButton);
		
		// DECORATION STACKS
		for (int i = 0; i < rowLength * 2; i++) {
			if (!(estateGUI.getSlot(i) instanceof Clickable)) {
				Clickable c = new DecorationStack(createIcon(Material.BLACK_STAINED_GLASS, " "));
				estateGUI.setSlot(c, i);
			}
		}
		
		// BUTTONS:
		// ACTIVE DEVELOPMENTS:
		for(Development development: estate.getActiveDevelopments()) {
			DevelopmentType type = development.getDevelopmentType();
			Material m = type.getIcon();
			ItemStack icon = new ItemStack(m);
			nameItem(icon, type.getTitle());
			addLore(icon, ChatColor.GREEN + "Active");

			
			if (!type.getUpkeepCost().isEmpty()) {			
				addLore(icon, ChatColor.YELLOW + "Upkeep Cost:");
				for (ItemStack cost : type.getUpkeepCost()) {
					addLore(icon, ItemNames.getItemName(cost) + ": " + cost.getAmount());
				}

			}
			if (type.getResource() != null) {
				addLore(icon, "Collection Power (base): " + development.getCollectionPower() * development.getProductivity() + " (4)");
				addLore(icon, "Region Total: " + estate.getRegion().getResource(type.getResource().toUpperCase()));
				//addLore(icon, "Percent: " + TODO: actualYield / regionTotal);
				//TODO: Actual Yield, Food Usage, if (foodUsage != maximum) "Click to increase food usage"
			}

			// IF ACTIVE DEVELOPMENT IS CLICKED
			Clickable c = new Clickable(icon) {				
				@Override
				public void clicked(Player p) {
					// TODO open development options menu
					String developmentName = development.getDevelopmentType().getTitle();
					development.deactivate();
					player.sendMessage(developmentName + " is now inactive");
					openEstateGUI(p);
				}
			};
			
			estateGUI.addSlot(c);
		}
		
		// INACTIVE DEVELOPMENTS:
		for(Development development: estate.getInactiveDevelopments()) {
			Material m = Material.FIREWORK_STAR;
			ItemStack icon = new ItemStack(m);
			nameItem(icon, development.getDevelopmentType().getTitle());
			addLore(icon, ChatColor.RED + "Inactive");
			addLore(icon, "Click to Activate");

			
			// IF INACTIVE DEVELOPMENT IS CLICKED
			Clickable c = new Clickable(icon) {				
				@Override
				public void clicked(Player p) {
					// TODO if development has enough food...
					String developmentName = development.getDevelopmentType().getTitle();
					development.activate();
					player.sendMessage(developmentName + " is now active");
					openEstateGUI(p);
				}
			};
			
			estateGUI.addSlot(c);
		}
		
		// OPEN
		estateGUI.showInventory(player);
	}
	
	
	// TODO Could use CivModCore for item renaming
	public void openBuildGUI(Player player) {
		Estate estate = getEstateOfPlayer(player);
		// TODO Estate name length can't be longer than 32
		ClickableInventory developmentGUI = new ClickableInventory(9, "Build");
		
		// UNBUILT AVAILABLE DEVELOPMENTS
		for(DevelopmentType type: estate.getUnbuiltDevelopments()) {			
			if (estate.getActiveDevelopmentsToString().containsAll(type.getPrerequisites())) {
				Material m = type.getIcon();
				ItemStack icon = new ItemStack(m);
				nameItem(icon, type.getTitle());
				addLore(icon, ChatColor.YELLOW + "Not Yet Constructed");
				
				if (!type.getPrerequisites().isEmpty()) {
					addLore(icon, ChatColor.YELLOW + "Prerequisites:");
					for(String prerequisite : type.getPrerequisites()) {
						addLore(icon, DevelopmentType.getDevelopmentType(prerequisite).getTitle());
					}
					addLore(icon, "");
				}
				
				if (!type.getUpkeepCost().isEmpty()) {
					addLore(icon, ChatColor.YELLOW + "Upkeep Cost:");
					for(ItemStack cost : type.getUpkeepCost()) {						
						addLore(icon, ItemNames.getItemName(cost) + ": " + cost.getAmount());
					}
					addLore(icon, "");
				}
				
				if(!type.getInitialCost().isEmpty() ) {
					addLore(icon, ChatColor.YELLOW + "Initial Cost:");
					for(ItemStack cost : type.getInitialCost()) {
						addLore(icon, ItemNames.getItemName(cost) +  ": " + cost.getAmount());
					}
					if(!Nobility.getDevelopmentManager().checkCosts(type, estate)) {
						addLore(icon, ChatColor.RED + "Not enough to construct this estate");
					}
				}
				
				// IF UNBUILT DEVELOPMENT IS CLICKED:
				Clickable c = new Clickable(icon) {				
					@Override
					public void clicked(Player p) {
						if (!Nobility.getDevelopmentManager().checkCosts(type, estate)) {
							player.sendMessage("You don't have enough to construct this development");
							return;
						}
						estate.buildDevelopment(type);
						player.sendMessage("You constructed a " + type.getTitle());
						openEstateGUI(p);
					}
				};
				
				developmentGUI.addSlot(c);
			}			
		}
		
		developmentGUI.showInventory(player);
	}
	
	//Utility method to rename an item. Returns bold.
	public static ItemStack nameItem(ItemStack item, String name) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.BOLD + name);		
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Utility method to add a single line of lore text to an item
	 * @param item ItemStack item to add lore text to
	 * @param text String text to add
	 */
	public static void addLore(ItemStack item, String text) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		if(lore == null) lore = new ArrayList<>();
		lore.add(text);
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
	
	public static ItemStack createIcon(Material mat, String name) {
		ItemStack icon = new ItemStack(mat);
		ItemAPI.setDisplayName(icon, ChatColor.WHITE + name);
		return icon;
	}
	
	
	//getters and setters for player-estate hashmap
	public void setEstateOfPlayer(Player p, Estate e) {
		estateOfPlayer.put(p, e);
	}
	
	public Estate getEstateOfPlayer(Player p) {
		return estateOfPlayer.get(p);
	}
	
	public boolean playerHasEstate(Player p) {
		return estateOfPlayer.containsKey(p);
	}

	
}
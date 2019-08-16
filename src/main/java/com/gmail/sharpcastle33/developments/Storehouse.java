package com.gmail.sharpcastle33.developments;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.gmail.sharpcastle33.development.Development;
import com.gmail.sharpcastle33.estate.Estate;

public class Storehouse extends Development {
	
	private static String name = "Storehouse";
	private static ItemStack icon = new ItemStack(Material.IRON_PICKAXE);
	private static int price = 5;
	
	public Storehouse(Estate estate) {
		super(Storehouse.name, Storehouse.icon, Storehouse.price);	
	}
	
	static void buildStorehouse(Estate estate) {
		Location loc = estate.getBlock().getLocation().add(0, 0, -1);
		loc.getBlock().setType(Material.CHEST);
	}

}

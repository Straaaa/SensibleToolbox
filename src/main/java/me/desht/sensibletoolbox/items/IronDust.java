package me.desht.sensibletoolbox.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class IronDust extends BaseSTBItem {
	@Override
	public Material getBaseMaterial() {
		return Material.SULPHUR;
	}

	@Override
	public String getItemName() {
		return "Iron Dust";
	}

	@Override
	public String[] getLore() {
		return new String[] { "Smelt in a Smelter or Furnace", " to get iron ingots"};
	}

	@Override
	public Recipe getRecipe() {
		return null;  // Only made by the Masher
	}

	@Override
	public boolean hasGlow() {
		return true;
	}

	@Override
	public ItemStack getSmeltingResult() {
		return new ItemStack(Material.IRON_INGOT);
	}
}

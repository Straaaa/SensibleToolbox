package me.desht.sensibletoolbox.blocks.machines;

import me.desht.sensibletoolbox.items.GoldDust;
import me.desht.sensibletoolbox.items.IronDust;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.Dye;

public class Masher extends SimpleProcessingMachine {
	private static CustomRecipeCollection recipes = new CustomRecipeCollection();

	static {
		System.out.println("adding masher recipes");
		recipes.addCustomRecipe(new ItemStack(Material.COBBLESTONE), new ItemStack(Material.SAND), 120);
		recipes.addCustomRecipe(new ItemStack(Material.GRAVEL), new ItemStack(Material.SAND), 80);
		Dye white = new Dye();
		white.setColor(DyeColor.WHITE);
		recipes.addCustomRecipe(new ItemStack(Material.BONE), white.toItemStack(5), 40);
		recipes.addCustomRecipe(new ItemStack(Material.BLAZE_ROD), new ItemStack(Material.BLAZE_POWDER, 4), 80);
		recipes.addCustomRecipe(new ItemStack(Material.COAL_ORE), new ItemStack(Material.COAL, 2), 100);
		recipes.addCustomRecipe(new ItemStack(Material.REDSTONE_ORE), new ItemStack(Material.REDSTONE, 6), 100);
		recipes.addCustomRecipe(new ItemStack(Material.DIAMOND_ORE), new ItemStack(Material.DIAMOND, 2), 160);
		recipes.addCustomRecipe(new ItemStack(Material.IRON_ORE), new IronDust().toItemStack(2), 120);
		recipes.addCustomRecipe(new ItemStack(Material.GOLD_ORE), new GoldDust().toItemStack(2), 80);
		recipes.addCustomRecipe(new ItemStack(Material.WOOL), new ItemStack(Material.STRING, 4), 60);
	}

	public Masher() {
	}

	public Masher(ConfigurationSection conf) {
		super(conf);
	}

	@Override
	public Material getBaseMaterial() {
		return Material.STAINED_CLAY;
	}

	@Override
	public Byte getBaseBlockData() {
		return DyeColor.GREEN.getWoolData();
	}

	@Override
	public String getItemName() {
		return "Masher";
	}

	@Override
	public String[] getLore() {
		return new String[] { "Grinds ores and other resources ", " into dusts" };
	}

	@Override
	public Recipe getRecipe() {
		ShapedRecipe recipe = new ShapedRecipe(toItemStack(1));
		recipe.shape("FFF", "SIS", "RGR");
		recipe.setIngredient('F', Material.FLINT);
		recipe.setIngredient('S', Material.STONE);
		recipe.setIngredient('I', Material.IRON_BLOCK);
		recipe.setIngredient('R' ,Material.REDSTONE);
		recipe.setIngredient('G', Material.GOLD_INGOT);
		return recipe;
	}

	@Override
	public boolean insertItem(ItemStack item, BlockFace face) {
		return false;
	}

	@Override
	public ItemStack extractItem(BlockFace face) {
		return null;
	}

	@Override
	public int[] getInputSlots() {
		return new int[] { 10 };
	}

	@Override
	public int[] getOutputSlots() {
		return new int[] { 14 };
	}

	@Override
	protected int getInventorySize() {
		return 36;
	}

	@Override
	public int getMaxCharge() {
		return 1000;
	}

	@Override
	public boolean acceptsItemType(ItemStack item) {
		System.out.println("do we accept " + item + " ? " + recipes.hasRecipe(item));
		return recipes.hasRecipe(item);
	}


	@Override
	protected CustomRecipeCollection.CustomRecipe getCustomRecipeFor(ItemStack stack) {
		return recipes.get(stack);
	}

	@Override
	protected int getProgressItemSlot() {
		return 12;
	}

	@Override
	protected int getProgressCounterSlot() {
		return 3;
	}
}

package me.desht.sensibletoolbox.items;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.ItemGlow;
import me.desht.sensibletoolbox.STBFreezable;
import me.desht.sensibletoolbox.SensibleToolboxPlugin;
import me.desht.sensibletoolbox.api.Chargeable;
import me.desht.sensibletoolbox.attributes.AttributeStorage;
import me.desht.sensibletoolbox.blocks.*;
import me.desht.sensibletoolbox.blocks.machines.Masher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Constructor;
import java.util.*;

public abstract class BaseSTBItem implements STBFreezable, Comparable<BaseSTBItem> {
	protected static final ChatColor LORE_COLOR = ChatColor.GRAY;
	private static final String STB_LORE_LINE = ChatColor.DARK_GRAY.toString() + ChatColor.ITALIC + "Sensible Toolbox item";
	protected static final ChatColor DISPLAY_COLOR = ChatColor.YELLOW;
	private static final Map<String, Class<? extends BaseSTBItem>> registry = new HashMap<String, Class<? extends BaseSTBItem>>();
	private static final Map<String, String> ids = new HashMap<String, String>();
	private static final Map<Material,Class<? extends BaseSTBItem>> customSmelts = new HashMap<Material, Class<? extends BaseSTBItem>>();
	private Map<Enchantment,Integer> enchants;

	public static void registerItems(SensibleToolboxPlugin plugin) {
		registerItem(new AngelicBlock());
		registerItem(new EnderLeash());
		registerItem(new RedstoneClock());
		registerItem(new BlockUpdateDetector());
		registerItem(new BagOfHolding());
		registerItem(new WateringCan());
		registerItem(new MoistureChecker());
		registerItem(new AdvancedMoistureChecker());
		registerItem(new WoodCombineHoe());
		registerItem(new IronCombineHoe());
		registerItem(new GoldCombineHoe());
		registerItem(new DiamondCombineHoe());
		registerItem(new TrashCan());
		registerItem(new PaintBrush());
		registerItem(new PaintRoller());
		registerItem(new PaintCan());
		registerItem(new Elevator());
		registerItem(new TapeMeasure());
		registerItem(new BuildersMultiTool());
		registerItem(new Floodlight());
		registerItem(new Masher());
		registerItem(new IronDust());
		registerItem(new GoldDust());
		if (plugin.isProtocolLibEnabled()) {
			registerItem(new SoundMuffler());
		}
	}

	private static void registerItem(BaseSTBItem item) {
		registry.put(item.getItemName(), item.getClass());
		ids.put(item.getItemID(), item.getItemName());
	}

	public static void setupRecipes() {
		for (String key : registry.keySet()) {
			BaseSTBItem item = getItemByName(key);
			Recipe r = item.getRecipe();
			if (r != null) {
				Bukkit.addRecipe(r);
			}
			ItemStack stack = item.getSmeltingResult();
			if (stack != null) {
				FurnaceRecipe fr = new FurnaceRecipe(stack, item.getBaseMaterial());
				Bukkit.addRecipe(fr);
				customSmelts.put(item.getBaseMaterial(), item.getClass());
			}
		}
	}

	public static Class<? extends BaseSTBItem> getCustomSmelt(Material mat) {
		return customSmelts.get(mat);
	}

	public static Set<String> getItemIds() {
		return ids.keySet();
	}

	public static BaseSTBItem getItemFromItemStack(ItemStack stack) {
		if (!isSTBItem(stack)) {
			return null;
		}
		System.out.println("get item from stack : " + stack);
		String disp = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
		Configuration conf = BaseSTBItem.getItemAttributes(stack);
		BaseSTBItem item = getItemByName((disp.split(":"))[0], conf);
		if (item != null) {
			item.storeEnchants(stack);
		}
		return item;
	}

	public static <T extends BaseSTBItem> T getItemFromItemStack(ItemStack stack, Class<T> type) {
		BaseSTBItem item = getItemFromItemStack(stack);
		if (item != null && type.isAssignableFrom(item.getClass())) {
			return type.cast(item);
		} else {
			return null;
		}
	}

	public static BaseSTBItem getItemById(String id) {
		return ids.containsKey(id) ? BaseSTBItem.getItemByName(ids.get(id)) : null;
	}

	public static BaseSTBItem getItemByName(String disp) {
		return getItemByName(disp, null);
	}

	public static BaseSTBItem getItemByName(String disp, Configuration conf) {
		Class<? extends BaseSTBItem> c = registry.get(disp);
		if (c == null) {
			return null;
		}
		try {
			if (conf == null) {
				Constructor<? extends BaseSTBItem> cons = c.getDeclaredConstructor();
				return cons.newInstance();
			} else {
				Constructor<? extends BaseSTBItem> cons = c.getDeclaredConstructor(ConfigurationSection.class);
				return cons.newInstance(conf);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean isSTBItem(ItemStack stack) {
		if (stack == null) {
			return false;
		}
		ItemMeta im = stack.getItemMeta();
		if (im != null && im.hasLore()) {
			List<String> lore = im.getLore();
			return !lore.isEmpty() && lore.get(0).equals(STB_LORE_LINE);
		} else {
			return false;
		}
	}

	protected static Configuration getItemAttributes(ItemStack stack) {
		AttributeStorage storage = AttributeStorage.newTarget(stack, SensibleToolboxPlugin.UNIQUE_ID);
		YamlConfiguration conf = new YamlConfiguration();
		try {
			String s = storage.getData("");
			if (s != null) {
				conf.loadFromString(s);
				Debugger.getInstance().debug("get item attributes for " + stack + ":");
				for (String k : conf.getKeys(false)) { Debugger.getInstance().debug("- " + k + "=" + conf.get(k)); }
				return conf;
			} else {
				return null;
			}
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
			return new MemoryConfiguration();
		}
	}

	public static String getChargeString(Chargeable ch) {
		double d = ch.getCharge() / ch.getMaxCharge();
		ChatColor cc;
		if (d < 0.333) {
			cc = ChatColor.RED;
		} else if (d < 0.666) {
			cc = ChatColor.GOLD;
		} else {
			cc = ChatColor.GREEN;
		}
		return ChatColor.WHITE + "\u2301 " + cc + Math.round(ch.getCharge()) + "/" + ch.getMaxCharge();
	}

	private void storeEnchants(ItemStack stack) {
		enchants = stack.getEnchantments();
	}

	/**
	 * Get the Bukkit Material used for this item.
	 *
	 * @return the Bukkit Material
	 */
	public abstract Material getBaseMaterial();

	/**
	 * Get the block data byte used for this item.
	 *
	 * @return the block data
	 */
	public Byte getBaseBlockData() { return null; }

	/**
	 * Get the item's displayed name.
	 *
	 * @return the item name
	 */
	public abstract String getItemName();

	/**
	 * Get any suffix to be appended to the item's displayed name.  Override this in
	 * implementing classes where you wish to represent some or all of the item's state
	 * in the display name.
	 *
	 * @return the display suffix
	 */
	public String getDisplaySuffix() { return null; }

	/**
	 * Get the base lore to display for the item.
	 *
	 * @return the item lore
	 */
	public abstract String[] getLore();

	/**
	 * Get extra lore to be appended to the base lore.  verride this in
	 * implementing classes where you wish to represent some or all of the item's state
	 * in the item lore.
	 *
	 * @return the extra item lore
	 */
	public String[] getExtraLore() { return new String[0]; }

	/**
	 * Get the recipe used to create the item.
	 *
	 * @return the recipe
	 */
	public abstract Recipe getRecipe();

	/**
	 * Check if the item should glow.  This will only work if ProtocolLib is installed.
	 *
	 * @return true if the item should glow
	 */
	public boolean hasGlow() { return false; }

	/**
	 * Called when a player interacts with a block or air while holding an STB item.
	 *
	 * @param event the interaction event.
	 */
	public void handleItemInteraction(PlayerInteractEvent event) { }

	/**
	 * Called when a player attempts to consume an STB item (which must be food or potion).
	 *
	 * @param event the consume event
	 */
	public void handleConsume(PlayerItemConsumeEvent event) { }

	/**
	 * Called when a player interacts with an entity while holding an STB item.
	 *
	 * @param event the interaction event
	 */
	public void handleEntityInteraction(PlayerInteractEntityEvent event) { }

	/**
	 * Called when a player rolls the mouse wheel while sneaking and holding an STB item.
	 *
	 * @param event the held item change event
	 */
	public void handleMouseWheel(PlayerItemHeldEvent event) { }

	/**
	 * Get the item into which this object would be smelted into
	 *
	 * @return the resulting itemstack, or null if this object does not smelt
	 */
	public ItemStack getSmeltingResult() { return null; }

	/**
	 * Get an ItemStack from this STB item, serializing any item-specific data into the ItemStack.
	 *
	 * @param amount number of items in the stack
	 * @return the new ItemStack
	 */
	public ItemStack toItemStack(int amount) {
		ItemStack res = new ItemStack(getBaseMaterial(), amount);
		if (getBaseBlockData() != null) {
			res.setDurability(getBaseBlockData());
		}
		ItemMeta im = res.getItemMeta();
		String suffix = getDisplaySuffix() == null ? "" : ": " + getDisplaySuffix();
		im.setDisplayName(DISPLAY_COLOR + getItemName() + suffix);
		im.setLore(buildLore());
		res.setItemMeta(im);
		if (enchants != null) {
			res.addUnsafeEnchantments(enchants);
		}
		if (SensibleToolboxPlugin.getInstance().isProtocolLibEnabled()) {
			ItemGlow.setGlowing(res, hasGlow());
		}

		if (this instanceof Chargeable && res.getType().getMaxDurability() > 0) {
			// encode the STB item's charge level into the itemstack's damage bar
			Chargeable ch = (Chargeable) this;
			short max = res.getType().getMaxDurability();
			double d = ch.getCharge() / (double) ch.getMaxCharge();
			short dur = (short)(max * d);
			res.setDurability((short)(max - dur));
		}

		// any serialized data from the object goes in the ItemStack attributes
		YamlConfiguration conf = freeze();
		if (conf.getKeys(false).size() > 0) {
			AttributeStorage storage = AttributeStorage.newTarget(res, SensibleToolboxPlugin.UNIQUE_ID);
			String data = conf.saveToString();
			storage.setData(data);
			Debugger.getInstance().debug("serialize to itemstack:\n" + data);
			return storage.getTarget();
		} else {
			return res;
		}
	}

	private List<String> buildLore() {
		String[] lore = getLore();
		String[] lore2 = getExtraLore();
		List<String> res = new ArrayList<String>(lore.length + lore2.length + 1);
		res.add(STB_LORE_LINE);
		for (String l : lore) {
			res.add(LORE_COLOR + l);
		}
		Collections.addAll(res, lore2);
		return res;
	}

	@Override
	public YamlConfiguration freeze() {
		return new YamlConfiguration();
	}

	@Override
	public String toString() {
		return "STB item: " + getItemName();
	}

	@Override
	public int compareTo(BaseSTBItem other) {
		return getItemName().compareTo(other.getItemName());
	}

	public String getItemID() {
		return getItemName().replace(" ", "").replace("'", "").toLowerCase();
	}
}

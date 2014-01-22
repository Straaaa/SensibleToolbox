package me.desht.sensibletoolbox.blocks;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.sensibletoolbox.SensibleToolboxPlugin;
import me.desht.sensibletoolbox.items.BaseSTBItem;
import me.desht.sensibletoolbox.storage.BlockPosition;
import me.desht.sensibletoolbox.storage.LocationManager;
import me.desht.sensibletoolbox.util.RelativePosition;
import me.desht.sensibletoolbox.util.STBUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

public abstract class BaseSTBBlock extends BaseSTBItem {
	public static final String STB_MULTI_BLOCK = "STB_MultiBlock_Origin";
	private PersistableLocation persistableLocation;
	private BlockFace facing;

	protected BaseSTBBlock() {
	}

	public BaseSTBBlock(ConfigurationSection conf) {
		setFacing(BlockFace.valueOf(conf.getString("facing", "SELF")));
	}

	public BlockFace getFacing() {
		return facing;
	}

	public void setFacing(BlockFace facing) {
		this.facing = facing;
	}

	/**
	 * Called when an STB block receives a damage event.  The default behaviour is to ignore
	 * the event.
	 *
	 * @param event the block damage event
	 */
	public void onBlockDamage(BlockDamageEvent event) { }

	/**
	 * Called when an STB block receives a physics event.  The default behaviour is to ignore
	 * the event.
	 *
	 * @param event the block physics event
	 */
	public void onBlockPhysics(BlockPhysicsEvent event) { }

	/**
	 * Called when an STB block is interacted with by a player.  The default behaviour allows
	 * for the block to be labelled by left-clicking it with a sign in hand.  If you override
	 * this method and want to keep this behaviour, be sure to call super.onInteractBlock()
	 *
	 * @param event the interaction event
	 */
	public void onInteractBlock(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().getItemInHand().getType() == Material.SIGN) {
			// attach a label sign
			attachLabelSign(event);
		}
	}

	/**
	 * Called when a sign attached to an STB block is updated.  The default behaviour is to ignore
	 * the event.
	 *
	 * @param event the sign change event
	 * @return true if the sign should be popped off the block
	 */
	public boolean onSignChange(SignChangeEvent event) { return false; }

	/**
	 * Called when this STB block has been hit by an explosion.  The default behaviour is to return
	 * true; STB blocks will break and drop their item form if hit by an explosion.
	 *
	 * @param event the explosion event
	 * @return true if the explosion should cause the block to break, false otherwise
	 */
	public boolean onEntityExplode(EntityExplodeEvent event) {
		return true;
	}

	/**
	 * Get a list of extra blocks this STB block has.  By default this returns an empty list,
	 * but multi-block structures should override this.  Each element of the list is a vector
	 * containing a relative offset from the item's base location.
	 *
	 * @return an array of relative offsets for extra blocks in the item
	 */
	public RelativePosition[] getBlockStructure() { return new RelativePosition[0]; }

	/**
	 * Called every tick for each STB block that is placed in the world, for any STB block where
	 * shouldTick() returns true.
	 */
	public void onServerTick() { }

	/**
	 * Called when the chunk that an STB block is in gets loaded.
	 */
	public void onChunkLoad() { }

	/**
	 * Called when the chunk that an STB block is in gets unloaded.
	 */
	public void onChunkUnload() { }

	/**
	 * Check if this block needs to tick, i.e. have onServerTick() called every tick.  Only override
	 * this to return true for block which need to tick, for performance reasons.
	 *
	 * @return true if the block should tick
	 */
	public boolean shouldTick() {
		return false;
	}

	/**
	 * Get the location of the base block of this STB block.  This could be null if called
	 * on an STB Block object which has not yet been placed in the world (i.e. in item form).
	 *
	 * @return the base block location
	 */
	public Location getLocation() {
		return persistableLocation == null ? null : persistableLocation.getLocation();
	}

	/**
	 * Set the location of the base block of this STB block.  This should only be called when the
	 * block is first placed, or when deserialized.
	 *
	 * @param loc the base block location
	 * @throws IllegalStateException if the caller attempts to set a non-null location when the object already has a location set
	 */
	public void setLocation(Location loc) {
		if (loc != null) {
			if (persistableLocation != null && !loc.equals(persistableLocation.getLocation())) {
				throw new IllegalStateException("Attempt to change the location of existing STB block @ " + persistableLocation);
			}
			Block origin = loc.getBlock();
			persistableLocation = new PersistableLocation(loc);
			BlockPosition pos0 = new BlockPosition(origin.getLocation());
			for (RelativePosition pos : getBlockStructure()) {
				Block b1 = getMultiBlock(pos);
				Debugger.getInstance().debug(2, "multiblock for " + this + " -> " + b1);
				b1.setMetadata(STB_MULTI_BLOCK, new FixedMetadataValue(SensibleToolboxPlugin.getInstance(), pos0));
			}
		} else {
			if (persistableLocation != null) {
				Block origin = persistableLocation.getBlock();
				for (RelativePosition pos : getBlockStructure()) {
					Block b1 = getMultiBlock(pos);
					b1.removeMetadata(STB_MULTI_BLOCK, SensibleToolboxPlugin.getInstance());
				}
			}
			persistableLocation = null;
		}
	}

	protected Block getMultiBlock(RelativePosition pos) {
		if (getLocation() == null) {
			return null;
		}
		Block b = getLocation().getBlock();
		int dx = 0, dz = 0;
		switch (getFacing()) {
			case NORTH: dz = -pos.getFront(); dx = -pos.getLeft(); break;
			case SOUTH: dz = pos.getFront(); dx = pos.getLeft(); break;
			case EAST: dz = -pos.getLeft(); dx = pos.getFront(); break;
			case WEST: dz = pos.getLeft(); dx = -pos.getFront(); break;
		}
		return b.getRelative(dx, pos.getUp(), dz);
	}

	/**
	 * Called when an STB block is placed.  Subclasses may override this method, but should take care
	 * to call the superclass method.
	 *
	 * @param event the block place event
	 */
	public void onBlockPlace(BlockPlaceEvent event) {
		placeBlock(event.getBlock(), STBUtil.getFaceFromYaw(event.getPlayer().getLocation().getYaw()).getOppositeFace());
	}

	/**
	 * Called when an STB block is broken.  Subclasses may override this method, but should take care
	 * to call the superclass method.
	 *
	 * @param event the block break event
	 */
	public void onBlockBreak(BlockBreakEvent event) {
		breakBlock(event.getBlock());
		event.setCancelled(true);
	}

	protected void placeBlock(Block b, BlockFace facing) {
		setFacing(facing);
		LocationManager.getManager().registerLocation(b.getLocation(), this);
	}

	public void breakBlock(Block b) {
		b.getWorld().dropItemNaturally(b.getLocation(), toItemStack(1));
		Block origin = getLocation().getBlock();
		origin.setType(Material.AIR);
		for (RelativePosition pos : getBlockStructure()) {
			Block b1 = getMultiBlock(pos);
			b1.setType(Material.AIR);
		}
		LocationManager.getManager().unregisterLocation(getLocation());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BaseSTBBlock that = (BaseSTBBlock) o;

		if (persistableLocation != null ? !persistableLocation.equals(that.persistableLocation) : that.persistableLocation != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return persistableLocation != null ? persistableLocation.hashCode() : 0;
	}

	public void updateBlock() {
		updateBlock(true);
	}

	public void updateBlock(boolean redraw) {
		if (getLocation() != null) {
			if (redraw) {
				Block b = getLocation().getBlock();
				b.setTypeIdAndData(getBaseMaterial().getId(), getBaseBlockData(), true);
			}
			LocationManager.getManager().updateLocation(getLocation());
		}
	}

	protected void attachLabelSign(PlayerInteractEvent event) {
		Block signBlock = event.getClickedBlock().getRelative(event.getBlockFace());
		signBlock.setTypeIdAndData(event.getBlockFace() == BlockFace.UP ? Material.SIGN_POST.getId() : Material.WALL_SIGN.getId(), (byte) 0, false);
		Sign sign = (Sign) signBlock.getState();
		org.bukkit.material.Sign s = (org.bukkit.material.Sign) sign.getData();
		if (event.getBlockFace() == BlockFace.UP) {
			s.setFacingDirection(STBUtil.getFaceFromYaw(event.getPlayer().getLocation().getYaw()).getOppositeFace());
		} else {
			s.setFacingDirection(event.getBlockFace());
		}
		sign.setData(s);
		String[] text = getSignLabel();
		for (int i = 0; i < text.length; i++) {
			sign.setLine(i, text[i]);
		}
		sign.update();
		ItemStack stack = event.getPlayer().getItemInHand();
		if (stack.getAmount() > 1) {
			stack.setAmount(stack.getAmount() - 1);
			event.getPlayer().setItemInHand(stack);
		} else {
			event.getPlayer().setItemInHand(null);
		}
	}

	protected String[] getSignLabel() {
		return new String[] { getItemName(), "", "", "" };
	}

	@Override
	public String toString() {
		return "STB block: " + getItemName() + " @ " +
				(getLocation() == null ? "(null)" : MiscUtil.formatLocation(getLocation()));
	}

	/**
	 * Temporarily override the item display name, just before the item is placed.  The item
	 * display name is used as the inventory title for blocks such as the dropper.
	 *
	 * @param event the block place event
	 */
	protected void setInventoryTitle(BlockPlaceEvent event, final String tempTitle) {
		ItemStack inHand = event.getItemInHand();
		final Player player = event.getPlayer();
		ItemMeta meta = inHand.getItemMeta();
		meta.setDisplayName(tempTitle);
		inHand.setItemMeta(meta);
		if (inHand.getAmount() > 1) {
			// any remaining items need to have their proper title restored
			Bukkit.getScheduler().runTask(SensibleToolboxPlugin.getInstance(), new Runnable() {
				@Override
				public void run() {
					ItemStack inHand = player.getItemInHand();
					if (inHand.getType() == getBaseMaterial()) {
						ItemMeta meta = inHand.getItemMeta();
						if (meta.getDisplayName().equals(tempTitle)) {
							player.setItemInHand(toItemStack(inHand.getAmount()));
						}
					}
				}
			});
		}
	}
}

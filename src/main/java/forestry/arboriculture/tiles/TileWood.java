/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.arboriculture.tiles;

import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import forestry.api.arboriculture.EnumWoodType;
import forestry.arboriculture.IWoodTyped;
import forestry.core.network.DataInputStreamForestry;
import forestry.core.network.DataOutputStreamForestry;
import forestry.core.network.IStreamable;
import forestry.core.network.PacketTileStream;
import forestry.core.tiles.TileUtil;

public class TileWood extends TileEntity implements IStreamable {
	private EnumWoodType woodType;

	public TileWood() {
		this.woodType = EnumWoodType.LARCH;
	}

	public void setWoodType(EnumWoodType woodType) {
		this.woodType = woodType;
		markDirty();
	}

	public EnumWoodType getWoodType() {
		return woodType;
	}

	@Override
	public boolean canUpdate() {
		return false;
	}

	/* NETWORK */
	@Override
	public Packet getDescriptionPacket() {
		return new PacketTileStream(this).getPacket();
	}

	@Override
	public void writeData(DataOutputStreamForestry data) throws IOException {
		data.writeVarInt(woodType.ordinal());
	}

	@Override
	public void readData(DataInputStreamForestry data) throws IOException {
		int ordinal = data.readVarInt();
		woodType = EnumWoodType.VALUES[ordinal];
		worldObj.func_147479_m(xCoord, yCoord, zCoord);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if (woodType != null) {
			nbt.setInteger("WT", woodType.ordinal());
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (nbt.hasKey("WT")) {
			int ordinal = nbt.getInteger("WT");
			woodType = EnumWoodType.VALUES[ordinal];
		}
	}

	public static NBTTagCompound getTagCompound(IBlockAccess world, int x, int y, int z) {
		TileWood wood = getWoodTile(world, x, y, z);
		NBTTagCompound nbttagcompound = new NBTTagCompound();
		if (wood == null) {
			return nbttagcompound;
		}
		EnumWoodType woodType = wood.getWoodType();
		woodType.saveToCompound(nbttagcompound);
		return nbttagcompound;
	}

	public static TileWood getWoodTile(IBlockAccess world, int x, int y, int z) {
		return TileUtil.getTile(world, x, y, z, TileWood.class);
	}

	public static <T extends Block & IWoodTyped> ArrayList<ItemStack> getDrops(T block, World world, int x, int y, int z) {
		ArrayList<ItemStack> drops = new ArrayList<>();

		TileWood wood = getWoodTile(world, x, y, z);
		if (wood != null) {
			ItemStack stack = new ItemStack(block);
			NBTTagCompound compound = new NBTTagCompound();
			wood.getWoodType().saveToCompound(compound);
			stack.setTagCompound(compound);
			drops.add(stack);
		}

		return drops;
	}
}

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
package forestry.core.network;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.MinecraftForge;

import forestry.api.core.ForestryEvent;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IBreedingTracker;
import forestry.api.genetics.ISpeciesRoot;
import forestry.core.genetics.BreedingTracker;
import forestry.core.proxy.Proxies;

public class PacketGenomeTrackerUpdate extends PacketNBT implements IForestryPacketClient {

	public PacketGenomeTrackerUpdate() {
	}

	public PacketGenomeTrackerUpdate(NBTTagCompound nbtTagCompound) {
		super(PacketIdClient.GENOME_TRACKER_UPDATE, nbtTagCompound);
	}

	@Override
	public void onPacketData(DataInputStreamForestry data, EntityPlayer player) throws IOException {
		IBreedingTracker tracker = null;
		String type = getTagCompound().getString(BreedingTracker.TYPE_KEY);

		ISpeciesRoot root = AlleleManager.alleleRegistry.getSpeciesRoot(type);
		if (root != null) {
			tracker = root.getBreedingTracker(Proxies.common.getRenderWorld(), player.getGameProfile());
		}
		if (tracker != null) {
			tracker.decodeFromNBT(getTagCompound());
			MinecraftForge.EVENT_BUS.post(new ForestryEvent.SyncedBreedingTracker(tracker, player));
		}
	}
}

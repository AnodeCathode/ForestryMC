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
package forestry.factory.tiles;

import java.io.IOException;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import forestry.api.core.IErrorLogic;
import forestry.api.recipes.IStillRecipe;
import forestry.core.config.Constants;
import forestry.core.errors.EnumErrorCode;
import forestry.core.fluids.FluidHelper;
import forestry.core.fluids.TankManager;
import forestry.core.fluids.tanks.FilteredTank;
import forestry.core.fluids.tanks.StandardTank;
import forestry.core.network.DataInputStreamForestry;
import forestry.core.network.DataOutputStreamForestry;
import forestry.core.network.GuiId;
import forestry.core.render.TankRenderInfo;
import forestry.core.tiles.ILiquidTankTile;
import forestry.core.tiles.TilePowered;
import forestry.factory.inventory.InventoryStill;
import forestry.factory.recipes.StillRecipeManager;

public class TileStill extends TilePowered implements ISidedInventory, ILiquidTankTile, IFluidHandler {
	private static final int ENERGY_PER_RECIPE_TIME = 200;

	private final FilteredTank resourceTank;
	private final FilteredTank productTank;
	private final TankManager tankManager;

	private IStillRecipe currentRecipe;
	private FluidStack bufferedLiquid;

	public TileStill() {
		super(GuiId.StillGUI, "still", 1100, 8000);
		setInternalInventory(new InventoryStill(this));
		resourceTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY, StillRecipeManager.recipeFluidInputs);
		resourceTank.tankMode = StandardTank.TankMode.INPUT;
		productTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY, StillRecipeManager.recipeFluidOutputs);
		productTank.tankMode = StandardTank.TankMode.OUTPUT;
		tankManager = new TankManager(this, resourceTank, productTank);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		tankManager.writeToNBT(nbttagcompound);

		if (bufferedLiquid != null) {
			NBTTagCompound buffer = new NBTTagCompound();
			bufferedLiquid.writeToNBT(buffer);
			nbttagcompound.setTag("Buffer", buffer);
		}

	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		tankManager.readFromNBT(nbttagcompound);

		if (nbttagcompound.hasKey("Buffer")) {
			NBTTagCompound buffer = nbttagcompound.getCompoundTag("Buffer");
			bufferedLiquid = FluidStack.loadFluidStackFromNBT(buffer);
		}
	}

	@Override
	public void writeData(DataOutputStreamForestry data) throws IOException {
		super.writeData(data);
		tankManager.writeData(data);
	}

	@Override
	public void readData(DataInputStreamForestry data) throws IOException {
		super.readData(data);
		tankManager.readData(data);
	}

	@Override
	public void updateServerSide() {
		super.updateServerSide();

		if (updateOnInterval(20)) {
			FluidHelper.drainContainers(tankManager, this, InventoryStill.SLOT_CAN);

			FluidStack fluidStack = productTank.getFluid();
			if (fluidStack != null) {
				FluidHelper.fillContainers(tankManager, this, InventoryStill.SLOT_RESOURCE, InventoryStill.SLOT_PRODUCT, fluidStack.getFluid());
			}
		}
	}

	@Override
	public boolean workCycle() {

		int cycles = currentRecipe.getCyclesPerUnit();
		FluidStack output = currentRecipe.getOutput();

		FluidStack product = new FluidStack(output, output.amount * cycles);
		productTank.fill(product, true);

		bufferedLiquid = null;

		return true;
	}

	private void checkRecipe() {
		FluidStack recipeLiquid = bufferedLiquid != null ? bufferedLiquid : resourceTank.getFluid();

		if (!StillRecipeManager.matches(currentRecipe, recipeLiquid)) {
			currentRecipe = StillRecipeManager.findMatchingRecipe(recipeLiquid);

			int recipeTime = currentRecipe == null ? 0 : currentRecipe.getCyclesPerUnit();
			setEnergyPerWorkCycle(ENERGY_PER_RECIPE_TIME * recipeTime);
			setTicksPerWorkCycle(recipeTime);
		}
	}

	@Override
	public boolean hasWork() {
		checkRecipe();

		boolean hasRecipe = (currentRecipe != null);
		boolean hasTankSpace = true;
		boolean hasFluidResource = true;

		if (hasRecipe) {
			hasTankSpace = productTank.canFill(currentRecipe.getOutput());
			if (bufferedLiquid == null) {
				int cycles = currentRecipe.getCyclesPerUnit();
				FluidStack input = currentRecipe.getInput();
				int drainAmount = cycles * input.amount;
				hasFluidResource = resourceTank.canDrain(drainAmount);
				if (hasFluidResource) {
					bufferedLiquid = new FluidStack(input, drainAmount);
					resourceTank.drain(drainAmount, true);
				}
			}
		}

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(!hasRecipe, EnumErrorCode.NORECIPE);
		errorLogic.setCondition(!hasTankSpace, EnumErrorCode.NOSPACETANK);
		errorLogic.setCondition(!hasFluidResource, EnumErrorCode.NORESOURCE);

		return hasRecipe && hasFluidResource && hasTankSpace;
	}

	@Override
	public TankRenderInfo getResourceTankInfo() {
		return new TankRenderInfo(resourceTank);
	}

	@Override
	public TankRenderInfo getProductTankInfo() {
		return new TankRenderInfo(productTank);
	}

	/* IFluidHandler */
	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		return tankManager.fill(from, resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		return tankManager.drain(from, resource, doDrain);
	}

	@Override
	public FluidStack drain(ForgeDirection from, int quantityMax, boolean doEmpty) {
		return tankManager.drain(from, quantityMax, doEmpty);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		return tankManager.canFill(from, fluid);
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return tankManager.canDrain(from, fluid);
	}

	@Override
	public TankManager getTankManager() {
		return tankManager;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return tankManager.getTankInfo(from);
	}
}

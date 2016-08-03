package hellfirepvp.astralsorcery.common.starlight.transmission.base;

import hellfirepvp.astralsorcery.common.starlight.WorldNetworkHandler;
import hellfirepvp.astralsorcery.common.starlight.transmission.IPrismTransmissionNode;
import hellfirepvp.astralsorcery.common.starlight.transmission.ITransmissionNode;
import hellfirepvp.astralsorcery.common.starlight.transmission.NodeConnection;
import hellfirepvp.astralsorcery.common.util.NBTUtils;
import hellfirepvp.astralsorcery.common.util.RaytraceAssist;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: TransmissionNodeLens
 * Created by HellFirePvP
 * Date: 03.08.2016 / 11:09
 */
public class SimpleTransmissionNode implements ITransmissionNode {

    private boolean nextReachable = false;
    private BlockPos nextPos = null;
    private double dstToNext = 0;
    private RaytraceAssist assistNext = null;

    private BlockPos thisPos;

    private Set<BlockPos> sourcesToThis = new HashSet<>();

    public SimpleTransmissionNode(@Nonnull BlockPos thisPos) {
        this.thisPos = thisPos;
    }

    @Override
    public BlockPos getPos() {
        return thisPos;
    }

    @Override
    public void notifyUnlink(World world, BlockPos to) {
        if(to.equals(nextPos)) { //cleanup
            this.nextPos = null;
            this.assistNext = null;
            this.dstToNext = 0;
            this.nextReachable = false;
        }
    }

    @Override
    public void notifyLink(World world, BlockPos pos) {
        addLink(world, pos, true, false);
    }

    private void addLink(World world, BlockPos pos, boolean doRayTest, boolean oldRayState) {
        this.nextPos = pos;
        this.assistNext = new RaytraceAssist(world, thisPos, nextPos);
        if(doRayTest) {
            this.nextReachable = assistNext.isClear();
        } else {
            this.nextReachable = oldRayState;
        }
        this.dstToNext = pos.getDistance(thisPos.getX(), thisPos.getY(), thisPos.getZ());
    }

    @Override
    public void notifyBlockChange(World world, BlockPos at) {
        if(nextPos == null) return;
        double dstStart = thisPos.getDistance(at.getX(), at.getY(), at.getZ());
        double dstEnd = nextPos.getDistance(at.getX(), at.getY(), at.getZ());
        if(dstStart > dstToNext || dstEnd > dstToNext) return; //out of range
        this.nextReachable = assistNext.isClear();
    }

    @Override
    public void notifySourceLink(World world, BlockPos source) {
        if(!sourcesToThis.contains(source)) sourcesToThis.add(source);
    }

    @Override
    public void notifySourceUnlink(World world, BlockPos source) {
        sourcesToThis.remove(source);
    }

    @Override
    public NodeConnection<IPrismTransmissionNode> queryNextNode(WorldNetworkHandler handler) {
        return new NodeConnection<>(handler.getTransmissionNode(nextPos), nextPos, nextReachable);
    }

    @Override
    public List<BlockPos> getSources() {
        return sourcesToThis.stream().collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public IPrismTransmissionNode provideEmptyNBTReadInstance() {
        return new SimpleTransmissionNode(null);
    }

    @Override
    public void readFromNBT(World world, NBTTagCompound compound) {
        this.thisPos = NBTUtils.readBlockPosFromNBT(compound);
        this.sourcesToThis.clear();

        NBTTagList list = compound.getTagList("sources", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            sourcesToThis.add(NBTUtils.readBlockPosFromNBT(list.getCompoundTagAt(i)));
        }

        if(compound.hasKey("nextPos")) {
            NBTTagCompound tag = compound.getCompoundTag("nextPos");
            BlockPos next = NBTUtils.readBlockPosFromNBT(tag);
            boolean oldRay = tag.getBoolean("rayState");
            addLink(world, next, false, oldRay);
        }
    }

    @Override
    public void writeToNBT(World world, NBTTagCompound compound) {
        NBTUtils.writeBlockPosToNBT(thisPos, compound);

        NBTTagList sources = new NBTTagList();
        for (BlockPos source : sourcesToThis) {
            NBTTagCompound comp = new NBTTagCompound();
            NBTUtils.writeBlockPosToNBT(source, comp);
            sources.appendTag(comp);
        }
        compound.setTag("sources", compound);

        if(nextPos != null) {
            NBTTagCompound pos = new NBTTagCompound();
            NBTUtils.writeBlockPosToNBT(nextPos, pos);
            pos.setBoolean("rayState", nextReachable);
            compound.setTag("nextPos", pos);
        }
    }

}

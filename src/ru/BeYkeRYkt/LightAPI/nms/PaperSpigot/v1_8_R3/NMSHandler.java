package ru.BeYkeRYkt.LightAPI.nms.PaperSpigot.v1_8_R3;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumSkyBlock;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.WorldServer;
import ru.BeYkeRYkt.LightAPI.ChunkCoord;
import ru.BeYkeRYkt.LightAPI.nms.INMSHandler;

public class NMSHandler implements INMSHandler {
	private static BlockFace[] SIDES = { BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
			BlockFace.WEST };
	private static Field cachedChunkModified;

	public void createLight(Location location, int light) {
		WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
		BlockPosition position = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		world.a(EnumSkyBlock.BLOCK, position, light);

		Block adjacent = getAdjacentAirBlock(location.getBlock());
		recalculateBlockLighting(location.getWorld(), adjacent.getX(), adjacent.getY(), adjacent.getZ());
	}

	public void deleteLight(Location location) {
		recalculateBlockLighting(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	public List<ChunkCoord> collectChunks(Location location) {
		List<ChunkCoord> list = new CopyOnWriteArrayList<ChunkCoord>();
		try {
			WorldServer nmsWorld = ((CraftWorld) location.getChunk().getWorld()).getHandle();
			for (int dX = -1; dX <= 1; dX++) {
				for (int dZ = -1; dZ <= 1; dZ++) {
					if (nmsWorld.chunkProviderServer.isChunkLoaded(location.getChunk().getX() + dX,
							location.getChunk().getZ() + dZ)) {
						net.minecraft.server.v1_8_R3.Chunk chunk = nmsWorld.getChunkAt(location.getChunk().getX() + dX,
								location.getChunk().getZ() + dZ);
						Field isModified = getChunkField(chunk);
						if (isModified.getBoolean(chunk)) {
							ChunkCoord cCoord = new ChunkCoord(location.getWorld(), chunk.locX, chunk.locZ);
							list.add(cCoord);
							chunk.f(false);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public void recalculateBlockLighting(org.bukkit.World world, int x, int y, int z) {
		WorldServer nmsWorld = ((CraftWorld) world).getHandle();
		BlockPosition pos = new BlockPosition(x, y, z);
		nmsWorld.updateLight(EnumSkyBlock.BLOCK, pos);
	}

	public Block getAdjacentAirBlock(Block block) {
		for (BlockFace face : SIDES) {
			if ((block.getY() != 0) || (face != BlockFace.DOWN)) {
				if ((block.getY() != 255) || (face != BlockFace.UP)) {
					Block candidate = block.getRelative(face);
					if (candidate.getType().isTransparent()) {
						return candidate;
					}
				}
			}
		}
		return block;
	}

	private static Field getChunkField(Object chunk) throws NoSuchFieldException, SecurityException {
		if (cachedChunkModified == null) {
			cachedChunkModified = chunk.getClass().getDeclaredField("q");
			cachedChunkModified.setAccessible(true);
		}
		return cachedChunkModified;
	}

	private void sendPacket(net.minecraft.server.v1_8_R3.Chunk chunk) {
		for (EntityHuman human : chunk.world.players) {
			EntityPlayer player = (EntityPlayer) human;
			net.minecraft.server.v1_8_R3.Chunk pChunk = player.world
					.getChunkAtWorldCoords(player.getChunkCoordinates());
			if (distanceTo(pChunk, chunk) < 5) {
				PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(chunk, false, 65535);
				player.playerConnection.sendPacket(packet);
			}
		}
	}

	private int distanceTo(net.minecraft.server.v1_8_R3.Chunk from, net.minecraft.server.v1_8_R3.Chunk to) {
		if (!from.world.getWorldData().getName().equals(to.world.getWorldData().getName())) {
			return 100;
		}
		double var2 = to.locX - from.locX;
		double var4 = to.locZ - from.locZ;
		return (int) Math.sqrt(var2 * var2 + var4 * var4);
	}

	public void updateChunk(ChunkCoord cCoord) {
		net.minecraft.server.v1_8_R3.Chunk chunk = ((CraftWorld) cCoord.getWorld()).getHandle()
				.getChunkAt(cCoord.getX(), cCoord.getZ());
		sendPacket(chunk);
	}

}

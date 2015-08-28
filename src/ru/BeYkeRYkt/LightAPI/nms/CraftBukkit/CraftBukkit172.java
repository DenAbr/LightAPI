package ru.BeYkeRYkt.LightAPI.nms.CraftBukkit;

import ru.BeYkeRYkt.LightAPI.LightAPI;
import ru.BeYkeRYkt.LightAPI.nms.BukkitImpl;

public class CraftBukkit172 implements BukkitImpl {

	@Override
	public String getNameImpl() {
		return "Bukkit-1.7.2";
	}

	@Override
	public String getPath() {
		String packageName = LightAPI.getInstance().getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		return "ru.BeYkeRYkt.LightAPI.nms.CraftBukkit." + version + ".NMSHandler";
	}

}

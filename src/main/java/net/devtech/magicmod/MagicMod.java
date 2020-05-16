package net.devtech.magicmod;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MagicMod implements PreLaunchEntrypoint {
	private static final Field ENTRYPOINT_STORAGE;
	private static final Field ENTRY_MAP;
	private static final Method GET_MOD_CONTAINER;

	public static net.fabricmc.loader.ModContainer whatIsThis(Object object) {
		try {
			return (net.fabricmc.loader.ModContainer) GET_MOD_CONTAINER.invoke(object);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	static {
		try {
			GET_MOD_CONTAINER = Class.forName("net.fabricmc.loader.EntrypointStorage$Entry").getDeclaredMethod("getModContainer");
			GET_MOD_CONTAINER.setAccessible(true);
			ENTRYPOINT_STORAGE = net.fabricmc.loader.FabricLoader.class.getDeclaredField("entrypointStorage");
			ENTRYPOINT_STORAGE.setAccessible(true);
			ENTRY_MAP = Class.forName("net.fabricmc.loader.EntrypointStorage").getDeclaredField("entryMap");
			ENTRY_MAP.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onPreLaunch() {
		try {
			Object storage = ENTRYPOINT_STORAGE.get(FabricLoader.getInstance());
			Map<String, List<?>> entryMap = (Map<String, List<?>>) ENTRY_MAP.get(storage); // Map<String, List<Entry>>
			// guarentee mod load order for all entrypoints
			for (Map.Entry<String, List<?>> entry : entryMap.entrySet()) {
				List<?> b = entry.getValue();
				b.sort(Comparator.comparing(a -> whatIsThis(a).getMetadata().getId()));
			}

			ArrayList<ModContainer> containers = new ArrayList<>(FabricLoader.getInstance().getAllMods());
			containers.sort(Comparator.comparing(a -> a.getMetadata().getId()));
			for (ModContainer mod : containers) {
				String id = mod.getMetadata().getId();
				try {
					Class<?> magic = Class.forName("magic." + id);
					Method method = magic.getDeclaredMethod("i", Map.class);
					method.invoke(null, entryMap);
				} catch (ReflectiveOperationException ignored) {}
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}

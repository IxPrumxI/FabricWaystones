package wraith.fwaystones;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wraith.fwaystones.packets.WaystonePacketHandler;
import wraith.fwaystones.registry.*;
import wraith.fwaystones.util.FWConfig;
import wraith.fwaystones.util.WaystoneStorage;
import wraith.fwaystones.util.WaystonesEventManager;
import java.io.File;

public class FabricWaystones implements ModInitializer {

    public static final FWConfig CONFIG;
    public static final Logger LOGGER = LogManager.getLogger("Fabric-Waystones");
    public static final String MOD_ID = "fwaystones";
    public static WaystoneStorage WAYSTONE_STORAGE;

    static {
        var configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fwaystones/config.json");
        if (configFile.exists()) {
            FabricWaystones.LOGGER.info("Old config file found, migrating...");
            configFile.renameTo(new File(FabricLoader.getInstance().getConfigDir().toFile(), "fwaystones/config.json5"));
        }
        CONFIG = FWConfig.createAndLoad();
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Is initializing.");
        DataComponentRegistry.init();
        BlockRegistry.registerBlocks();
        BlockEntityRegistry.registerBlockEntities();
        ItemRegistry.init();
        CompatRegistry.init();
        CustomScreenHandlerRegistry.registerScreenHandlers();
        WaystonesEventManager.registerEvents();
        WaystonePacketHandler.registerPackets();
        WaystonePacketHandler.registerPacketHandlers();

        LOGGER.info("Has successfully been initialized.");
    }

}

package crystal.hordes;

import crystal.hordes.config.HordesCommand;
import crystal.hordes.event.TickHandler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheHordes implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Hordes");
	@Override
	public void onInitialize() {
        LOGGER.info("Initializing mod");
        HordesCommand.register();
        ServerTickEvents.END_SERVER_TICK.register(TickHandler::onServerTick);
    }
}
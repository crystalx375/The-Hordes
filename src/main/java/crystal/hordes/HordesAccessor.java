package crystal.hordes;

import java.util.UUID;

public interface HordesAccessor {
    /**
     * Просто достаем через интерфейс для использования
     **/
    // Зомби потому что раньше были зомбаки, а сейчас нет
    void the_Hordes$setHordeZombie(boolean value, UUID clusterId, UUID targetPlayerUuid);
    boolean the_Hordes$isHordeZombie();
    UUID the_Hordes$getTargetPlayerUuid();
}

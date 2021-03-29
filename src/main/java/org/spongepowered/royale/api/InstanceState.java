package org.spongepowered.royale.api;

public interface InstanceState {

    default boolean canAnyoneMove() {
        return true;
    }

    default boolean canAnyoneInteract() {
        return false;
    }

    default boolean doesCancelTasksOnAdvance() {
        return true;
    }

    default boolean doesCheckRoundStatusOnAdvance() {
        return true;
    }
}
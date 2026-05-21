package org.awp0rtuh1ty.hearth.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import org.awp0rtuh1ty.hearth.DustBag;
import org.awp0rtuh1ty.hearth.Repellent;
import org.awp0rtuh1ty.hearth.client.screen.DustBagScreen;
import org.awp0rtuh1ty.hearth.client.screen.RepellentScreen;

public class HearthClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(DustBag.DUST_BAG_SCREEN_HANDLER, DustBagScreen::new);
        MenuScreens.register(Repellent.REPELLENT_SCREEN_HANDLER, RepellentScreen::new);
        RepellentOverlayRenderer.register();
    }
}

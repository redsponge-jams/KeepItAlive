package com.redsponge.keepitalive;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.redsponge.redengine.assets.AssetSpecifier;
import com.redsponge.redengine.screen.systems.RenderSystem;

public interface GameableScreen extends SizableScreen{

    AssetSpecifier getAssets();

    void notified(Object obj, int notification);

    void beginMoveAnimation(Human from, Human to);
    boolean isMoving();

    Array<Human> getHumans();

    FitViewport getGUIViewport();

    RenderSystem getRenderSystem();

    boolean isPlayerDead();

    ParticleManager getPM();

}

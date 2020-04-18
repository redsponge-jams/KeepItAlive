package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.redsponge.redengine.assets.AssetSpecifier;
import com.redsponge.redengine.screen.AbstractScreen;
import com.redsponge.redengine.screen.systems.RenderSystem;
import com.redsponge.redengine.utils.GameAccessor;

public class GameScreen extends AbstractScreen {

    private RenderSystem renderSystem;
    private PlayerController controller;
    private Array<Human> humans;

    public GameScreen(GameAccessor ga) {
        super(ga);
    }

    @Override
    public void show() {
        humans = new Array<>();
        renderSystem = getEntitySystem(RenderSystem.class);
        renderSystem.getBackground().set(Color.GRAY);
        Human h = new Human(batch, shapeRenderer);
        addHuman(h);
        controller = new PlayerController(this);
        controller.setControlled(h);

        addHuman(new Human(batch, shapeRenderer));
        addHuman(new Human(batch, shapeRenderer));
//        addHuman(new Human(batch, shapeRenderer));
//        addHuman(new Human(batch, shapeRenderer));
//        addHuman(new Human(batch, shapeRenderer));
    }

    public void addHuman(Human h) {
        humans.add(h);
        addEntity(h);
    }

    @Override
    public int getScreenWidth() {
        return 320;
    }

    @Override
    public int getScreenHeight() {
        return 180;
    }

    @Override
    public void tick(float v) {
        controller.tick(v);
        shapeRenderer.setProjectionMatrix(renderSystem.getCamera().combined);
        tickEntities(v);
        updateEngine(v);
    }

    @Override
    public void render() {
        controller.render(batch, shapeRenderer);
    }

    @Override
    public Class<? extends AssetSpecifier> getAssetSpecsType() {
        return null;
    }


    @Override
    public void notified(Object notifier, int notification) {
        super.notified(notifier, notification);
        if(notification == Notifications.LOST) {
            Gdx.app.exit();
        }
    }

    @Override
    public void reSize(int width, int height) {
        renderSystem.resize(width, height);
    }

    public Array<Human> getHumans() {
        return humans;
    }
}

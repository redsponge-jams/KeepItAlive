package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.redsponge.redengine.assets.AssetSpecifier;
import com.redsponge.redengine.screen.AbstractScreen;
import com.redsponge.redengine.screen.systems.RenderSystem;
import com.redsponge.redengine.transitions.Transitions;
import com.redsponge.redengine.utils.GameAccessor;

public class GameScreen extends AbstractScreen {

    private RenderSystem renderSystem;
    private PlayerController controller;
    private Array<Human> humans;

    private FrameBuffer buff;
    private FitViewport viewport;
    private Texture tex;
    private TextureRegion reg;

    private int gameWidth = 640, gameHeight = 360;
    private FitViewport guiViewport;

    public GameScreen(GameAccessor ga) {
        super(ga);
    }

    @Override
    public void show() {
        guiViewport = new FitViewport(getScreenWidth(), getScreenHeight());
        humans = new DelayedRemovalArray<>();
        renderSystem = getEntitySystem(RenderSystem.class);
        Human h = new Human(batch, shapeRenderer);
        addHuman(h);
        controller = new PlayerController(this);
        controller.setControlled(h);

        addEntity(new Background(batch, shapeRenderer));

        addHuman(new Human(batch, shapeRenderer));
        addHuman(new Human(batch, shapeRenderer));
        addHuman(new Doctor(batch, shapeRenderer));
        addHuman(new Doctor(batch, shapeRenderer));
        addHuman(new Doctor(batch, shapeRenderer));

        tex = assets.get("syringe", Texture.class);
        viewport = new FitViewport(48, 16);
        buff = new FrameBuffer(Pixmap.Format.RGBA8888, 48, 16, false);
        buff.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        reg = new TextureRegion(buff.getColorBufferTexture());
        reg.flip(false, true);
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
        if(!transitioning) {
            controller.tick(v);
        }
        if(controller.isChoosingTarget()) {
            v /= 5f;
        }
        shapeRenderer.setProjectionMatrix(renderSystem.getCamera().combined);
        controller.focusCamera(renderSystem.getCamera());
        Vector3 p = renderSystem.getCamera().position;
        if(p.x < renderSystem.getViewport().getWorldWidth() / 2f) {
            p.x = renderSystem.getViewport().getWorldWidth() / 2f;
        }

        if(p.y < renderSystem.getViewport().getWorldHeight() / 2f) {
            p.y = renderSystem.getViewport().getWorldHeight() / 2f;
        }

        if(p.x > gameWidth - (renderSystem.getViewport().getWorldWidth() / 2f)) {
            p.x = gameWidth - (renderSystem.getViewport().getWorldWidth() / 2f);
        }

        if(p.y > gameHeight - (renderSystem.getViewport().getWorldHeight() / 2f)) {
            p.y = gameHeight - (renderSystem.getViewport().getWorldHeight() / 2f);
        }
        renderSystem.getViewport().apply();

        tickEntities(v);
        updateEngine(v);
    }

    @Override
    public void render() {
//        Gdx.gl.glClearColor(0, 0, 0, 1);
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderEntities();
        controller.render(batch, shapeRenderer);

//        viewport.apply();
//        batch.setProjectionMatrix(viewport.getCamera().combined);
//        buff.begin();
//        Gdx.gl.glClearColor(1, 0, 0, 1);
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//        batch.begin();
//        batch.draw(tex, 0, 0, 16, 16);
//        batch.end();
//        buff.end();
//
//        renderSystem.getViewport().apply();
//        batch.setProjectionMatrix(renderSystem.getViewport().getCamera().combined);
//        batch.begin();
//        batch.draw(reg, 0, 0, 48, 16);
////        batch.draw(tex, 20, 10);
//        batch.end();
    }

    @Override
    public Class<? extends AssetSpecifier> getAssetSpecsType() {
        return GameAssets.class;
    }


    @Override
    public void notified(Object notifier, int notification) {
        super.notified(notifier, notification);
        controller.notified(notifier, notification);
        if(notification == Notifications.LOST) {
            ga.transitionTo(new GameScreen(ga), Transitions.sineSlide(1, batch, shapeRenderer));
        }
    }

    @Override
    public void reSize(int width, int height) {
        renderSystem.resize(width, height);
        guiViewport.update(width, height, true);
    }

    @Override
    public void disposeAssets() {
        controller.dispose();
    }

    public Array<Human> getHumans() {
        return humans;
    }

    public RenderSystem getRenderSystem() {
        return renderSystem;
    }

    public FitViewport getGUIViewport() {
        return guiViewport;
    }

    public float getGameWidth() {
        return gameWidth;
    }

    public int getGameHeight() {
        return gameHeight;
    }
}

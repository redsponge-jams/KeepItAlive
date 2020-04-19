package com.redsponge.keepitalive;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.redsponge.redengine.assets.AssetSpecifier;
import com.redsponge.redengine.assets.Fonts;
import com.redsponge.redengine.render.util.ScreenFiller;
import com.redsponge.redengine.screen.AbstractScreen;
import com.redsponge.redengine.screen.components.Mappers;
import com.redsponge.redengine.screen.components.PositionComponent;
import com.redsponge.redengine.screen.systems.RenderSystem;
import com.redsponge.redengine.utils.GameAccessor;
import com.redsponge.redengine.utils.MathUtilities;

public class GameScreen extends AbstractScreen {

    private RenderSystem renderSystem;
    private PlayerController controller;
    private Array<Human> humans;

    private int gameWidth = 640, gameHeight = 360;
    private FitViewport guiViewport;

    private Human moveFrom;
    private Human moveTo;
    private float moveTime;
    private boolean isMoving;
    private float time;

    private boolean isDead;
    private DeathCause deathCause;
    private BitmapFont font;

    private Texture warnTexture;
    private float deathTime;

    public GameScreen(GameAccessor ga) {
        super(ga);
    }

    @Override
    public void show() {
        guiViewport = new FitViewport(getScreenWidth(), getScreenHeight());
        humans = new DelayedRemovalArray<>();
        renderSystem = getEntitySystem(RenderSystem.class);
        Human h = new Human(batch, shapeRenderer, gameWidth / 2, gameHeight / 2);
        addHuman(h);
        controller = new PlayerController(this);
        controller.setControlled(h);

        addEntity(new Background(batch, shapeRenderer));

        addEntity(new EntitySpawner(batch, shapeRenderer, 100, 100));
        addEntity(new EntitySpawner(batch, shapeRenderer, gameWidth - 100, 100));
        addEntity(new EntitySpawner(batch, shapeRenderer, gameWidth - 100, gameHeight - 100));
        addEntity(new EntitySpawner(batch, shapeRenderer, 100, gameHeight - 100));
        font = Fonts.getFont("pixelmix", 16);

        warnTexture = assets.get("warnTexture", Texture.class);
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

    public void beginMoveAnimation(Human from, Human to) {
        this.moveFrom = from;
        this.moveTo = to;
        this.isMoving = true;
        this.moveTime = 0;
    }

    @Override
    public void tick(float v) {
        time += v;
        if(isDead) {
            OrthographicCamera cam = renderSystem.getCamera();
            cam.zoom = MathUtilities.lerp(cam.zoom, 0.5f, 0.1f);
            deathTime += v;
        } else {
            if (!transitioning) {
                if (isMoving) {
                    moveTime += v * 4;
                    if (moveTime > 1) {
                        isMoving = false;
                        controller.setControlled(moveTo);
                    } else {
                        v = 0;
                    }
                }
                controller.tick(v);
            }
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
        if(isMoving) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            PositionComponent posFrom = Mappers.position.get(moveFrom);
            PositionComponent posTo = Mappers.position.get(moveTo);
            shapeRenderer.rectLine(posFrom.getX() + 8, posFrom.getY() + 8, posTo.getX() + 8, posTo.getY() + 8, 2, Color.GREEN, new Color(0, 0.5f, 0, 1.0f));

            float angle = MathUtils.atan2(posFrom.getY() - posTo.getY(), posFrom.getX() - posTo.getX());
            double progress = Interpolation.exp5In.apply(moveTime) * Vector2.dst(posFrom.getX(), posFrom.getY(), posTo.getX(), posTo.getY());
            float vx = -(float) (Math.cos(angle) * progress);
            float vy = -(float) (Math.sin(angle) * progress);
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.circle(posFrom.getX() + 8 + vx, posFrom.getY() + 8 + vy, 5);
            shapeRenderer.end();
        }


        if(controller.getControlled() != null){
            guiViewport.apply();
            batch.setProjectionMatrix(guiViewport.getCamera().combined);
            batch.begin();
            if(controller.getControlled().getHPRatio() < 0.5f) {
                batch.setColor(1, 1,1, .5f - MathUtils.map(0, 0.5f, 0, .5f, controller.getControlled().getHPRatio()));
                batch.draw(warnTexture, 0, 0);
            }
            batch.end();
        }

        if(isDead) {
            ScreenFiller.fillScreen(shapeRenderer, 0, 0, 0, 0.5f);

            guiViewport.apply();
            batch.setProjectionMatrix(guiViewport.getCamera().combined);
            batch.begin();
            font.getColor().a = (deathTime / 2f) > 1 ? 1 : (deathTime / 2f);
            font.draw(batch, deathCause.getMsg(),  0, 50, guiViewport.getWorldWidth(), Align.center, true);
            if(deathTime > 1) {
                font.getColor().a = ((deathTime - 1) / 2f) > 1 ? 1 : ((deathTime - 1) / 2f);
                font.getData().setScale(0.5f);
                font.draw(batch, "Press Enter To Replay", 0, 25, guiViewport.getWorldWidth(), Align.center, true);
                font.draw(batch, "Press ESC To Go To Menu", 0, 10, guiViewport.getWorldWidth(), Align.center, true);
                font.getData().setScale(1);
            }
            font.getColor().a = 1;
            batch.end();
        }
    }

    @Override
    public Class<? extends AssetSpecifier> getAssetSpecsType() {
        return GameAssets.class;
    }


    @Override
    public void notified(Object notifier, int notification) {
        if(!isDead) {
            if (notification == Notifications.CONTROLLED_HEALED) {
                isDead = true;
                deathCause = DeathCause.HOST_HEALED;
            } else if (notification == Notifications.HOST_DIED) {
                isDead = true;
                deathCause = DeathCause.HOST_DIED;
            }
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

    public boolean isMoving() {
        return isMoving;
    }

    public float getTimeAlive() {
        return time;
    }

    public boolean isPlayerDead() {
        return isDead;
    }
}

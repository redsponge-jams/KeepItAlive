package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.redsponge.redengine.screen.components.SizeComponent;
import com.redsponge.redengine.screen.systems.RenderSystem;
import com.redsponge.redengine.transitions.Transition;
import com.redsponge.redengine.transitions.Transitions;
import com.redsponge.redengine.utils.GameAccessor;
import com.redsponge.redengine.utils.MathUtilities;

import java.sql.Time;

public class GameScreen extends AbstractScreen implements GameableScreen {

    private RenderSystem renderSystem;
    private PlayerController controller;
    private Array<Human> humans;

    private final int gameWidth = 640;
    private final int gameHeight = 360;
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

    private int deaths;
    private float timeAlive;

    private TextureRegion humanDeathIcon;
    private Texture clockIcon;
    private Texture straightArrow;

    private ParticleManager pm;

    private Music music;
    private Sound deathSound;

    public GameScreen(GameAccessor ga) {
        super(ga);
    }

    public GameScreen(GameAccessor ga, Music music) {
        super(ga);
        this.music = music;
    }

    @Override
    public void show() {
        if(music == null) {
            music = Gdx.audio.newMusic(Gdx.files.internal("music/alienated.ogg"));
            music.setLooping(true);
            music.play();
        }
        deathSound = assets.get("loseSound", Sound.class);

        pm = new ParticleManager();
        guiViewport = new FitViewport(getScreenWidth(), getScreenHeight());
        humans = new DelayedRemovalArray<>();
        renderSystem = getEntitySystem(RenderSystem.class);
        Human h = new Human(batch, shapeRenderer, gameWidth / 2, gameHeight / 2);
        addHuman(h);
        controller = new PlayerController(this);
        controller.setControlled(h);

        addEntity(new Background(batch, shapeRenderer));

        addEntity(new EntitySpawner(batch, shapeRenderer, 30, 30));
        addEntity(new EntitySpawner(batch, shapeRenderer, gameWidth - 30, 30));
        addEntity(new EntitySpawner(batch, shapeRenderer, gameWidth - 30, gameHeight - 30));
        addEntity(new EntitySpawner(batch, shapeRenderer, 30, gameHeight - 30));
        font = Fonts.getFont("pixelmix", 16);

        warnTexture = assets.get("warnTexture", Texture.class);
        humanDeathIcon = assets.getTextureRegion("humanDead");
        straightArrow = assets.get("takeoverArrow", Texture.class);
        clockIcon = assets.get("clock", Texture.class);
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
            if(Gdx.input.isKeyJustPressed(Keys.ENTER) && !transitioning) {
                ga.transitionTo(new GameScreen(ga, music), Transitions.sineSlide(1, batch, shapeRenderer));
            }
            if(Gdx.input.isKeyJustPressed(Keys.ESCAPE) && !transitioning) {
                ga.transitionTo(new TutorialScreen(ga), Transitions.sineSlide(1, batch, shapeRenderer));
                music.dispose();
            }
        } else {
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
        if(controller.isChoosingTarget()) {
            v /= 5f;
        }
        if(!isDead) {
            timeAlive += v;
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
        pm.tick(v);
    }

    @Override
    public void render() {
        renderEntities();
        controller.render(batch, shapeRenderer);
        if(isMoving) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            PositionComponent posFrom = Mappers.position.get(moveFrom);
            SizeComponent sizeFrom = Mappers.size.get(moveFrom);
            PositionComponent posTo = Mappers.position.get(moveTo);
            shapeRenderer.rectLine(posFrom.getX() + 8, posFrom.getY() + 8, posTo.getX() + 8, posTo.getY() + 8, 2, Color.GREEN, new Color(0, 0.5f, 0, 1.0f));
            shapeRenderer.end();
            float angle = MathUtils.atan2(posFrom.getY() - posTo.getY(), posFrom.getX() - posTo.getX());
            double progress = Interpolation.exp5In.apply(moveTime) * Vector2.dst(posFrom.getX(), posFrom.getY(), posTo.getX(), posTo.getY());
            float vx = -(float) (Math.cos(angle) * progress);
            float vy = -(float) (Math.sin(angle) * progress);

            batch.begin();
            batch.setColor(Color.GREEN);
            batch.draw(straightArrow, posFrom.getX() + sizeFrom.getX() / 2f - 3 + vx, posFrom.getY() + sizeFrom.getY() / 2f + vy, 3, 0, 6, straightArrow.getHeight(), 1, 1, angle * MathUtils.radiansToDegrees + 90, 0, 0, 6, straightArrow.getHeight(), false, false);
            batch.end();
        }

        batch.begin();
        pm.render(batch);
        batch.end();


        guiViewport.apply();
        batch.setProjectionMatrix(guiViewport.getCamera().combined);
        batch.begin();

        if(controller.getControlled() != null){
            guiViewport.apply();
            if(controller.getControlled().getHPRatio() < 0.5f) {
                batch.setColor(1, 1,1, .5f - MathUtils.map(0, 0.5f, 0, .5f, controller.getControlled().getHPRatio()));
                batch.draw(warnTexture, 0, 0);
            }
        }
        font.getData().setScale(0.5f);
        batch.setColor(Color.WHITE);
        batch.draw(humanDeathIcon, 10, guiViewport.getWorldHeight() - 20);
        font.draw(batch, "" + deaths, 30, guiViewport.getWorldHeight() - 12);
        batch.draw(clockIcon, guiViewport.getWorldWidth() - 60, guiViewport.getWorldHeight() - 20);
        String timeFormat = String.format("%02d:%02d", (int) timeAlive / 60, (int) timeAlive % 60);
        font.draw(batch, timeFormat, guiViewport.getWorldWidth() - 40, guiViewport.getWorldHeight() - 9);
        batch.end();

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
                font.draw(batch, "Press ESC To Go Back To The Tutorial", 0, 10, guiViewport.getWorldWidth(), Align.center, true);
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
                deathSound.play();
            } else if (notification == Notifications.HOST_DIED) {
                isDead = true;
                deathCause = DeathCause.HOST_DIED;
                deathSound.play();
            }
            if(notification == Notifications.HUMAN_DIED) {
                deaths++;
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

    public float getGameHeight() {
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

    @Override
    public ParticleManager getPM() {
        return pm;
    }
}

package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.redsponge.redengine.assets.AssetSpecifier;
import com.redsponge.redengine.assets.Fonts;
import com.redsponge.redengine.screen.AbstractScreen;
import com.redsponge.redengine.screen.components.Mappers;
import com.redsponge.redengine.screen.components.PositionComponent;
import com.redsponge.redengine.screen.components.SizeComponent;
import com.redsponge.redengine.screen.systems.RenderSystem;
import com.redsponge.redengine.transitions.Transitions;
import com.redsponge.redengine.utils.GameAccessor;

public class TutorialScreen extends AbstractScreen implements SizableScreen, GameableScreen {

    private RenderSystem renderSystem;

    private Human moveFrom;
    private Human moveTo;
    private float moveTime;
    private boolean isMoving;
    private float time;
    private Texture straightArrow;

    private Array<Human> humans;
    private FitViewport guiViewport;
    private boolean isDead;
    private Doctor doc;
    private ParticleManager pm;

    enum TutorialPart {

        ONE("Welcome to experiment #31415 - The Takeover.\nYou are controlling a virus which controls this human. Your mission? Keep the virus *alive*\nMove using arrows, Go near the other human, hold Z, choose him, and release.\n TO SKIP THE TUTORIAL PRESS ENTER") {
            @Override
            public void init(TutorialScreen screen) {
                Human h = new Human(screen.batch, screen.shapeRenderer, 250, 82);
                h.maxHp = Integer.MAX_VALUE;
                h.hp = h.maxHp;
                h.speed = 0;
                screen.addHuman(h);
                h.isFacingLeft = true;
            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                if(Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                    screen.ga.transitionTo(new GameScreen(screen.ga), Transitions.sineSlide(1, screen.batch, screen.shapeRenderer));
                    return false;
                }
                return screen.controller.getControlled() != screen.firstHuman;
            }
        },
        TWO("You are now in control of this human!\nNow get control of the first human again") {
            @Override
            public void init(TutorialScreen screen) {

            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                return screen.controller.getControlled() == screen.firstHuman;
            }
        },
        THREE("You're doing great! Notice that health bar above your human?\nYou would like that to go down.. being a virus and all.. but you don't really want to be there when the human dies, ya-know?\nAnyways, take over the other human to progress") {
            @Override
            public void init(TutorialScreen screen) {

            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                return screen.controller.getControlled() != screen.firstHuman;
            }
        },
        FOUR("The damaging effect has been activated - look at what you do to people!") {
            @Override
            public void init(TutorialScreen screen) {
                for (int i = 0; i < screen.humans.size; i++) {
                    screen.humans.get(i).hp = 20 * screen.humans.get(i).getHPRatio();
                    screen.humans.get(i).maxHp = 20;
                    screen.humans.get(i).setHPGoDownSpeed(3);
                }
            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                if(screen.controller.getControlled() == null) return false;
                return screen.controller.getControlled().getHPRatio() < 0.2f;
            }
        },
        FIVE("I stopped it. The humans of course have some defenses of their own.\nFirst, they heal when you leave them. And second - they have medicine.\nEnter: The Doctor. (As usual, take over the other person to see more)") {
            Human self;

            @Override
            public void init(TutorialScreen screen) {
                self = screen.controller.getControlled();
                for (int i = 0; i < screen.humans.size; i++) {
                    screen.humans.get(i).setHPGoDownSpeed(0);
                }
                screen.doc = new Doctor(screen.batch, screen.shapeRenderer, 32, 32);
                screen.doc.maxHp = Integer.MAX_VALUE;
                screen.doc.hp = screen.doc.maxHp;
                screen.doc.speed = 0;
                screen.doc.syringes = 0;
                screen.doc.setProtected(true);
                screen.addHuman(screen.doc);
            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                return self != screen.controller.getControlled() && screen.controller.getControlled() != null;
            }
        },
        SIX("Our doctor will now go ahead and heal the human - watch out for that syringe. It'll definitely kill you if you get healed.") {
            @Override
            public void init(TutorialScreen screen) {
                screen.controller.getControlled().hp = 50;
                screen.controller.getControlled().maxHp = 50;
                for (int i = 0; i < screen.humans.size; i++) {
                    if(!(screen.humans.get(i) instanceof Doctor) && screen.humans.get(i) == screen.humans.get(i)) {
                        screen.humans.get(i).speed = 0;
                        screen.humans.get(i).setHPGoUpSpeed(0);
                    }
                }
                screen.doc.speed = 10;
                screen.doc.syringes = 3;
                screen.doc.generatePos();
            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                return screen.doc.syringes < 3;
            }
        },
        SEVEN("Augh, gross! You know what would be better? If those syringes were more - lethal.\nTake over the Doctor.") {
            @Override
            public void init(TutorialScreen screen) {
                for (int i = 0; i < screen.humans.size; i++) {
                    if(screen.humans.get(i) != screen.controller.getControlled()) {
                        screen.humans.get(i).setProtected(true);
                        break;
                    }
                }
                screen.doc.setProtected(false);
                screen.doc.speed = 0;
                screen.doc.syringes = 0;
                screen.doc.generatePos();
                screen.doc.syringes = 2;
            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                return screen.controller.getControlled() == screen.doc;
            }
        },
        EIGHT("Much better. Go ahead and use your newly acquired syringes.\nGo near the human, hold X, choose it, and release X.") {
            @Override
            public void init(TutorialScreen screen) {
                screen.doc.speed = 0;
                for (int i = 0; i < screen.humans.size; i++) {
                    if(!(screen.humans.get(i) instanceof Doctor)) {
                        screen.humans.get(i).hp = 1;
                        screen.humans.get(i).setHPGoUpSpeed(0);
                        screen.humans.get(i).setHPGoDownSpeed(0);
                        screen.humans.get(i).setProtected(false);
                    }
                }
            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                return screen.humans.size == 1;
            }
        },
        NINE("MMMmmmmmm.. doesn't that fell GOOD?\nOn another topic - see this glowing human? He got healed enough to become immune!\nYou can't take over him - try.\n To take over him, you must inject him with your fun syringes first.") {
            Human h;
            @Override
            public void init(TutorialScreen screen) {
                h = new Human(screen.batch, screen.shapeRenderer, 100, 30);
                h.setProtected(true);
                h.setHPGoDownSpeed(0);
                screen.addHuman(h);
            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                return screen.controller.getControlled() == h;
            }
        },
        TEN("This is it for the tutorial. Remember: your mission is to keep the virus alive!\nAs a side mission - see how many humans can you kill [think virus - forget about compassion >:)]\nPress Enter to start the game!") {
            @Override
            public void init(TutorialScreen screen) {
                for (int i = 0; i < screen.humans.size; i++) {
                    screen.removeEntity(screen.humans.get(i));
                }
                screen.humans.clear();
            }

            @Override
            public boolean isDone(TutorialScreen screen) {
                return Gdx.input.isKeyJustPressed(Keys.ENTER);
            }
        }
        ;

        public abstract void init(TutorialScreen screen);
        public abstract boolean isDone(TutorialScreen screen);
        private final String msg;

        TutorialPart(String msg) {
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }
    }

    private Texture stone;
    private PlayerController controller;
    private TutorialPart part;
    private BitmapFont font;
    private Human firstHuman;

    private Music music;

    public TutorialScreen(GameAccessor ga) {
        super(ga);
    }

    @Override
    public void show() {
        pm = new ParticleManager();
        part = TutorialPart.ONE;
        guiViewport = new FitViewport(getScreenWidth() * 2, getScreenHeight() * 2);
        humans = new DelayedRemovalArray<>();
        straightArrow = assets.get("takeoverArrow", Texture.class);
        stone = assets.get("stone", Texture.class);
        addEntity(new Background(batch, shapeRenderer, stone));
        renderSystem = getEntitySystem(RenderSystem.class);
        controller = new PlayerController(this);
        firstHuman = new Human(batch, shapeRenderer, 100, 100);
        firstHuman.maxHp = Integer.MAX_VALUE;
        firstHuman.hp = firstHuman.maxHp;
        addHuman(firstHuman);
        controller.setControlled(firstHuman);
        font = Fonts.getFont("pixelmix", 8);

        part.init(this);
        music = Gdx.audio.newMusic(Gdx.files.internal("music/somethings_not_right.ogg"));
        music.setLooping(true);
        music.play();
    }

    private void addHuman(Human human) {
        humans.add(human);
        addEntity(human);
    }

    @Override
    public void tick(float v) {
        controller.tick(v);
        if(controller.isChoosingTarget()) {
            v /= 5;
        }
        if(isMoving) {
            moveTime += v * 4;
            if(moveTime >= 1) {
                isMoving = false;
                controller.setControlled(moveTo);
            }
        }
        if(part.isDone(this)) {
            nextPart();
        }


        updateEngine(v);
        tickEntities(v);
        pm.tick(v);
    }

    private void nextPart() {
        if(part == TutorialPart.ONE) {
            part = TutorialPart.TWO;
        } else if(part == TutorialPart.TWO) {
            part = TutorialPart.THREE;
        } else if(part == TutorialPart.THREE) {
            part = TutorialPart.FOUR;
        } else if(part == TutorialPart.FOUR) {
            part = TutorialPart.FIVE;
        } else if(part == TutorialPart.FIVE) {
            part = TutorialPart.SIX;
        } else if(part == TutorialPart.SIX) {
            part = TutorialPart.SEVEN;
        } else if(part == TutorialPart.SEVEN) {
            part = TutorialPart.EIGHT;
        } else if(part == TutorialPart.EIGHT) {
            part = TutorialPart.NINE;
        } else if(part == TutorialPart.NINE) {
            part = TutorialPart.TEN;
        } else if(part == TutorialPart.TEN) {
            ga.transitionTo(new GameScreen(ga), Transitions.sineSlide(1, batch, shapeRenderer));
        }
        part.init(this);
    }

    @Override
    public void render() {
        renderEntities();
        shapeRenderer.setProjectionMatrix(renderSystem.getCamera().combined);
        batch.setProjectionMatrix(renderSystem.getCamera().combined);
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
        batch.setColor(Color.WHITE);
        pm.render(batch);
        batch.end();

        guiViewport.apply();
        batch.setProjectionMatrix(guiViewport.getCamera().combined);
        batch.begin();
        font.draw(batch, part.msg, 0, guiViewport.getWorldHeight() - 30, guiViewport.getWorldWidth(), Align.center, true);
        batch.end();
    }

    @Override
    public void beginMoveAnimation(Human from, Human to) {
        this.moveFrom = from;
        this.moveTo = to;
        this.isMoving = true;
        this.moveTime = 0;
    }

    @Override
    public boolean isMoving() {
        return isMoving;
    }

    @Override
    public Array<Human> getHumans() {
        return humans;
    }

    @Override
    public FitViewport getGUIViewport() {
        return guiViewport;
    }

    @Override
    public RenderSystem getRenderSystem() {
        return renderSystem;
    }

    @Override
    public boolean isPlayerDead() {
        return isDead;
    }

    @Override
    public Class<? extends AssetSpecifier> getAssetSpecsType() {
        return GameAssets.class;
    }

    @Override
    public void reSize(int width, int height) {
        renderSystem.resize(width, height);
        guiViewport.update(width, height, true);
    }

    @Override
    public float getGameWidth() {
        return 360;
    }

    @Override
    public float getGameHeight() {
        return 180;
    }

    @Override
    public int getScreenHeight() {
        return 180;
    }

    @Override
    public int getScreenWidth() {
        return 360;
    }

    @Override
    public void disposeAssets() {
        super.disposeAssets();
        pm.dispose();
        music.dispose();
    }

    @Override
    public ParticleManager getPM() {
        return pm;
    }
}

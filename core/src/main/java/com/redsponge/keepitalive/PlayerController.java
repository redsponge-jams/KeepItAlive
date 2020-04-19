package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.redsponge.redengine.screen.INotified;
import com.redsponge.redengine.screen.components.Mappers;
import com.redsponge.redengine.screen.components.PositionComponent;
import com.redsponge.redengine.screen.components.SizeComponent;
import com.redsponge.redengine.screen.components.VelocityComponent;
import com.redsponge.redengine.utils.MathUtilities;

public class PlayerController implements Disposable {

    private Human controlled;
    private VelocityComponent vel;
    private SizeComponent size;
    private PositionComponent pos;

    private final int speed = 100;

    private final Array<Human> targets;
    private boolean isChoosingTargets;
    private final GameScreen screen;
    private Vector2 controlledCenter;
    private Vector2 tmp;
    private int currentInterestingTarget;
    private boolean isControllingDoctor;
    private boolean isChoosingForTakeover;

    private final TextureRegion syringe;
    private final FrameBuffer syringeArea;
    private final TextureRegion syringeAreaRegion;
    private final Pixmap syringeMask;

    private final Texture syringeMaskTexture;
    private final TextureRegion syringeMaskRegion;

    private ScalingViewport syringeViewport;

    private FitViewport guiViewport;
    private float checkRadius;

    private Texture syringeStraight;
    private Texture takeoverArrow;
    private Texture target;
    private float x;

    public PlayerController(GameScreen screen) {
        this.screen = screen;
        targets = new Array<>();
        syringeArea = new FrameBuffer(Pixmap.Format.RGBA8888, 16 * 3, 16, false);
        syringeMask = new Pixmap(16 * 3, 16, Pixmap.Format.RGBA8888);

        syringeViewport = new ScalingViewport(Scaling.fill, 16 * 3, 16);
        syringeAreaRegion = new TextureRegion(syringeArea.getColorBufferTexture());
        syringeAreaRegion.flip(false, true);
        syringeArea.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        syringe = new TextureRegion(screen.getAssets().get("syringe", Texture.class));
        syringeStraight = screen.getAssets().get("syringeStraight", Texture.class);
        takeoverArrow = screen.getAssets().get("takeoverArrow", Texture.class);
        target = screen.getAssets().get("target", Texture.class);

        syringeMaskTexture = new Texture(syringeMask);
        syringeMaskRegion = new TextureRegion(syringeMaskTexture);
        syringeMaskRegion.flip(false, true);
    }

    private float getAngleRelativeToSelf(Human h) {
        PositionComponent hPos = Mappers.position.get(h);
        SizeComponent hSize = Mappers.size.get(h);
        double angle = MathUtils.atan2(pos.getY() + size.getY() / 2f - hPos.getY() - hSize.getY() / 2f, pos.getX() + size.getX() / 2f - hPos.getX() - hSize.getX() / 2f);
        return (float) angle;
    }

    public void setControlled(Human controlled) {
        if (this.controlled != null) {
            this.controlled.setControlled(false);
            this.controlled.virusLeft();
        }
        this.controlled = controlled;
        if(controlled != null) {
            this.isControllingDoctor = controlled instanceof Doctor;
            this.controlled.setControlled(true);
            this.vel = Mappers.velocity.get(controlled);
            this.pos = Mappers.position.get(controlled);
            this.size = Mappers.size.get(controlled);

            this.controlledCenter = new Vector2();
            this.tmp = new Vector2();
        }
    }


    public void tick(float delta) {
        x += delta * 360;
        if(delta == 0) return;
        if(controlled.isDead()) {
            screen.notified(this, Notifications.HOST_DIED);
            return;
        }


        if (isChoosingTargets) {
            checkRadius = MathUtilities.lerp(checkRadius, 50, 0.1f);
            vel.set(0, 0);
            generateTargets(!isChoosingForTakeover, true);
            int horiz = (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) ? 1 : 0) - (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ? 1 : 0);
            currentInterestingTarget += horiz;
            if(currentInterestingTarget >= targets.size) {
                currentInterestingTarget = 0;
            }
            if(currentInterestingTarget < 0) {
                currentInterestingTarget = targets.size - 1;
            }
            if(isChoosingForTakeover) {
                if (!Gdx.input.isKeyPressed(Input.Keys.Z)) {
                    if (targets.size > 0) {
                        if (targets.get(currentInterestingTarget).isProtected()) {
                            screen.notified(this, Notifications.LOST);
                            return;
                        }

                        screen.beginMoveAnimation(controlled, targets.get(currentInterestingTarget));
                        setControlled(null);
                    }
                    releaseTargets();
                }
            } else {
                if (isControllingDoctor && !Gdx.input.isKeyPressed(Input.Keys.X)) {
                    if(targets.size > 0) {
                        targets.get(currentInterestingTarget).injectBad();
                        ((Doctor) controlled).syringes--;
                    }
                    releaseTargets();
                } else if(!isControllingDoctor) {
                    releaseTargets();

                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                releaseTargets();
            }
        } else {
            checkRadius = 0;
            int horiz = (Gdx.input.isKeyPressed(Input.Keys.RIGHT) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.LEFT) ? 1 : 0);
            int vert = (Gdx.input.isKeyPressed(Input.Keys.UP) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.DOWN) ? 1 : 0);

            this.vel.setX(horiz * speed);
            this.vel.setY(vert * speed);

            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
                generateTargets(false, false);
                isChoosingForTakeover = true;
            } else if(isControllingDoctor && Gdx.input.isKeyJustPressed(Input.Keys.X) && ((Doctor)controlled).syringes >= 1) {
                generateTargets(true, false);
                isChoosingForTakeover = false;
            }
        }
        if(pos.getX() + vel.getX() * delta < 0) {
            pos.setX(0);
            vel.setX(0);
        } else if(pos.getX() + vel.getX() * delta + size.getX() > screen.getGameWidth()) {
            pos.setX(screen.getGameWidth() - size.getX());
            vel.setX(0);
        }
        if(pos.getY() + vel.getY() * delta < 0) {
            pos.setY(0);
            vel.setY(0);
        } else if(pos.getY() + vel.getY() * delta + size.getY() > screen.getGameHeight()) {
            pos.setY(screen.getGameHeight() - size.getY());
            vel.setY(0);
        }
    }

    private void releaseTargets() {
        isChoosingTargets = false;
        targets.clear();
    }

    private void renderSyringes(SpriteBatch batch) {
        syringeViewport.apply(true);
        batch.setProjectionMatrix(syringeViewport.getCamera().combined);
        syringeArea.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        syringeMask.setColor(0, 0, 0, 0.1f);
        syringeMask.fill();
        float tmp = ((Doctor)controlled).syringes;
        batch.begin();
        int i = 0;
        for (int j = 0; j < 3; j++) {
            batch.setColor(Color.GREEN);
            batch.draw(syringe, j * 16, 0, 16, 16);
        }
        while(tmp > 0) {
            syringeMask.setColor(1, 1, 1, 1);
            syringeMask.fillRectangle(i * 16, 0, 16, (int) (syringeMask.getHeight() * (tmp >= 1 ? 1 : tmp)));

            tmp--;
            i++;
        }
        syringeMaskTexture.draw(syringeMask, 0, 0);

        batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_SRC_ALPHA);
        batch.draw(syringeMaskRegion, 0, 0, syringeArea.getWidth(), syringeArea.getHeight());
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.end();

        syringeArea.end();
        screen.getGUIViewport().apply();
        batch.setProjectionMatrix(screen.getGUIViewport().getCamera().combined);
        batch.begin();
        batch.draw(syringeAreaRegion, 0, 0, 96, 32);
        batch.end();
    }

    public void render(SpriteBatch batch, ShapeRenderer renderer) {
        if(screen.isPlayerDead()) return;
        if(isControllingDoctor && !screen.isMoving()) {
            renderSyringes(batch);
        }
        if(controlled != null) {
            renderer.begin(ShapeType.Filled);
            renderer.setColor(Color.BLACK);
            renderer.rect(pos.getX(), pos.getY() + size.getY() + 1+1, size.getX(), 3);
            float prog = controlled.hp / controlled.maxHp;
            renderer.setColor(Color.GREEN);
            renderer.rect(pos.getX() + 1, pos.getY() + size.getY() + 2+1, prog * (size.getX() - 2), 1);
            renderer.end();
        }
        if (!isChoosingTargets) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        renderer.begin(ShapeType.Filled);
        renderer.setColor(Color.RED);
        renderer.getColor().a = 0.2f;
        renderer.circle(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f, checkRadius, 300);
        if(targets.size > 0) {
            renderer.set(ShapeRenderer.ShapeType.Filled);
            PositionComponent targetPos = Mappers.position.get(targets.get(currentInterestingTarget));
            SizeComponent targetSize = Mappers.size.get(targets.get(currentInterestingTarget));
            renderer.rectLine(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f, targetPos.getX() + targetSize.getX() / 2f, targetPos.getY() + targetSize.getY() / 2f, 2, new Color(0, 0.5f, 0, 1.0f), Color.GREEN);
        }
        renderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if(targets.size > 0) {
            Human h = targets.get(currentInterestingTarget);
            PositionComponent targetPos = Mappers.position.get(h);
            batch.setProjectionMatrix(screen.getRenderSystem().getViewport().getCamera().combined);
            batch.begin();
            batch.setColor(Color.GREEN);
            batch.draw(target, targetPos.getX() + 4, targetPos.getY() + 4, target.getWidth() / 2f, target.getHeight() / 2f);
            batch.end();
        }

        float rotation = 0;
        if(targets.size > 0) {
            rotation = getAngleRelativeToSelf(targets.get(currentInterestingTarget)) * MathUtils.radiansToDegrees;
            rotation = rotation + 90;
        }
        Texture tex = isChoosingForTakeover ? takeoverArrow : syringeStraight;
        screen.getRenderSystem().getViewport().apply();
        batch.setProjectionMatrix(screen.getRenderSystem().getViewport().getCamera().combined);
        batch.begin();
        batch.setColor(Color.GREEN);
        batch.draw(tex, pos.getX() + size.getX() / 2f - 3, pos.getY() + size.getY() / 2f, 3, 0, 6, tex.getHeight(), 1, 1, rotation, 0, 0, 6, tex.getHeight(), false, false);
        batch.end();
    }

    private void generateTargets(boolean includeProtected, boolean update) {
        Human targetedBefore = null;
        if(update && targets.size > 0) {
            targetedBefore = targets.get(currentInterestingTarget);
        }

        targets.clear();
        isChoosingTargets = true;
        controlledCenter.set(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f);

        for (Human h : screen.getHumans()) {
            if (h == controlled) continue;

            PositionComponent humanPos = Mappers.position.get(h);
            SizeComponent humanSize = Mappers.size.get(h);
            tmp.set(humanPos.getX() + humanSize.getX() / 2f, humanPos.getY() + humanSize.getY() / 2f);
            if(includeProtected || !h.isProtected()) {
                if (tmp.dst2(controlledCenter) < checkRadius * checkRadius) {
                    targets.add(h);
                }
            }
        }
        targets.sort((h1, h2) -> {
            float h1Angle = getAngleRelativeToSelf(h1);
            float h2Angle = getAngleRelativeToSelf(h2);
            return (int) (h1Angle - h2Angle);
        });
        if(update) {
            for (int i = 0; i < targets.size; i++) {
                if(targets.get(i) == targetedBefore) {
                    currentInterestingTarget = i;
                    break;
                }
            }
        } else {
            currentInterestingTarget = 0;
        }
    }



    @Override
    public void dispose() {
        syringeMask.dispose();
        syringeArea.dispose();
    }

    public void focusCamera(OrthographicCamera camera) {
        camera.position.lerp(new Vector3(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f, 0), 0.1f);
        if(isChoosingTargets) {
            camera.zoom = MathUtilities.lerp(camera.zoom, 0.8f, 0.1f);
        } else {
            camera.zoom = MathUtilities.lerp(camera.zoom, 1, 0.1f);
        }
    }

    public void resize(int width, int height) {
        guiViewport.update(width, height, true);
    }

    public boolean isChoosingTarget() {
        return isChoosingTargets;
    }

    public Human getControlled() {
        return controlled;
    }
}

package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
import com.redsponge.redengine.utils.GeneralUtils;
import com.redsponge.redengine.utils.Logger;
import com.redsponge.redengine.utils.MathUtilities;

public class PlayerController implements INotified, Disposable {

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

    private ScalingViewport syringeViewport;

    private FitViewport guiViewport;
    private float checkRadius;

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
        guiViewport = new FitViewport(screen.getScreenWidth(), screen.getScreenHeight());
    }

    public void setControlled(Human controlled) {
        if (this.controlled != null) {
            this.controlled.setControlled(false);
            this.controlled.virusLeft();
        }
        this.controlled = controlled;
        this.isControllingDoctor = controlled instanceof Doctor;
        this.controlled.setControlled(true);
        this.vel = Mappers.velocity.get(controlled);
        this.pos = Mappers.position.get(controlled);
        this.size = Mappers.size.get(controlled);

        this.controlledCenter = new Vector2();
        this.tmp = new Vector2();
    }

    @Override
    public void notified(Object o, int i) {
        if(i == Notifications.CONTROLLED_HEALED) {
            if(o == controlled) {
                screen.notified(this, Notifications.LOST);
            }
        }
    }

    public void tick(float delta) {
        if(controlled.isDead()) {
            Logger.log(this, "CONTROLLED DEAD :(");
            screen.notified(this, Notifications.LOST);
            return;
        }


        if (isChoosingTargets) {
            checkRadius = MathUtilities.lerp(checkRadius, 50, 0.1f);
            vel.set(0, 0);
            generateTargets(!isChoosingForTakeover);
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

                        setControlled(targets.get(currentInterestingTarget));
                        releaseTargets();
                    }
                }
            } else {
                if (isControllingDoctor && !Gdx.input.isKeyPressed(Input.Keys.X)) {
                    targets.get(currentInterestingTarget).injectBad();
                    ((Doctor)controlled).syringes--;
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
                generateTargets(false);
                isChoosingForTakeover = true;
            } else if(isControllingDoctor && Gdx.input.isKeyJustPressed(Input.Keys.X) && ((Doctor)controlled).syringes >= 1) {
                generateTargets(true);
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
        TextureRegion reg = new TextureRegion(new Texture(syringeMask));
        reg.flip(false, true);

//        batch.enableBlending();
        batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_SRC_ALPHA);
        batch.draw(reg, 0, 0, syringeArea.getWidth(), syringeArea.getHeight());
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
        if(isControllingDoctor) {
            renderSyringes(batch);
        }

        if (!isChoosingTargets) return;

        renderer.begin(ShapeRenderer.ShapeType.Line);
        renderer.setColor(Color.RED);
        for (int i = 0; i < targets.size; i++) {
            PositionComponent pos = Mappers.position.get(targets.get(i));
            SizeComponent size = Mappers.size.get(targets.get(i));
            renderer.rect(pos.getX(), pos.getY(), size.getX(), size.getY());
            if (i == currentInterestingTarget) {
                renderer.setColor(Color.GREEN);
                renderer.setAutoShapeType(true);
                renderer.set(ShapeRenderer.ShapeType.Filled);
                renderer.circle(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f, 4);
                renderer.setColor(Color.RED);
                renderer.set(ShapeRenderer.ShapeType.Line);
            }
        }
        renderer.circle(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f, checkRadius);
        renderer.end();
    }

    private void generateTargets(boolean includeProtected) {
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
        targets.sort((h1, h2) -> (int) (Mappers.position.get(h2).getX() - Mappers.position.get(h1).getX()));
        if (targets.isEmpty() && checkRadius > 40) {
            isChoosingTargets = false;
            Logger.log(this, "Couldn't find any targets!");
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
}

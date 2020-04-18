package com.redsponge.keepitalive;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.redsponge.redengine.screen.components.Mappers;
import com.redsponge.redengine.screen.components.PositionComponent;
import com.redsponge.redengine.screen.components.SizeComponent;
import com.redsponge.redengine.screen.components.VelocityComponent;
import com.redsponge.redengine.utils.Logger;

public class PlayerController {

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

    public PlayerController(GameScreen screen) {
        this.screen = screen;
        targets = new Array<>();
    }

    public void setControlled(Human controlled) {
        if (this.controlled != null) {
            this.controlled.setControlled(false);
            this.controlled.virusLeft();
        }
        this.controlled = controlled;
        this.controlled.setControlled(true);
        this.vel = Mappers.velocity.get(controlled);
        this.pos = Mappers.position.get(controlled);
        this.size = Mappers.size.get(controlled);

        this.controlledCenter = new Vector2();
        this.tmp = new Vector2();
    }

    public void tick(float delta) {
        if(controlled.isDead()) {
            screen.notified(this, Notifications.LOST);
            return;
        }

        if (isChoosingTargets) {
            vel.set(0, 0);
            int horiz = (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) ? 1 : 0) - (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) ? 1 : 0);
            currentInterestingTarget += horiz;
            if(currentInterestingTarget >= targets.size) {
                currentInterestingTarget = 0;
            }
            if(currentInterestingTarget < 0) {
                currentInterestingTarget = targets.size - 1;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
                setControlled(targets.get(currentInterestingTarget));
                releaseTargets();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                releaseTargets();
            }
        } else {

            int horiz = (Gdx.input.isKeyPressed(Input.Keys.RIGHT) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.LEFT) ? 1 : 0);
            int vert = (Gdx.input.isKeyPressed(Input.Keys.UP) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.DOWN) ? 1 : 0);

            this.vel.setX(horiz * speed);
            this.vel.setY(vert * speed);

            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
                generateTargets();
            }
        }
    }

    private void releaseTargets() {
        isChoosingTargets = false;
        targets.clear();
    }

    public void render(SpriteBatch batch, ShapeRenderer renderer) {
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
        renderer.end();
    }

    private void generateTargets() {
        currentInterestingTarget = 0;
        targets.clear();
        isChoosingTargets = true;
        controlledCenter.set(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f);

        for (Human h : screen.getHumans()) {
            if (h == controlled) continue;

            PositionComponent humanPos = Mappers.position.get(h);
            SizeComponent humanSize = Mappers.size.get(h);
            tmp.set(humanPos.getX() + humanSize.getX() / 2f, humanPos.getY() + humanSize.getY() / 2f);
            if (tmp.dst2(controlledCenter) < 50 * 50) {
                targets.add(h);
            }
        }
        if (targets.isEmpty()) {
            isChoosingTargets = false;
            Logger.log(this, "Couldn't find any targets!");
        }
    }
}

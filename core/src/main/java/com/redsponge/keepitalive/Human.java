package com.redsponge.keepitalive;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.redsponge.redengine.screen.components.RenderRunnableComponent;
import com.redsponge.redengine.screen.entity.ScreenEntity;
import com.redsponge.redengine.utils.IntVector2;
import com.redsponge.redengine.utils.Logger;

public class Human extends ScreenEntity {

    private boolean controlled;
    private IntVector2 wantedPos;
    private float speed = 40;

    private float hp;
    private boolean isDead;
    private float maxHp = 10;

    public Human(SpriteBatch batch, ShapeRenderer shapeRenderer) {
        super(batch, shapeRenderer);
        wantedPos = new IntVector2();
        hp = maxHp;
    }

    @Override
    public void added() {
        pos.set(32, 32);
        size.set(8, 8);
        add(new RenderRunnableComponent(this::debugRender));
        generatePos();
    }

    private void generatePos() {
        wantedPos.set(MathUtils.random(320), MathUtils.random(180));
    }

    @Override
    public void additionalTick(float delta) {
        if(hp <= 0) {
            die();
        }
        if(controlled) {
            hp -= delta;
            return;
        } else {
            hp += delta / 2f;
        }
        double angle = Math.atan2(wantedPos.y - pos.getY(), wantedPos.x - pos.getX());
        double vx = Math.cos(angle);
        double vy = Math.sin(angle);
        vel.set((float) (vx * speed), (float) (vy * speed));
        if(Vector2.dst2(pos.getX() + size.getX() / 2f, pos.getY() + size.getY() / 2f, wantedPos.x, wantedPos.y) < 100) {
            generatePos();
        }
    }

    private void die() {
        isDead = true;
        remove();
    }

    public void virusLeft() {
        hp /= 2;
        if(hp < 0.1f) {
            hp = 0;
        }
    }

    public void setControlled(boolean controlled) {
        this.controlled = controlled;
    }

    private void debugRender() {
        batch.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.getColor().lerp(0, 0.5f, 0, 1.0f, 1 - (hp / maxHp));
        shapeRenderer.rect(pos.getX(), pos.getY(), size.getX(), size.getY());
        shapeRenderer.end();
        batch.begin();
    }

    public boolean isDead() {
        return isDead;
    }
}
